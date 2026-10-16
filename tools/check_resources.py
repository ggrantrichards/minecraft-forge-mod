#!/usr/bin/env python3
"""Checks that every block and item the Java code registers has everything it needs to
show up correctly in game, and that every JSON file only points at things that exist.

A missing or misspelled model/texture is what gives the purple-and-black checkerboard,
and a missing name shows up as "block.astralend.something". This catches both before
you ever launch the game.

Run from the project root:  python3 tools/check_resources.py   (exit code 1 on problems)
"""
import gzip
import json
import re
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NS = "astralend"
JAVA = ROOT / "src/main/java/com/ggrantrichards/astralend"
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets" / NS

# Vanilla 26.3 models this mod is allowed to use as parents.
VANILLA_PARENTS = {
    "minecraft:block/cube_all", "minecraft:block/cube_column", "minecraft:block/cube_bottom_top",
    "minecraft:block/cross", "minecraft:block/crop", "minecraft:block/slab", "minecraft:block/slab_top",
    "minecraft:block/stairs", "minecraft:block/inner_stairs", "minecraft:block/outer_stairs",
    "minecraft:block/template_wall_post", "minecraft:block/template_wall_side",
    "minecraft:block/template_wall_side_tall", "minecraft:block/wall_inventory",
    "minecraft:item/generated", "minecraft:item/handheld", "minecraft:item/handheld_rod", "minecraft:block/carpet", "minecraft:block/leaves", "minecraft:block/block",
    "minecraft:block/fence_post", "minecraft:block/fence_side", "minecraft:block/fence_inventory",
    "minecraft:block/template_fence_gate", "minecraft:block/template_fence_gate_open",
    "minecraft:block/template_fence_gate_wall", "minecraft:block/template_fence_gate_wall_open",
    "minecraft:block/template_orientable_trapdoor_bottom", "minecraft:block/template_orientable_trapdoor_top",
    "minecraft:block/template_orientable_trapdoor_open", "minecraft:block/button", "minecraft:block/button_pressed",
    "minecraft:block/button_inventory", "minecraft:block/pressure_plate_up", "minecraft:block/pressure_plate_down",
    "minecraft:block/orientable", "minecraft:block/cube_bottom_top",
    # Phase 8: the Astral Forge (like the smithing table), and upgrade accents drawn over vanilla items.
    "minecraft:block/cube", "minecraft:item/bow", "minecraft:item/crossbow", "minecraft:item/trident", "minecraft:item/mace",
} | {f"minecraft:block/door_{part}_{hinge}{op}" for part in ("bottom", "top") for hinge in ("left", "right") for op in ("", "_open")}

errors = []

# Blocks registered with .noLootTable() (structure parts that can't be broken) need no loot table.
_blocks_src = (Path(__file__).resolve().parents[1] / "src/main/java/com/ggrantrichards/astralend/ModBlocks.java").read_text()
no_loot = {m.group(1) for m in re.finditer(r'register(?:NoItem)?\("([a-z0-9_]+)"(?:(?!public static).)*?noLootTable\(\)',
                                           _blocks_src, re.S)}


def err(msg):
    errors.append(msg)


def load(path):
    try:
        return json.loads(path.read_text())
    except Exception as e:  # noqa: BLE001
        err(f"{path.relative_to(ROOT)}: not valid JSON ({e})")
        return None


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", head[16:24])


def split_id(ref):
    ns, _, path = ref.partition(":") if ":" in ref else ("minecraft", "", ref)
    return ns, path


# ------------------------------------------------------------------ what Java registers

blocks_src = (JAVA / "ModBlocks.java").read_text()
items_src = (JAVA / "ModItems.java").read_text()
blocks_with_item = set(re.findall(r'\b(?:register|stairs|slab|wall)\("([a-z0-9_]+)"', blocks_src))
blocks_no_item = set(re.findall(r'\bregisterNoItem\("([a-z0-9_]+)"', blocks_src)) - {"name"}
blocks_with_item = {b for b in blocks_with_item if b != "name" and not b.endswith("_")}  # skip "stripped_" + name
# Wood sets are registered by one helper call: woodSet("lumine", "stem", "hyphae", ...).
WOOD_PARTS = ["{n}_{log}", "{n}_{wood}", "stripped_{n}_{log}", "stripped_{n}_{wood}", "{n}_planks", "{n}_stairs", "{n}_slab",
              "{n}_fence", "{n}_fence_gate", "{n}_door", "{n}_trapdoor", "{n}_button", "{n}_pressure_plate"]
wood_sets = re.findall(r'woodSet\("([a-z0-9_]+)", "([a-z]+)", "([a-z]+)"', blocks_src)
for n, log, wood in wood_sets:
    blocks_with_item |= {part.format(n=n, log=log, wood=wood) for part in WOOD_PARTS}
blocks = blocks_with_item | blocks_no_item
plain_items = set(re.findall(r'\b(?:simple|register|ITEMS\.register)\("([a-z0-9_]+)"', items_src))
plain_items |= {f"{mob}_spawn_egg" for mob in re.findall(r'\bspawnEgg\("([a-z0-9_]+)"', items_src)}
items = blocks_with_item | plain_items
entities = set(re.findall(r'ENTITY_TYPES\.register\("([a-z0-9_]+)"', (JAVA / "entity/ModEntities.java").read_text()))

lang = load(ASSETS / "lang/en_us.json") or {}

# ------------------------------------------------------------------ models and textures

checked_models = {}


def check_texture(ref, where):
    ns, path = split_id(ref)
    if ns != NS:
        return
    png = ASSETS / "textures" / f"{path}.png"
    if not png.exists():
        err(f"{where}: texture {ref} -> missing file {png.relative_to(ROOT)}")
        return
    size = png_size(png)
    if not size:
        err(f"{png.relative_to(ROOT)}: not a PNG")
        return
    w, h = size
    if w != 16 or h % 16:
        err(f"{png.relative_to(ROOT)}: is {w}x{h}, expected 16x16 (or a 16-wide animation strip)")
    if h > 16 and not png.with_suffix(".png.mcmeta").exists():
        err(f"{png.relative_to(ROOT)}: {h // 16} frames tall but has no .mcmeta animation file")


def check_model(ref, where):
    """Checks a model and its parents; returns the set of texture variables it defines."""
    if ref in checked_models:
        return checked_models[ref]
    ns, path = split_id(ref)
    if ns != NS:
        if ref not in VANILLA_PARENTS:
            err(f"{where}: uses vanilla model {ref}, which isn't in the known list")
        checked_models[ref] = set()
        return set()
    file = ASSETS / "models" / f"{path}.json"
    if not file.exists():
        err(f"{where}: model {ref} -> missing file {file.relative_to(ROOT)}")
        checked_models[ref] = set()
        return set()
    model = load(file) or {}
    textures = model.get("textures", {})
    for key, value in textures.items():
        sprite = value["sprite"] if isinstance(value, dict) else value
        if not sprite.startswith("#"):
            check_texture(sprite, f"{file.relative_to(ROOT)} (#{key})")
    defined = set(textures)
    if "parent" in model:
        defined |= check_model(model["parent"], str(file.relative_to(ROOT)))
    checked_models[ref] = defined
    return defined


# Texture variables each vanilla parent needs filled in.
REQUIRED = {
    "minecraft:block/cube_all": {"all"}, "minecraft:block/cube_column": {"end", "side"},
    "minecraft:block/cross": {"cross"}, "minecraft:block/crop": {"crop"},
    "minecraft:block/slab": {"bottom", "top", "side"}, "minecraft:block/slab_top": {"bottom", "top", "side"},
    "minecraft:block/stairs": {"bottom", "top", "side"}, "minecraft:block/inner_stairs": {"bottom", "top", "side"},
    "minecraft:block/outer_stairs": {"bottom", "top", "side"}, "minecraft:block/template_wall_post": {"wall"},
    "minecraft:block/template_wall_side": {"wall"}, "minecraft:block/template_wall_side_tall": {"wall"},
    "minecraft:block/wall_inventory": {"wall"}, "minecraft:item/generated": {"layer0"},
    "minecraft:block/carpet": {"wool"}, "minecraft:block/leaves": {"all"},
}

# Every other vanilla template used here takes one "texture" (doors take "bottom" and "top").
for parent in VANILLA_PARENTS - set(REQUIRED):
    REQUIRED[parent] = {"bottom", "top"} if "/door_" in parent else {"texture"} if "template" in parent or any(
        k in parent for k in ("fence", "button", "pressure_plate")) else set()

for file in sorted((ASSETS / "models").rglob("*.json")):
    rel = file.relative_to(ASSETS / "models").with_suffix("").as_posix()
    model = load(file) or {}
    defined = check_model(f"{NS}:{rel}", str(file.relative_to(ROOT)))
    parent = model.get("parent")
    if parent in REQUIRED and not REQUIRED[parent] <= defined:
        err(f"{file.relative_to(ROOT)}: parent {parent} needs textures {sorted(REQUIRED[parent] - defined)}")

# ------------------------------------------------------------------ blocks

for name in sorted(blocks):
    bs = ASSETS / "blockstates" / f"{name}.json"
    if not bs.exists():
        err(f"block {NS}:{name}: missing blockstate {bs.relative_to(ROOT)}")
    else:
        data = load(bs) or {}
        refs = [v["model"] for v in data.get("variants", {}).values()]
        refs += [p["apply"]["model"] for p in data.get("multipart", [])]
        if not refs:
            err(f"{bs.relative_to(ROOT)}: no models")
        for ref in refs:
            check_model(ref, str(bs.relative_to(ROOT)))
    if f"block.{NS}.{name}" not in lang:
        err(f"block {NS}:{name}: missing English name block.{NS}.{name}")
    loot = RES / "data" / NS / "loot_table/blocks" / f"{name}.json"
    if not loot.exists() and name not in no_loot:
        err(f"block {NS}:{name}: missing loot table {loot.relative_to(ROOT)} (it would drop nothing)")

# ------------------------------------------------------------------ items

for name in sorted(items):
    idef = ASSETS / "items" / f"{name}.json"
    if not idef.exists():
        err(f"item {NS}:{name}: missing item definition {idef.relative_to(ROOT)} (would show as missing texture)")
    else:
        data = load(idef) or {}
        model = data.get("model", {})
        if model.get("type") == "minecraft:range_dispatch":       # a compass-like item: check every frame
            for e in model.get("entries", []):
                check_model(e["model"]["model"], str(idef.relative_to(ROOT)))
        elif model.get("type") != "minecraft:model" or "model" not in model:
            err(f"{idef.relative_to(ROOT)}: expected {{\"model\": {{\"type\": \"minecraft:model\", \"model\": ...}}}}")
        else:
            check_model(model["model"], str(idef.relative_to(ROOT)))
    key = f"block.{NS}.{name}" if name in blocks_with_item else f"item.{NS}.{name}"
    if key not in lang:
        err(f"item {NS}:{name}: missing English name {key}")

# ------------------------------------------------------------------ data files only reference real things

known = {f"{NS}:{n}" for n in blocks | items | entities}
# Advancements are registered by their JSON files.
ADVANCEMENTS = RES / "data" / NS / "advancement"
advancements = {f"{NS}:{f.relative_to(ADVANCEMENTS).with_suffix('').as_posix()}"
                for f in ADVANCEMENTS.rglob("*.json")} if ADVANCEMENTS.exists() else set()
known |= advancements

# Every mob needs a name, a loot table, a spawn egg and its texture.
for mob in sorted(entities):
    if f"entity.{NS}.{mob}" not in lang:
        err(f"entity {NS}:{mob}: missing English name entity.{NS}.{mob}")
    if not (RES / "data" / NS / "loot_table" / "entities" / f"{mob}.json").exists():
        err(f"entity {NS}:{mob}: missing loot table data/{NS}/loot_table/entities/{mob}.json")
    if f"{mob}_spawn_egg" not in items:
        err(f"entity {NS}:{mob}: no spawn egg")
    if not (ASSETS / "textures" / "entity" / mob / f"{mob}.png").exists():
        err(f"entity {NS}:{mob}: missing texture textures/entity/{mob}/{mob}.png")
# World generation entries (biomes, features, noise settings...) are registered by their JSON files,
# plus the biome source type registered in Java.
WORLDGEN = RES / "data" / NS / "worldgen"
worldgen_ids = {}
for file in sorted(WORLDGEN.rglob("*.json")) if WORLDGEN.exists() else []:
    kind, *path = file.relative_to(WORLDGEN).with_suffix("").parts
    ident = f"{NS}:{'/'.join(path)}"   # density functions live in subfolders: astralend:terrain/outer
    worldgen_ids.setdefault(kind, set()).add(ident)
    known.add(ident)
known |= {f"{NS}:{n}" for n in re.findall(r'(?:BIOME_SOURCES|FEATURE_TYPES|STRUCTURE_TYPES|DENSITY_FUNCTION_TYPES)\.register\("([a-z0-9_]+)"',
                                           (JAVA / "world/ModWorldgen.java").read_text())}
# Phase 5: mob effects and potions (ModEffects.java), and data-driven damage types.
effects_src = (JAVA / "ModEffects.java").read_text()
effects = set(re.findall(r'EFFECTS\.register\("([a-z0-9_]+)"', effects_src))
potion_names = set(re.findall(r'new Potion\("([a-z0-9_]+)"', effects_src))
damage_types = {f.stem for f in (RES / "data" / NS / "damage_type").glob("*.json")}
potions = set(re.findall(r'POTIONS\.register\("([a-z0-9_]+)"', effects_src))
known |= {f"{NS}:{n}" for n in effects | damage_types | potions}
# Recipes are registered by their files (the recipe-book unlock advancements name them).
known |= {f"{NS}:{f.stem}" for f in (RES / "data" / NS / "recipe").glob("*.json")}
# Phase 8: global loot modifiers (their codecs are registered in Java, each instance is a data file).
known |= {f"{NS}:{n}" for n in re.findall(r'SERIALIZERS\.register\("([a-z0-9_]+)"', (JAVA / "starforge/StarforgeLoot.java").read_text())}
known |= {f"{NS}:{f.stem}" for f in (RES / "data" / NS / "loot_modifiers").glob("*.json")}
# Data-driven registries: banner patterns and trim patterns (their asset ids name their textures).
# sounds.json: every sound file it names exists, and its subtitles are in the lang file.
SUBTITLES = set()
_sounds_file = ASSETS / "sounds.json"
if _sounds_file.exists():
    for event, entry in json.loads(_sounds_file.read_text()).items():
        if "subtitle" in entry:
            SUBTITLES.add(entry["subtitle"])
        for sound in entry.get("sounds", []):
            name = sound["name"] if isinstance(sound, dict) else sound
            ns, _, path = name.partition(":")
            if not (ROOT / "src/main/resources/assets" / ns / "sounds" / f"{path}.ogg").exists():
                err(f"sounds.json: {event} names {name}, but there's no {path}.ogg")

for kind in ("banner_pattern", "trim_pattern", "jukebox_song"):
    known |= {f"{NS}:{f.stem}" for f in (RES / "data" / NS / kind).glob("*.json")}
# Phase 6: structure templates (data/astralend/structure/*.nbt) and chest loot tables are registered by
# their files. Every id written inside a template (block names, chest loot tables) must be real too.
STRUCTURE_DIR = RES / "data" / NS / "structure"
templates = {f"{NS}:{f.stem}" for f in STRUCTURE_DIR.glob("*.nbt")}
chest_loot = {f"{NS}:chests/{f.stem}" for f in (RES / "data" / NS / "loot_table" / "chests").glob("*.json")}
known |= templates | chest_loot
for file in sorted(STRUCTURE_DIR.glob("*.nbt")):
    raw = gzip.decompress(file.read_bytes())
    for ref in set(re.findall(rb"astralend:[a-z0-9_/]+", raw)):
        ref = ref.decode()
        if ref not in known:
            err(f"{file.relative_to(ROOT)}: uses {ref}, which isn't a block or loot table of the mod")


def need(kind, ref, where):
    if ref.startswith(f"{NS}:") and ref not in worldgen_ids.get(kind, set()):
        err(f"{where}: {ref} is not a {kind} (no data/{NS}/worldgen/{kind}/{ref.split(':')[1]}.json)")


# (1.21.1 port) Configured features live in worldgen/configured_feature (26.3: worldgen/feature).
FEATURE_KIND = "configured_feature" if (WORLDGEN / "configured_feature").exists() else "feature"
# Tags are registered by their files ({"tag": "astralend:x"} ingredients name them without the #).
known |= {f"{NS}:{'/'.join(f.relative_to(RES / 'data' / NS / 'tags').with_suffix('').parts[1:])}"
          for f in (RES / "data" / NS / "tags").rglob("*.json")}
for file in sorted((WORLDGEN / "placed_feature").glob("*.json")) if WORLDGEN.exists() else []:
    need(FEATURE_KIND, (load(file) or {}).get("feature", ""), file.relative_to(ROOT))
for file in sorted((WORLDGEN / "biome").glob("*.json")) if WORLDGEN.exists() else []:
    for step in (load(file) or {}).get("features", []):
        for ref in step:
            need("placed_feature", ref, file.relative_to(ROOT))
    if f"biome.{NS}.{file.stem}" not in lang:
        err(f"biome {NS}:{file.stem}: missing English name biome.{NS}.{file.stem}")
for file in sorted((WORLDGEN).rglob("*.json")) if WORLDGEN.exists() else []:
    if file.relative_to(WORLDGEN).parts[0] in ("density_function", "noise_settings"):
        for ref in set(re.findall(rf'"({NS}:(?:region|terrain)/[a-z0-9_/]+)"', file.read_text())):
            need("density_function", ref, file.relative_to(ROOT))
for file in sorted((WORLDGEN / "noise_settings").glob("*.json")) if WORLDGEN.exists() else []:
    need("material_rule", (load(file) or {}).get("material_rule", ""), file.relative_to(ROOT))
for file in sorted((WORLDGEN / "structure").glob("*.json")) if WORLDGEN.exists() else []:
    obj = load(file) or {}
    need("template_pool", obj.get("start_pool", ""), file.relative_to(ROOT))
    biomes = obj.get("biomes", "")
    if biomes.startswith(f"#{NS}:") and not (RES / "data" / NS / "tags/worldgen/biome" / f"{biomes.split(':')[1]}.json").exists():
        err(f"{file.relative_to(ROOT)}: biome tag {biomes} has no file")
    if f"structure.{NS}.{file.stem}" not in lang:
        err(f"structure {NS}:{file.stem}: missing English name structure.{NS}.{file.stem}")
for file in sorted((WORLDGEN / "structure_set").glob("*.json")) if WORLDGEN.exists() else []:
    for s in (load(file) or {}).get("structures", []):
        need("structure", s.get("structure", ""), file.relative_to(ROOT))
for file in sorted((RES / "data/minecraft/dimension").glob("*.json")):
    gen = (load(file) or {}).get("generator", {})
    need("noise_settings", gen.get("settings", ""), file.relative_to(ROOT))
for file in sorted((RES / "data").rglob("*.json")):
    text = file.read_text()
    load(file)
    for ref in set(re.findall(rf'"({NS}:[a-z0-9_/]+)"', text)):
        if ref.startswith(f"{NS}:blocks/") or ref.startswith(f"{NS}:entities/") or ref.startswith(f"{NS}:chests/"):
            continue  # loot table random_sequence names
        if ref not in known:
            err(f"{file.relative_to(ROOT)}: refers to {ref}, which the Java code doesn't register")

for file in sorted(ASSETS.rglob("*.json")):
    load(file)
item_messages = {k for k in lang if k.startswith(f"item.{NS}.") and k.split(".")[2] in items and k.count(".") > 2}
advancement_text = {f"advancements.{NS}.{a.split('/')[-1]}.{part}" for a in advancements for part in ("title", "description")}
for extra in sorted(set(lang) - {f"block.{NS}.{n}" for n in blocks} - {f"item.{NS}.{n}" for n in items}
                    - {f"biome.{NS}.{b.split(':')[1]}" for b in worldgen_ids.get("biome", set())}
                    - {f"entity.{NS}.{n}" for n in entities} - item_messages - advancement_text
                    - {"itemGroup.astralend"}
                    - {f"effect.{NS}.{e}" for e in effects}
                    - {f"item.minecraft.{k}.effect.{p}" for p in potion_names
                       for k in ("potion", "splash_potion", "lingering_potion", "tipped_arrow")}
                    - {f"death.attack.{NS}.{d}{suffix}" for d in damage_types for suffix in ("", ".player")}
                    - {k for k in lang if k.startswith(("flare.astralend.", "maw.astralend.", "maze.astralend.",
                                                        "upgrade.astralend.", "container.astralend.",
                                                        f"block.{NS}.banner.", f"trim_pattern.{NS}.", f"jukebox_song.{NS}."))}
                    - {f"structure.{NS}.{st.split(':')[1]}" for st in worldgen_ids.get("structure", set())}
                    - SUBTITLES):
    err(f"lang/en_us.json: {extra} doesn't match anything registered")

if errors:
    print(f"{len(errors)} problem(s):")
    for e in errors:
        print("  -", e)
    sys.exit(1)
print(f"OK: {len(blocks)} blocks, {len(items)} items and {len(entities)} mobs all have blockstates, models, textures, names and loot tables; "
      f"{sum(len(v) for v in worldgen_ids.values())} world generation files all point at things that exist.")
