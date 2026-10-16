"""(1.21.1 port) Turns the resources tools/generate_resources.py writes (Minecraft 26.3's formats)
into Minecraft 1.21.1's, in place, so one generator serves both builds:

    python3 tools/generate_resources.py && python3 tools/port/to_1_21.py

Run it once on fresh generator output (it converts what it finds in 26.3's shape and leaves the rest).
What changed between the two, and what this does about it:
  biomes             26.3's "attributes" (fog, music, ambience, particles, spawns) -> 1.21.1's effects/spawners
  configured features worldgen/feature, config inline -> worldgen/configured_feature, under "config"
  block states       "astralend:x" / {"id", "properties"} -> {"Name", "Properties"} and state providers
  placements         offset(x, y, z) -> random_offset; trapezoid ints -> clamped normal
  density functions  left/right/input -> argument1/argument2/argument; sub, lerp, gradient spelled out
  surface rules      worldgen/material_rule refs inlined into the noise settings' surface_rule
  loot tables        modifier/condition -> functions/conditions; named tool predicates spelled out
  recipes            "id" / "#tag" ingredients -> {"item"} / {"tag"}; brewing goes to Java (ModBrewing)
  advancements       player/entity conditions as lists, predicate keys, recipe/loot_table criteria
  item models        assets/*/items definitions -> models/item (Starforge overlays: StarforgeItemModels)
  render types       26.3 picks a block's layer from its textures; 1.21.1 reads "render_type" from the model
  armour trims       texture paths, the trims atlas, the template item
  tags               vanilla entries made optional (some of 26.3's blocks and items aren't in 1.21.1)
  structures         DataVersion of 1.21.1; palette entries {Name, Properties}
  pack.mcmeta        pack_format instead of 26.3's format range
"""
import json, gzip, math, shutil, struct, sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources"
DATA, ASSETS = ROOT / "data", ROOT / "assets"
NS = "astralend"
DATA_VERSION = 3955   # Minecraft 1.21.1
changed = []


def load(p):
    return json.loads(p.read_text())


def save(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n")
    changed.append(p)


def colour(c):
    return int(c[1:], 16) if isinstance(c, str) and c.startswith("#") else c


# ---- block states ------------------------------------------------------------------------------

def state(s):
    """"astralend:x" or {"id"/"Name", "properties"} -> {"Name", "Properties"}."""
    if isinstance(s, str):
        return {"Name": s}
    if isinstance(s, dict) and "id" in s:
        out = {"Name": s["id"]}
        if s.get("properties"):
            out["Properties"] = s["properties"]
        return out
    return s


def provider(p):
    """26.3's block state provider ({"id"} for one block) -> 1.21.1's."""
    if isinstance(p, str) or (isinstance(p, dict) and "id" in p and "type" not in p):
        return {"type": "minecraft:simple_state_provider", "state": state(p)}
    if isinstance(p, dict):
        p = dict(p)
        if p.get("type") == "minecraft:randomized_int" and "source" in p:   # 1.21.1's name for it
            p["type"] = "minecraft:randomized_int_state_provider"
        if p.get("type") == "minecraft:randomized_int_state_provider":
            p["source"] = provider(p["source"])
        for k in ("default", "fallback"):
            if k in p:
                p[k] = provider(p[k])
        if "entries" in p:   # weighted
            p["entries"] = [{**e, "data": state(e["data"])} if "data" in e else e for e in p["entries"]]
    return p


def int_provider(v):
    """26.3's trapezoid int provider -> a clamped normal of the same spread."""
    if isinstance(v, dict) and v.get("type") == "minecraft:trapezoid" and "min" in v:
        lo, hi, plateau = v["min"], v["max"], v.get("plateau", 0)
        if lo == hi:
            return lo
        dev = math.sqrt(((hi - lo) ** 2 + plateau ** 2) / 24.0)
        return {"type": "minecraft:clamped_normal", "mean": (lo + hi) / 2.0, "deviation": round(dev, 3),
                "min_inclusive": lo, "max_inclusive": hi}
    return v


def predicate(p):
    """Block predicates: string states in would_survive; nested all_of/any_of/not."""
    if isinstance(p, dict):
        p = dict(p)
        if p.get("type") == "minecraft:would_survive":
            p["state"] = state(p["state"])
        for k in ("predicates",):
            if k in p:
                p[k] = [predicate(x) for x in p[k]]
        if "predicate" in p:
            p["predicate"] = predicate(p["predicate"])
    return p


# ---- biomes ------------------------------------------------------------------------------------

def biome(b):
    if "attributes" not in b:
        return None
    a = b["attributes"]
    fx = {"fog_color": colour(a.get("minecraft:visual/fog_color", "#000000")), "sky_color": 0,
          "water_color": colour(b.get("effects", {}).get("water_color", "#3f76e4")), "water_fog_color": 329011}
    for k in ("grass_color", "foliage_color"):
        if k in b.get("effects", {}):
            fx[k] = colour(b["effects"][k])
    music = a.get("minecraft:audio/background_music", {}).get("default")
    if music:
        fx["music"] = {k: music[k] for k in ("sound", "min_delay", "max_delay", "replace_current_music") if k in music}
    sounds = a.get("minecraft:audio/ambient_sounds", {})
    if "loop" in sounds:
        fx["ambient_sound"] = sounds["loop"]
    if "additions" in sounds:
        fx["additions_sound"] = sounds["additions"]
    if "mood" in sounds:
        fx["mood_sound"] = sounds["mood"]
    particles = a.get("minecraft:visual/ambient_particles", {}).get("argument", [])
    if particles:
        options = dict(particles[0]["particle"])
        if options["type"] == "minecraft:firefly":   # 1.21.5's; the nearest glowing mote in 1.21.1
            options = {"type": "minecraft:glow"}
        fx["particle"] = {"options": options, "probability": particles[0]["probability"]}
    spawns = a.get("minecraft:gameplay/natural_mob_spawns", {}).get("argument", {})
    spawners = {}
    for category, entries in spawns.get("spawns_by_category", {}).items():
        spawners[category] = [{"type": e["type"], "weight": e["weight"],
                               "minCount": e.get("count", e.get("minCount", 1)), "maxCount": e.get("count", e.get("maxCount", 1))}
                              for e in entries]
    out = {"has_precipitation": b.get("has_precipitation", False), "temperature": b.get("temperature", 0.5),
           "downfall": b.get("downfall", 0.5), "effects": fx, "spawners": spawners,
           "spawn_costs": spawns.get("spawn_costs", {}), "carvers": {}, "features": b.get("features", [])}
    if "temperature_modifier" in b:
        out["temperature_modifier"] = b["temperature_modifier"]
    return out


# ---- configured and placed features ------------------------------------------------------------

def configured_feature(f):
    if "config" in f:
        return f
    t = f["type"]
    c = {k: v for k, v in f.items() if k != "type"}
    if t == "minecraft:simple_block":
        c["to_place"] = provider(c["to_place"])
    elif t == "minecraft:ore":
        c["targets"] = [{**x, "state": state(x["state"])} for x in c["targets"]]
    elif t == "minecraft:block_column":
        c["layers"] = [{**x, "provider": provider(x["provider"]), "height": int_provider(x["height"])} for x in c["layers"]]
        if "allowed_placement" in c:
            c["allowed_placement"] = predicate(c["allowed_placement"])
    elif not t.startswith(NS + ":") and c:
        print("  configured feature of type", t, "left as it is - check it")
    return {"type": t, "config": c}


def placement(m):
    m = dict(m)
    t = m["type"]
    if t == "minecraft:offset":
        x, y, z = int_provider(m.get("x", 0)), int_provider(m.get("y", 0)), int_provider(m.get("z", 0))
        if x != z:
            print("  offset with different x and z spreads; using x for both:", m)
        return {"type": "minecraft:random_offset", "xz_spread": x, "y_spread": y}
    if t == "minecraft:block_predicate_filter":
        m["predicate"] = predicate(m["predicate"])
    if t == "minecraft:environment_scan":
        m["target_condition"] = predicate(m["target_condition"])
        if "allowed_search_condition" in m:
            m["allowed_search_condition"] = predicate(m["allowed_search_condition"])
    if t == "minecraft:count" or t == "minecraft:count_on_every_layer":
        m["count"] = int_provider(m["count"])
    return m


# ---- density functions and noise settings ------------------------------------------------------

UNARY = {"abs", "square", "cube", "half_negative", "quarter_negative", "squeeze", "interpolated", "flat_cache",
         "cache_2d", "cache_once", "cache_all_in_cell", "blend_density"}
BINARY = {"add", "mul", "min", "max"}


def density(d):
    if isinstance(d, str):
        return {"type": "minecraft:end_islands"} if d == "minecraft:end/islands" else d
    if not isinstance(d, dict):
        return d
    t = d.get("type", "")
    name = t.split(":")[-1]
    if name in BINARY:
        return {"type": t, "argument1": density(d.get("left", d.get("argument1"))), "argument2": density(d.get("right", d.get("argument2")))}
    if name == "sub":
        return {"type": "minecraft:add", "argument1": density(d["left"]),
                "argument2": {"type": "minecraft:mul", "argument1": -1.0, "argument2": density(d["right"])}}
    if name == "lerp":   # first + alpha * (second - first)
        first, second, alpha = density(d["first"]), density(d["second"]), density(d["alpha"])
        return {"type": "minecraft:add", "argument1": first, "argument2": {"type": "minecraft:mul", "argument1": alpha,
                "argument2": {"type": "minecraft:add", "argument1": second,
                              "argument2": {"type": "minecraft:mul", "argument1": -1.0, "argument2": first}}}}
    if name == "gradient":
        assert d.get("axis", "y") == "y", d
        return {"type": "minecraft:y_clamped_gradient", "from_y": d["from_coordinate"], "to_y": d["to_coordinate"],
                "from_value": d["from_value"], "to_value": d["to_value"]}
    if name == "distance_to_point":   # registered by the mod for 1.21.1 (world/DistanceToPoint.java)
        return {"type": f"{NS}:distance_to_point", "point": d["point"], "metric": d.get("metric", "euclidean")}
    if name in UNARY:
        return {"type": t, "argument": density(d.get("input", d.get("argument")))}
    if name == "clamp":
        return {"type": t, "input": density(d["input"]), "min": d["min"], "max": d["max"]}
    if name in ("noise", "shifted_noise", "beardifier", "end_islands", "constant", "y_clamped_gradient", "old_blended_noise",
                "weird_scaled_sampler", "range_choice", "spline", "shift", "shift_a", "shift_b"):
        return {k: density(v) if k in ("input", "argument", "when_in_range", "when_out_of_range", "shift_x", "shift_z") else v
                for k, v in d.items()}
    print("  density function of type", t, "left as it is - check it")
    return d


ROUTER = ["barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "temperature", "vegetation", "continents",
          "erosion", "depth", "ridges", "initial_density_without_jaggedness", "final_density", "vein_toggle", "vein_ridged", "vein_gap"]

NAMED_CONDITIONS = {   # 26.3's named surface conditions
    "minecraft:on_floor": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"},
    "minecraft:on_ceiling": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "ceiling"},
    "minecraft:under_floor": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True, "secondary_depth_range": 0, "surface_type": "floor"},
    "minecraft:under_ceiling": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True, "secondary_depth_range": 0, "surface_type": "ceiling"},
}
NAMED_RULES = {"minecraft:end": {"type": "minecraft:block", "result_state": {"Name": "minecraft:end_stone"}}}


def surface_rule(r, rules):
    if isinstance(r, str):
        if r in NAMED_RULES:
            return NAMED_RULES[r]
        path = rules / (r.split(":")[1] + ".json")
        return surface_rule(load(path), rules)
    r = dict(r)
    if r["type"] == "minecraft:block":
        r["result_state"] = state(r["result_state"])
    if r["type"] == "minecraft:sequence":
        r["sequence"] = [surface_rule(x, rules) for x in r["sequence"]]
    if r["type"] == "minecraft:condition":
        c = r["if_true"]
        r["if_true"] = NAMED_CONDITIONS[c] if isinstance(c, str) else c
        r["then_run"] = surface_rule(r["then_run"], rules)
    return r


def noise_settings(s, rules):
    if "material_rule" not in s:
        return None
    out = dict(s)
    out["surface_rule"] = surface_rule(out.pop("material_rule"), rules)
    out["default_block"], out["default_fluid"] = state(s["default_block"]), state(s["default_fluid"])
    noise = dict(s["noise"])
    noise.setdefault("size_horizontal", 2)
    noise.setdefault("size_vertical", 1)
    out["noise"] = noise
    router = {k: density(v) for k, v in s["noise_router"].items() if k != "chunk_surface_level"}
    out["noise_router"] = {k: router.get(k, 0.0) for k in ROUTER}
    out.setdefault("aquifers_enabled", False)
    out.setdefault("ore_veins_enabled", False)
    return out


# ---- loot --------------------------------------------------------------------------------------

TOOL = {
    "minecraft:tool/can_shear": {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
    "minecraft:tool/can_silk_touch": {"condition": "minecraft:match_tool", "predicate": {"predicates": {
        "minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}},
}
ENTITY_KEYS = {"minecraft:entity_type": "type"}


def entity_predicate(p):
    if not isinstance(p, dict):
        return p
    return {ENTITY_KEYS.get(k, k.split(":", 1)[1] if k.startswith("minecraft:") else k): v for k, v in p.items()}


def condition(c):
    if isinstance(c, str):
        return TOOL[c]
    c = dict(c)
    if "type" in c and "condition" not in c:
        c["condition"] = c.pop("type")
    if c["condition"] == "minecraft:match_block":   # 26.3's name for block_state_property
        blocks = c["blocks"][0] if isinstance(c["blocks"], list) else c["blocks"]
        c = {"condition": "minecraft:block_state_property", "block": blocks, **({"properties": c["state"]} if "state" in c else {})}
    if c["condition"] == "minecraft:entity_properties" and "predicate" in c:
        c["predicate"] = entity_predicate(c["predicate"])
    if "terms" in c:
        c["terms"] = [condition(x) for x in c["terms"]]
    if "term" in c:
        c["term"] = condition(c["term"])
    return c


def conditions(c):
    c = condition(c)
    return c["terms"] if c["condition"] == "minecraft:all_of" else [c]


def loot(o):
    if isinstance(o, list):
        return [loot(x) for x in o]
    if not isinstance(o, dict):
        return o
    o = dict(o)
    if "modifier" in o:
        fs = o.pop("modifier")
        o["functions"] = [loot_function(f) for f in (fs if isinstance(fs, list) else [fs])]
    if "condition" in o:
        o["conditions"] = conditions(o.pop("condition"))
    for k in ("pools", "entries", "children"):
        if k in o:
            o[k] = loot(o[k])
    return o


def loot_function(f):
    f = dict(f)
    if "type" in f and "function" not in f:
        f["function"] = f.pop("type")
    if "condition" in f:
        f["conditions"] = conditions(f.pop("condition"))
    return f


# ---- recipes -----------------------------------------------------------------------------------

def ingredient(i):
    if isinstance(i, list):
        return [ingredient(x) for x in i]
    if isinstance(i, str):
        return {"tag": i[1:]} if i.startswith("#") else {"item": i}
    return i


def recipe(r):
    r = dict(r)
    if "key" in r:
        r["key"] = {k: ingredient(v) for k, v in r["key"].items()}
    if "ingredients" in r:
        r["ingredients"] = [ingredient(x) for x in r["ingredients"]]
    for k in ("ingredient", "template", "base", "addition"):
        if k in r:
            r[k] = ingredient(r[k])
    r.pop("pattern", None) if r["type"] == "minecraft:smithing_trim" else None
    return r


# ---- advancements ------------------------------------------------------------------------------

def advancement(a):
    a = json.loads(json.dumps(a))
    for crit in a.get("criteria", {}).values():
        c = crit.get("conditions", {})
        for k in ("player", "entity"):
            if isinstance(c.get(k), dict) and ("type" in c[k] or "condition" in c[k]):
                c[k] = [condition(c[k])]
        if "recipes" in c:
            r = c.pop("recipes")
            c["recipe"] = r[0] if isinstance(r, list) else r
        if "loot_tables" in c:
            t = c.pop("loot_tables")
            c["loot_table"] = t[0] if isinstance(t, list) else t
    return a


# ---- item models -------------------------------------------------------------------------------

def item_models():
    """26.3 resolves an item's model through assets/<ns>/items/<item>.json; 1.21.1 loads models/item/<item>."""
    for f in sorted((ASSETS / NS / "items").glob("*.json")):
        name = f.stem
        target = ASSETS / NS / "models/item" / f"{name}.json"
        m = load(f)["model"]
        if m["type"] == "minecraft:model":
            if m["model"] != f"{NS}:item/{name}" and not target.exists():
                save(target, {"parent": m["model"]})
        elif m["type"] == "minecraft:range_dispatch" and m["property"] == "minecraft:compass":
            # an "angle" property (registered for it in StarforgeItemModels.registerProperties)
            scale = m.get("scale", 1.0)
            entries = sorted(m["entries"], key=lambda e: e["threshold"])
            base = load(ASSETS / NS / "models/item" / (entries[0]["model"]["model"].split("/")[-1] + ".json"))
            save(target, {**base, "overrides": [{"predicate": {"angle": round(e["threshold"] / scale, 6)}, "model": e["model"]["model"]}
                                                for e in entries]})
        else:
            print("  item", name, "uses a", m["type"], "definition - handle it in Java")


# ---- render types ------------------------------------------------------------------------------

_alpha = {}


def texture_alpha(ref):
    """'opaque', 'cutout' (only fully clear pixels) or 'translucent', for a mod texture."""
    if ref in _alpha:
        return _alpha[ref]
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    p = ASSETS / ns / "textures" / f"{path}.png"
    kind = "opaque"
    if p.exists():
        a = Image.open(p).convert("RGBA").getchannel("A")
        hist = a.histogram()
        clear, part = hist[0], sum(hist[1:255])
        kind = "translucent" if part > (sum(hist) * 0.02) else "cutout" if clear or part else "opaque"
    _alpha[ref] = kind
    return kind


def model_textures(name, seen=None):
    seen = seen or set()
    if name in seen or not name.startswith(NS + ":"):
        return []
    seen.add(name)
    p = ASSETS / NS / "models" / (name.split(":", 1)[1] + ".json")
    if not p.exists():
        return []
    m = load(p)
    tex = [v for v in m.get("textures", {}).values() if not v.startswith("#")]
    return tex + model_textures(m.get("parent", ""), seen)


def render_types():
    for p in sorted((ASSETS / NS / "models/block").glob("*.json")):
        m = load(p)
        if "render_type" in m:
            continue
        kinds = {texture_alpha(t) for t in model_textures(f"{NS}:block/{p.stem}")}
        kind = "translucent" if "translucent" in kinds else "cutout" if "cutout" in kinds else None
        if kind:
            if kind == "cutout" and "leaves" in p.stem:
                kind = "cutout_mipped"
            m["render_type"] = f"minecraft:{kind}"
            save(p, m)


# ---- armour trims ------------------------------------------------------------------------------

MATERIALS = ["quartz", "iron", "gold", "diamond", "netherite", "redstone", "copper", "emerald", "lapis", "amethyst",
             "iron_darker", "gold_darker", "diamond_darker", "netherite_darker"]


def trims():
    pats = []
    for p in sorted((DATA / NS / "trim_pattern").glob("*.json")):
        t = load(p)
        name = p.stem
        if "template_item" not in t:
            t["template_item"] = f"{NS}:{name}_armor_trim_smithing_template"
            t.pop("decal", None)
            save(p, t)
        for src, dst in ((f"trims/entity/humanoid/{name}.png", f"trims/models/armor/{name}.png"),
                         (f"trims/entity/humanoid_leggings/{name}.png", f"trims/models/armor/{name}_leggings.png")):
            s, d = ASSETS / NS / "textures" / src, ASSETS / NS / "textures" / dst
            if s.exists() and not d.exists():
                d.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy(s, d)
                changed.append(d)
        pats += [f"{NS}:trims/models/armor/{name}", f"{NS}:trims/models/armor/{name}_leggings"]
    if pats:
        save(ASSETS / "minecraft/atlases/armor_trims.json", {"sources": [{
            "type": "paletted_permutations", "textures": pats, "palette_key": "trims/color_palettes/trim_palette",
            "permutations": {m: f"trims/color_palettes/{m}" for m in MATERIALS}}]})


# ---- tags --------------------------------------------------------------------------------------

def tag(t):
    vals = []
    for v in t.get("values", []):
        if isinstance(v, str) and v.lstrip("#").startswith("minecraft:"):
            v = {"id": v, "required": False}
        vals.append(v)
    return {**t, "values": vals}


# ---- structures --------------------------------------------------------------------------------

class Nbt:
    """Just enough NBT to rewrite a structure template: values are kept as (type, value) pairs."""

    @staticmethod
    def read(b, i, t):
        if t in (1, 2, 3, 4, 5, 6):
            fmt, n = {1: (">b", 1), 2: (">h", 2), 3: (">i", 4), 4: (">q", 8), 5: (">f", 4), 6: (">d", 8)}[t]
            return struct.unpack(fmt, b[i:i + n])[0], i + n
        if t == 8:
            n = struct.unpack(">H", b[i:i + 2])[0]
            return b[i + 2:i + 2 + n].decode("utf-8"), i + 2 + n
        if t == 9:
            et, n = b[i], struct.unpack(">i", b[i + 1:i + 5])[0]
            i += 5
            out = []
            for _ in range(n):
                v, i = Nbt.read(b, i, et)
                out.append(v)
            return (et, out), i
        if t == 10:
            d = {}
            while b[i] != 0:
                tt, n = b[i], struct.unpack(">H", b[i + 1:i + 3])[0]
                name = b[i + 3:i + 3 + n].decode("utf-8")
                v, i = Nbt.read(b, i + 3 + n, tt)
                d[name] = (tt, v)
            return d, i + 1
        if t in (7, 11, 12):
            n, size = struct.unpack(">i", b[i:i + 4])[0], {7: 1, 11: 4, 12: 8}[t]
            return b[i + 4:i + 4 + n * size], i + 4 + n * size
        raise ValueError(f"NBT tag type {t}")

    @staticmethod
    def write(t, v):
        if t in (1, 2, 3, 4, 5, 6):
            return struct.pack({1: ">b", 2: ">h", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}[t], v)
        if t == 8:
            e = v.encode("utf-8")
            return struct.pack(">H", len(e)) + e
        if t == 9:
            et, items = v
            return bytes([et]) + struct.pack(">i", len(items)) + b"".join(Nbt.write(et, x) for x in items)
        if t == 10:
            return b"".join(bytes([tt]) + Nbt.write(8, k) + Nbt.write(tt, x) for k, (tt, x) in v.items()) + b"\x00"
        if t in (7, 11, 12):
            return struct.pack(">i", len(v) // {7: 1, 11: 4, 12: 8}[t]) + v
        raise ValueError(f"NBT tag type {t}")


def structure_nbt(p):
    """1.21.1's DataVersion, and palette entries as {Name, Properties} (26.3 writes {id, properties})."""
    raw = gzip.decompress(p.read_bytes())
    n = struct.unpack(">H", raw[1:3])[0]
    name = raw[3:3 + n].decode("utf-8")
    root, _ = Nbt.read(raw, 3 + n, 10)
    before = Nbt.write(10, root)
    root["DataVersion"] = (3, DATA_VERSION)
    for key in ("palette",):
        if key in root:
            for entry in root[key][1][1]:
                if "id" in entry:
                    entry["Name"] = entry.pop("id")
                if "properties" in entry:
                    entry["Properties"] = entry.pop("properties")
    after = Nbt.write(10, root)
    if after != before:
        p.write_bytes(gzip.compress(bytes([10]) + Nbt.write(8, name) + after, mtime=0))
        changed.append(p)


# ---- all of it ---------------------------------------------------------------------------------

def main():
    wg = DATA / NS / "worldgen"
    for p in sorted((wg / "biome").glob("*.json")):
        b = biome(load(p))
        if b:
            save(p, b)
    old = wg / "feature"
    if old.exists():
        for p in sorted(old.glob("*.json")):
            save(wg / "configured_feature" / p.name, configured_feature(load(p)))
            p.unlink()
        old.rmdir()
    for p in sorted((wg / "placed_feature").glob("*.json")):
        f = load(p)
        g = {**f, "placement": [placement(m) for m in f["placement"]]}
        if g != f:
            save(p, g)
    for p in sorted((wg / "density_function").rglob("*.json")):
        d = load(p)
        e = density(d)
        if e != d:
            save(p, e)
    rules = wg / "material_rule"
    for p in sorted((wg / "noise_settings").glob("*.json")):
        s = noise_settings(load(p), rules)
        if s:
            save(p, s)
    if rules.exists():
        shutil.rmtree(rules)
    for p in sorted(DATA.glob("*/loot_table/**/*.json")):
        t = load(p)
        u = loot(t)
        if u != t:
            save(p, u)
    brewing = DATA / NS / "recipe/brewing"
    if brewing.exists():
        shutil.rmtree(brewing)   # 1.21.1 has no brewing recipes in data: world/ModBrewing.java
    for p in sorted((DATA / NS / "recipe").glob("*.json")):
        r = load(p)
        s = recipe(r)
        if s != r:
            save(p, s)
    for p in sorted((DATA / NS / "advancement").rglob("*.json")):
        a = load(p)
        b = advancement(a)
        if b != a:
            save(p, b)
    for p in sorted(DATA.glob("*/tags/**/*.json")):
        t = load(p)
        u = tag(t)
        if u != t:
            save(p, u)
    # The Hydragrip breathes under water: 26.3 asks the mob, 1.21.1 this tag.
    breathe = DATA / "minecraft/tags/entity_type/can_breathe_under_water.json"
    if not breathe.exists():
        save(breathe, {"replace": False, "values": [f"{NS}:hydragrip"]})
    for p in sorted((DATA / NS / "structure").glob("*.nbt")):
        structure_nbt(p)
    # The pack's formats: 26.3 gives a min/max range, 1.21.1 a pack_format (34 resources, 48 data).
    mcmeta = ROOT / "pack.mcmeta"
    pack = load(mcmeta)
    if "pack_format" not in pack["pack"]:
        save(mcmeta, {"pack": {"description": pack["pack"]["description"], "pack_format": 34, "supported_formats": [34, 48]}})
    item_models()
    render_types()
    trims()
    print(f"to_1_21: {len(changed)} files written")


if __name__ == "__main__":
    main()
