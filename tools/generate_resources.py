#!/usr/bin/env python3
"""Writes every Astral End blockstate, model, item definition, loot table, recipe, tag
and English name from the lists below, in the same JSON format vanilla 26.3 uses.

Run from the project root:  python3 tools/generate_resources.py
Then check the result with:   python3 tools/check_resources.py

Adding a block: register it in ModBlocks.java, add it to the right list here, draw its
texture in tools/textures/make_textures.py, then run all three scripts.
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
NS = "astralend"
ASSETS = RES / "assets" / NS
DATA = RES / "data" / NS
MC_DATA = RES / "data" / "minecraft"

# ------------------------------------------------------------------ content lists

# Stone-type families: base block + optional stairs/slab/wall, all sharing one texture.
STONE_FAMILIES = [
    # (block id, English name, has stairs/slab/wall)
    ("umbral_stone", "Umbral Stone", True),
    ("polished_umbral_stone", "Polished Umbral Stone", True),
    ("umbral_stone_bricks", "Umbral Stone Bricks", True),
    # Phase 3
    ("pale_end_stone", "Pale End Stone", True),
    ("pale_end_stone_bricks", "Pale End Stone Bricks", True),
    ("charred_end_stone", "Charred End Stone", True),
    ("sunscorched_stone", "Sunscorched Stone", True),
    ("polished_sunscorched_stone", "Polished Sunscorched Stone", True),
    ("sunscorched_stone_bricks", "Sunscorched Stone Bricks", True),
    ("observatory_quartz", "Observatory Quartz", True),
]
SIMPLE_CUBES = [
    ("chiseled_umbral_stone", "Chiseled Umbral Stone"),
    ("void_crystal_ore", "Void Crystal Ore"),
    ("void_crystal_block", "Block of Void Crystal"),
    ("meteoric_iron_ore", "Meteoric Iron Ore"),
    ("meteoric_iron_block", "Block of Meteoric Iron"),
    ("raw_meteoric_iron_block", "Block of Raw Meteoric Iron"),
    ("stardust_block", "Stardust"),
    ("astral_glass", "Astral Glass"),
    ("nebula_moss", "Nebula Moss"),
    # Phase 3
    ("pale_end_stone_tiles", "Pale End Stone Tiles"),
    ("comet_ice", "Comet Ice"),
    ("solarite_ore", "Solarite Ore"),
    ("solarite_block", "Block of Solarite"),
    ("raw_solarite_block", "Block of Raw Solarite"),
    ("star_inlay_observatory_quartz", "Star Inlay Observatory Quartz"),
    ("gravity_anomaly", "Gravity Anomaly"),
]
PILLARS = [
    ("leviathan_bone_block", "Leviathan Bone Block", "leviathan_bone"),
    ("observatory_quartz_pillar", "Observatory Quartz Pillar", "observatory_quartz_pillar"),
    ("pollinator_husk", "Pollinator Husk", "pollinator_husk"),
]
CROSS_PLANTS = [
    ("lumineshroom", "Lumineshroom"), ("starbloom", "Starbloom"),
    ("shimmer_grass", "Shimmer Grass"), ("ashen_lumineshroom", "Ashen Lumineshroom"),
    ("petrified_astral_vine", "Petrified Astral Vine"),
]
ITEMS = [
    ("void_crystal", "Void Crystal"),
    ("raw_meteoric_iron", "Raw Meteoric Iron"),
    ("meteoric_iron_ingot", "Meteoric Iron Ingot"),
    ("meteoric_iron_nugget", "Meteoric Iron Nugget"),
    ("comet_cress", "Comet Cress"),
    ("raw_solarite", "Raw Solarite"),
    ("solarite_ingot", "Solarite Ingot"),
]

PLURAL = {"umbral_stone_bricks": "umbral_stone_brick", "pale_end_stone_bricks": "pale_end_stone_brick",
          "sunscorched_stone_bricks": "sunscorched_stone_brick"}  # "bricks" -> "brick_stairs", like vanilla


def variant_id(base, kind):
    return f"{PLURAL.get(base, base)}_{kind}"


def english_variant(name, kind):
    stem = name[:-1] if name.endswith("Bricks") else name
    return f"{stem} {kind.capitalize()}"


# ------------------------------------------------------------------ writers

written = []


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")
    written.append(path)


def rid(name):
    return f"{NS}:{name}"


def blockstate(name, obj):
    write(ASSETS / "blockstates" / f"{name}.json", obj)


def block_model(name, obj):
    write(ASSETS / "models" / "block" / f"{name}.json", obj)


def item_model(name, obj):
    write(ASSETS / "models" / "item" / f"{name}.json", obj)


def item_def(name, model):
    write(ASSETS / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": model}})


def loot(name, pools, extra=None):
    obj = {"type": "minecraft:block"}
    if extra:
        obj.update(extra)
    obj["pools"] = pools
    obj["random_sequence"] = f"{NS}:blocks/{name}"
    write(DATA / "loot_table" / "blocks" / f"{name}.json", obj)


def drop_self(name):
    loot(name, [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid(name)}],
        "rolls": 1,
    }])


def recipe(name, obj):
    write(DATA / "recipe" / f"{name}.json", obj)


def shaped(name, result, pattern, key, count=1, category="building"):
    res = {"id": rid(result)} if count == 1 else {"count": count, "id": rid(result)}
    recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "key": key,
                  "pattern": pattern, "result": res})


def shapeless(name, result, ingredients, count=1, category="misc"):
    res = {"id": rid(result)} if count == 1 else {"count": count, "id": rid(result)}
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category,
                  "ingredients": ingredients, "result": res})


def cooking(name, kind, ingredient, result, xp, time):
    recipe(name, {"type": f"minecraft:{kind}", "category": "misc", "cookingtime": time,
                  "experience": xp, "group": result, "ingredient": ingredient,
                  "result": {"id": rid(result)}})


def stonecutting(src, result, count=1):
    res = {"id": rid(result)} if count == 1 else {"count": count, "id": rid(result)}
    recipe(f"{result}_from_{src}_stonecutting",
           {"type": "minecraft:stonecutting", "ingredient": rid(src), "result": res})


lang = {"itemGroup.astralend": "The Astral End"}
tags = {}  # (registry, "namespace:path") -> [ids]


def tag(registry, name, value):
    tags.setdefault((registry, name), []).append(value)


# ------------------------------------------------------------------ simple cubes

def cube(name, texture=None):
    blockstate(name, {"variants": {"": {"model": rid(f"block/{name}")}}})
    block_model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{texture or name}")}})
    item_def(name, rid(f"block/{name}"))


# ------------------------------------------------------------------ stairs / slab / wall

def stairs_variants(name):
    straight, inner, outer = rid(f"block/{name}"), rid(f"block/{name}_inner"), rid(f"block/{name}_outer")
    base_y = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, y0 in base_y.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = straight if shape == "straight" else inner if shape.startswith("inner") else outer
                left = shape.endswith("left")
                if half == "bottom":
                    y = (y0 - 90) % 360 if left else y0
                    x = 0
                else:
                    y = y0 if (left or shape == "straight") else (y0 + 90) % 360
                    x = 180
                v = {"model": model}
                if x:
                    v["x"] = x
                if y:
                    v["y"] = y
                if x or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    return {"variants": variants}


def stone_family(base, english, texture=None):
    tex = rid(f"block/{texture or base}")
    stairs, slab, wall = variant_id(base, "stairs"), variant_id(base, "slab"), variant_id(base, "wall")

    blockstate(stairs, stairs_variants(stairs))
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        block_model(stairs + suffix, {"parent": f"minecraft:block/{parent}",
                                      "textures": {"bottom": tex, "side": tex, "top": tex}})
    item_def(stairs, rid(f"block/{stairs}"))

    blockstate(slab, {"variants": {
        "type=bottom": {"model": rid(f"block/{slab}")},
        "type=double": {"model": rid(f"block/{base}")},
        "type=top": {"model": rid(f"block/{slab}_top")},
    }})
    block_model(slab, {"parent": "minecraft:block/slab", "textures": {"bottom": tex, "side": tex, "top": tex}})
    block_model(slab + "_top", {"parent": "minecraft:block/slab_top", "textures": {"bottom": tex, "side": tex, "top": tex}})
    item_def(slab, rid(f"block/{slab}"))

    parts = [{"apply": {"model": rid(f"block/{wall}_post")}, "when": {"up": "true"}}]
    for height, model in (("low", f"{wall}_side"), ("tall", f"{wall}_side_tall")):
        for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            apply = {"model": rid(f"block/{model}"), "uvlock": True}
            if y:
                apply["y"] = y
            parts.append({"apply": apply, "when": {side: height}})
    blockstate(wall, {"multipart": parts})
    for suffix, parent in (("_post", "template_wall_post"), ("_side", "template_wall_side"),
                           ("_side_tall", "template_wall_side_tall"), ("_inventory", "wall_inventory")):
        block_model(wall + suffix, {"parent": f"minecraft:block/{parent}", "textures": {"wall": tex}})
    item_def(wall, rid(f"block/{wall}_inventory"))

    for kind, bid in (("stairs", stairs), ("slab", slab), ("wall", wall)):
        lang[f"block.{NS}.{bid}"] = english_variant(english, kind)
        tag("block", "minecraft:mineable/pickaxe", rid(bid))
        tag_name = {"stairs": "minecraft:stairs", "slab": "minecraft:slabs", "wall": "minecraft:walls"}[kind]
        tag("block", tag_name, rid(bid))
        tag("item", tag_name, rid(bid))
    drop_self(stairs)
    drop_self(wall)
    loot(slab, [{
        "entries": [{
            "type": "minecraft:item",
            "modifier": [
                {"type": "minecraft:set_count", "condition": {"type": "minecraft:match_block", "blocks": rid(slab),
                                                              "state": {"type": "double"}}, "count": 2},
                {"type": "minecraft:explosion_decay"},
            ],
            "name": rid(slab),
        }],
        "rolls": 1,
    }])

    shaped(stairs, stairs, ["#  ", "## ", "###"], {"#": rid(base)}, count=4)
    shaped(slab, slab, ["###"], {"#": rid(base)}, count=6)
    shaped(wall, wall, ["###", "###"], {"#": rid(base)}, count=6, category="misc")
    return stairs, slab, wall


# ------------------------------------------------------------------ plants

def cross_plant(name, item_texture=None):
    blockstate(name, {"variants": {"": {"model": rid(f"block/{name}")}}})
    block_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{name}")}})
    item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"block/{item_texture or name}")}})
    item_def(name, rid(f"item/{name}"))


# ------------------------------------------------------------------ world generation (Phase 2)
#
# How the End is built:
#  - data/minecraft/dimension/the_end.json swaps the End's biome source for ours
#    (astralend:astral_end, see AstralEndBiomeSource.java) and its noise settings for ours.
#  - Our noise settings are vanilla's End terrain unchanged, plus a "temperature" noise that the
#    biome source reads to decide where Nebula Fields go, and a material rule that tops islands
#    in Nebula Fields with Nebula Moss.
#  - The Nebula Fields biome lists the features (plants) that decorate it.

WG = DATA / "worldgen"

# Vanilla's End final density, except that "minecraft:end/sloped_cheese" (islands + 3D noise) is
# replaced by "astralend:terrain/shaped", which is the same thing outside the reshaped biomes.
END_FINAL_DENSITY = {
    "type": "minecraft:add",
    "left": {"type": "minecraft:squeeze", "input": {
        "type": "minecraft:interpolated", "cell_size_xz": 8, "cell_size_y": 4, "input": {
            "type": "minecraft:mul",
            "left": {"type": "minecraft:blend_density", "input": {
                "type": "minecraft:lerp",
                "alpha": {"type": "minecraft:gradient", "axis": "y", "from_coordinate": 4, "from_value": 0.0,
                          "to_coordinate": 32, "to_value": 1.0},
                "first": -0.234375,
                "second": {
                    "type": "minecraft:lerp",
                    "alpha": {"type": "minecraft:gradient", "axis": "y", "from_coordinate": 56, "from_value": 1.0,
                              "to_coordinate": 312, "to_value": 0.0},
                    "first": -23.4375,
                    "second": "astralend:terrain/shaped",
                },
            }},
            "right": 0.64,
        }}},
    "right": {"type": "minecraft:beardifier"},
}


def placed(name, obj):
    write(WG / "placed_feature" / f"{name}.json", obj)


def feature(name, obj):
    write(WG / "feature" / f"{name}.json", obj)


def trapezoid(n):
    return {"type": "minecraft:trapezoid", "max": n, "min": -n, "plateau": 0}


def plant_patch(name, block, rarity, count, spread, vertical=2, on=None):
    """A patch of plants, placed exactly like vanilla's mushroom and flower patches.
    on: if given, a list of blocks it must sit on (for blocks that would "survive" in mid-air)."""
    feature(block, {"type": "minecraft:simple_block", "to_place": {"id": rid(block)}})
    placed(name, {"feature": rid(block), "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:offset", "x": trapezoid(spread), "y": trapezoid(vertical), "z": trapezoid(spread)},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
            {"type": "minecraft:would_survive", "state": rid(block)},
        ] + ([{"type": "minecraft:matching_blocks", "blocks": on, "offset": [0, -1, 0]}] if on else [])}},
    ]})


# ---- Terrain
#
# Region noises (also read by the biome source through the noise router; the thresholds here
# must match AstralRegions.java):
REGION_NEBULA = 0.18      # nebula > this: Nebula Fields
REGION_COLD = -0.2        # nebula < this: the "cold" band (Aurora Shallows / Comet Wake)
REGION_COLD_BELT = 0.2    # |belt| beyond this in the cold band
REGION_MID_BELT = 0.3     # |belt| beyond this in the middle band (Dark Matter Belt / Corona)
BLEND = 0.03              # terrain blends between shapes over +-this much noise at a border


def df(name, obj):
    write(WG / "density_function" / f"{name}.json", obj)


def add(a, b):
    return {"type": "minecraft:add", "left": a, "right": b}


def sub(a, b):
    return {"type": "minecraft:sub", "left": a, "right": b}


def mul(a, b):
    return {"type": "minecraft:mul", "left": a, "right": b}


def dmin(a, b):
    return {"type": "minecraft:min", "left": a, "right": b}


def dmax(a, b):
    return {"type": "minecraft:max", "left": a, "right": b}


def clamp(x, lo, hi):
    return {"type": "minecraft:clamp", "input": x, "min": lo, "max": hi}


def lerp(alpha, first, second):
    return {"type": "minecraft:lerp", "alpha": alpha, "first": first, "second": second}


def y_line(y0, v0, y1, v1):
    """A straight line in y: v0 at y0, v1 at y1 (clamped beyond those)."""
    return {"type": "minecraft:gradient", "axis": "y", "from_coordinate": y0, "from_value": v0,
            "to_coordinate": y1, "to_value": v1}


def ramp(x, at, width=BLEND):
    """0 below at-width, 1 above at+width, linear in between."""
    return clamp(mul(add(x, -(at - width)), 1.0 / (2 * width)), 0.0, 1.0)


def terrain():
    nebula, belt, ruin = rid("region/nebula"), rid("region/belt"), rid("region/ruin")
    df("region/nebula", {"type": "minecraft:noise", "noise": "minecraft:vegetation", "xz_scale": 0.25, "y_scale": 0.0})
    df("region/belt", {"type": "minecraft:noise", "noise": "minecraft:temperature", "xz_scale": 1.0, "y_scale": 0.0})
    df("region/ruin", {"type": "minecraft:noise", "noise": "minecraft:ridge", "xz_scale": 0.12, "y_scale": 0.0})

    # Weights (0..1) of each reshaped biome. They are 0 within 1,024 blocks of the centre, so the
    # main island and the gap around it are exactly vanilla.
    outer = clamp(mul(add({"type": "minecraft:distance_to_point", "metric": "euclidean", "point": [0, 0, 0]},
                          -1024.0), 1.0 / 64), 0.0, 1.0)
    df("terrain/outer", outer)
    cold = ramp(mul(nebula, -1.0), -REGION_COLD)
    middle = mul(ramp(nebula, REGION_COLD), sub(1.0, ramp(nebula, REGION_NEBULA)))
    df("terrain/weight/aurora_shallows", mul(rid("terrain/outer"), mul(cold, ramp(belt, REGION_COLD_BELT))))
    df("terrain/weight/comet_wake", mul(rid("terrain/outer"), mul(cold, ramp(mul(belt, -1.0), REGION_COLD_BELT))))
    df("terrain/weight/dark_matter_belt", mul(rid("terrain/outer"), mul(middle, ramp(belt, REGION_MID_BELT))))

    islands, noise = "minecraft:end/islands", "minecraft:end/base_3d_noise"

    # Aurora Shallows: wide, thin, flat-topped shelves. The top is level at y=54; the shelf is
    # thickest (about 10 blocks) where vanilla would put an island's centre, and thins to a
    # see-through edge well beyond where vanilla's island would end.
    thickness = mul(add(add(islands, 0.12), mul(noise, 0.2)), 14.0)
    df("terrain/aurora_shallows", dmin(y_line(0, 54.5 * 0.25, 128, (54.5 - 128) * 0.25),
                                       add(y_line(0, -54.5 * 0.25, 128, (128 - 54.5) * 0.25), mul(thickness, 0.25))))

    # Comet Wake: vanilla islands plus a river of small floating debris between y=30 and 70.
    band = dmin(y_line(30, 0.0, 45, 1.0), y_line(55, 1.0, 70, 0.0))
    debris = sub(add(mul({"type": "minecraft:noise", "noise": "minecraft:ridge", "xz_scale": 4.0, "y_scale": 4.0}, 3.0), -1.7),
                 mul(sub(1.0, band), 5.0))
    df("terrain/comet_wake", dmax("minecraft:end/sloped_cheese", debris))

    # Dark Matter Belt: jagged rings. The middle of every big island is hollowed out, leaving a
    # ring around an empty centre, and the 3D noise is stronger, making the rings ragged.
    df("terrain/dark_matter_belt", sub(add("minecraft:end/sloped_cheese", mul(noise, 0.5)),
                                       mul(dmax(add(islands, -0.2), 0.0), 6.0)))

    shaped = "minecraft:end/sloped_cheese"
    for biome in ("dark_matter_belt", "comet_wake", "aurora_shallows"):
        shaped = lerp(rid(f"terrain/weight/{biome}"), shaped, rid(f"terrain/{biome}"))
    df("terrain/shaped", shaped)


def worldgen():
    # The End dimension: vanilla dimension type, our biome source and noise settings.
    write(RES / "data/minecraft/dimension/the_end.json", {
        "type": "minecraft:the_end",
        "generator": {"type": "minecraft:noise", "biome_source": {"type": rid("astral_end")}, "settings": rid("end")},
    })

    # Vanilla's End terrain, reshaped in some astral biomes (see terrain() below), plus three region
    # noises in the router's climate slots, which the biome source samples (see AstralRegions.java).
    write(WG / "noise_settings" / "end.json", {
        "default_block": "minecraft:end_stone",
        "default_fluid": "minecraft:air",
        "disable_mob_generation": True,
        "legacy_random_source": True,
        "material_rule": rid("end"),
        "noise": {"height": 128, "min_y": 0},
        "noise_router": {
            "chunk_surface_level": 0.0, "continents": 0.0, "depth": 0.0,
            "erosion": "minecraft:end/islands",
            "final_density": END_FINAL_DENSITY,
            "ridges": rid("region/ruin"),
            "temperature": rid("region/nebula"),
            "vegetation": rid("region/belt"),
        },
        "sea_level": 0,
        "spawn_target": [],
    })

    # Surface: the top block of every island floor in Nebula Fields becomes Nebula Moss;
    # everything else is End Stone, exactly like vanilla.
    def in_biome(biome, rule):
        return {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [rid(biome)]},
                "then_run": rule}

    def block(name):
        return {"type": "minecraft:block", "result_state": rid(name)}

    write(WG / "material_rule" / "end.json", {"type": "minecraft:sequence", "sequence": [
        in_biome("nebula_fields", {"type": "minecraft:condition", "if_true": "minecraft:on_floor",
                                   "then_run": block("nebula_moss")}),
        # Whole islands of a different stone:
        in_biome("aurora_shallows", block("pale_end_stone")),
        in_biome("comet_wake", block("charred_end_stone")),
        in_biome("dark_matter_belt", block("umbral_stone")),
        # The Corona: a bleached crust of Sunscorched Stone over Charred End Stone.
        in_biome("corona", {"type": "minecraft:sequence", "sequence": [
            {"type": "minecraft:condition", "if_true": "minecraft:under_floor", "then_run": block("sunscorched_stone")},
            block("charred_end_stone")]}),
        "minecraft:end",
    ]})
    terrain()

    # Plants of the Nebula Fields.
    plant_patch("nebula_lumineshrooms", "lumineshroom", rarity=1, count=32, spread=6)
    plant_patch("nebula_starblooms", "starbloom", rarity=2, count=28, spread=6)
    feature("astral_vine", {
        "type": "minecraft:block_column",
        "allowed_placement": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
        "direction": "up",
        "layers": [
            {"height": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 5},
             "provider": {"id": rid("astral_vine_plant")}},
            {"height": 1, "provider": {"type": "minecraft:randomized_int", "property": "age",
                                       "source": {"id": rid("astral_vine"), "properties": {"age": "0"}},
                                       "values": {"type": "minecraft:uniform", "min_inclusive": 17, "max_inclusive": 25}}},
        ],
        "prioritize_tip": True,
    })
    placed("nebula_astral_vines", {"feature": rid("astral_vine"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": 2},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": 10},
        {"type": "minecraft:offset", "x": trapezoid(5), "y": trapezoid(1), "z": trapezoid(5)},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
            {"type": "minecraft:matching_blocks", "blocks": [rid("nebula_moss")], "offset": [0, -1, 0]},
        ]}},
    ]})

    # The biome itself. Endermen spawn as in the rest of the End; violet fog and drifting motes.
    write(WG / "biome" / "nebula_fields.json", {
        "attributes": {
            "minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": {
                "monster": [{"type": "minecraft:enderman", "count": 4, "weight": 10}]}}, "modifier": "overlay"},
            "minecraft:visual/ambient_particles": {"argument": [
                {"particle": {"type": "minecraft:reverse_portal"}, "probability": 0.004}], "modifier": "append"},
            "minecraft:visual/fog_color": "#2b1d45",
        },
        "carvers": [],
        "downfall": 0.5,
        "effects": {"water_color": "#7a5fc4"},
        # Features by generation step. Step 4 (surface structures) has the return gateway, the
        # same as in the End Highlands; step 9 (vegetal decoration) has the plants.
        "features": [[], [], [], [], ["minecraft:end_gateway_return"], [], [], [], [],
                     [rid("nebula_lumineshrooms"), rid("nebula_starblooms"), rid("nebula_astral_vines")]],
        "has_precipitation": False,
        "temperature": 0.5,
    })

    tag("worldgen/biome", "minecraft:is_end", rid("nebula_fields"))
    # Biomes in this tag get the End's low gravity. To turn it off, override this tag with an
    # empty list in a datapack.
    tag("worldgen/biome", f"{NS}:low_gravity", rid("nebula_fields"))
    # Biomes where Gravity Anomalies flip gravity. Empty this tag in a datapack to turn it off.
    tag("worldgen/biome", f"{NS}:gravity_inversion", rid("dark_matter_belt"))
    lang[f"biome.{NS}.nebula_fields"] = "Nebula Fields"


# ------------------------------------------------------------------ world generation (Phase 3)
#
# Five more biomes. The biome source (AstralRegions.java) picks them from two region noises; our
# terrain density function reshapes islands in some of them (flat shelves, rings, debris); the
# material rule gives each its stone; and the features below decorate them.

def ore(name, block, target, size, discard=0.0):
    feature(name, {"type": "minecraft:ore", "discard_chance_on_air_exposure": discard, "size": size,
                   "targets": [{"state": rid(block),
                                "target": {"predicate_type": "minecraft:block_match", "block": target}}]})


def placed_ore(name, feature_name, count, min_y=0, max_y=128):
    placed(name, {"feature": rid(feature_name), "placement": [
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                      "max_inclusive": {"absolute": max_y},
                                                      "min_inclusive": {"absolute": min_y}}},
        {"type": "minecraft:biome"},
    ]})


def surface_feature(name, feature_name, rarity=1, count=1, y_offset=0, extra=None):
    """Placed on the island surface of random columns (like vanilla's forest rocks)."""
    placement = []
    if rarity > 1:
        placement.append({"type": "minecraft:rarity_filter", "chance": rarity})
    if count != 1:
        placement.append({"type": "minecraft:count", "count": count})
    placement += [{"type": "minecraft:in_square"},
                  {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}]
    if y_offset:
        placement.append({"type": "minecraft:offset", "x": 0, "z": 0,
                          "y": {"type": "minecraft:uniform", "min_inclusive": y_offset, "max_inclusive": 0}})
    placement += (extra or []) + [{"type": "minecraft:biome"}]
    placed(name, {"feature": rid(feature_name), "placement": placement})


def java_feature(name):
    """A feature coded in Java (world/feature/*.java), with no settings."""
    feature(name, {"type": rid(name)})


def end_biome(name, english, fog, water, features_step4=(), features_step7=(), features_step9=(),
              particle=None, particle_chance=0.0):
    attributes = {
        "minecraft:gameplay/natural_mob_spawns": {"argument": {"spawn_costs": {}, "spawns_by_category": {
            "monster": [{"type": "minecraft:enderman", "count": 4, "weight": 10}]}}, "modifier": "overlay"},
        "minecraft:visual/fog_color": fog,
    }
    if particle:
        attributes["minecraft:visual/ambient_particles"] = {"argument": [
            {"particle": {"type": particle}, "probability": particle_chance}], "modifier": "append"}
    steps = [[] for _ in range(10)]
    steps[4] = ["minecraft:end_gateway_return"] + list(features_step4)
    steps[7] = list(features_step7)   # underground ores
    steps[9] = list(features_step9)   # vegetation
    write(WG / "biome" / f"{name}.json", {
        "attributes": attributes, "carvers": [], "downfall": 0.5, "effects": {"water_color": water},
        "features": steps, "has_precipitation": False, "temperature": 0.5,
    })
    tag("worldgen/biome", "minecraft:is_end", rid(name))
    lang[f"biome.{NS}.{name}"] = english


def worldgen_phase3():
    air = {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}

    # ---- Aurora Shallows: Pale End Stone shelves, Shimmer Grass, standing stones, bone ribs
    plant_patch("aurora_shimmer_grass", "shimmer_grass", rarity=1, count=40, spread=7)
    feature("standing_stone", {"type": "minecraft:block_column", "allowed_placement": air, "direction": "up",
                               "layers": [{"height": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3},
                                           "provider": {"id": rid("pale_end_stone_bricks")}},
                                          {"height": 1, "provider": {"id": rid("pale_end_stone_tiles")}}],
                               "prioritize_tip": True})
    surface_feature("aurora_standing_stones", "standing_stone", rarity=5,
                    extra=[{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                        air, {"type": "minecraft:matching_blocks", "blocks": [rid("pale_end_stone")], "offset": [0, -1, 0]}]}}])
    feature("leviathan_rib", {"type": "minecraft:block_column", "allowed_placement": air, "direction": "up",
                              "layers": [{"height": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 7},
                                          "provider": {"id": rid("leviathan_bone_block"), "properties": {"axis": "y"}}}],
                              "prioritize_tip": False})
    placed("aurora_leviathan_ribs", {"feature": rid("leviathan_rib"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": 9},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:count", "count": 5},
        {"type": "minecraft:offset", "x": trapezoid(4), "y": trapezoid(1), "z": trapezoid(4)},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            air, {"type": "minecraft:matching_blocks", "blocks": [rid("pale_end_stone")], "offset": [0, -1, 0]}]}},
        {"type": "minecraft:biome"},
    ]})
    end_biome("aurora_shallows", "Aurora Shallows", fog="#1f3b4d", water="#5fc8c2",
              features_step4=[rid("aurora_standing_stones"), rid("aurora_leviathan_ribs")],
              features_step9=[rid("aurora_shimmer_grass")], particle="minecraft:end_rod", particle_chance=0.0006)

    # ---- Comet Wake: Charred End Stone, Stardust veins, Comet Ice, Comet Cores, Comet Cress
    ore("stardust_vein", "stardust_block", rid("charred_end_stone"), size=14)
    placed_ore("comet_wake_stardust_veins", "stardust_vein", count=7, min_y=20, max_y=80)
    feature("comet_ice_shard", {"type": "minecraft:block_blob",
                                "can_place_on": {"type": "minecraft:matching_blocks", "blocks": [rid("charred_end_stone")]},
                                "state": rid("comet_ice")})
    surface_feature("comet_wake_ice_shards", "comet_ice_shard", rarity=3)
    java_feature("comet_core")
    surface_feature("comet_wake_comet_cores", "comet_core", rarity=6)
    java_feature("stardust_underhang")
    placed("comet_wake_stardust_underhangs", {"feature": rid("stardust_underhang"), "placement": [
        {"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
    end_biome("comet_wake", "Comet Wake", fog="#2d2533", water="#8f7fb0",
              features_step4=[rid("comet_wake_comet_cores"), rid("comet_wake_ice_shards")],
              features_step7=[rid("comet_wake_stardust_veins")],
              features_step9=[rid("comet_wake_stardust_underhangs")], particle="minecraft:ash", particle_chance=0.004)

    # ---- Dark Matter Belt: Umbral Stone rings, Void Crystal Ore, gravity inversion pockets
    ore("void_crystal_vein", "void_crystal_ore", rid("umbral_stone"), size=5, discard=0.5)
    placed_ore("dark_matter_void_crystal", "void_crystal_vein", count=9, min_y=10, max_y=90)
    java_feature("inversion_pocket")
    placed("dark_matter_inversion_pockets", {"feature": rid("inversion_pocket"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
    end_biome("dark_matter_belt", "Dark Matter Belt", fog="#07060b", water="#1a1030",
              features_step4=[rid("dark_matter_inversion_pockets")],
              features_step7=[rid("dark_matter_void_crystal")])

    # ---- Shattered Observatory: quartz ruins, overgrown with vines and Starblooms
    java_feature("observatory_ruin")
    surface_feature("observatory_ruins", "observatory_ruin", rarity=2)
    plant_patch("observatory_starblooms", "starbloom", rarity=1, count=24, spread=6)
    placed("observatory_astral_vines", {"feature": rid("astral_vine"), "placement": [
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": 12},
        {"type": "minecraft:offset", "x": trapezoid(6), "y": trapezoid(2), "z": trapezoid(6)},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            air, {"type": "minecraft:matching_blocks", "blocks": ["minecraft:end_stone", rid("observatory_quartz")],
                  "offset": [0, -1, 0]}]}},
    ]})
    end_biome("shattered_observatory", "Shattered Observatory", fog="#262a47", water="#6b7fd6",
              features_step4=[rid("observatory_ruins")],
              features_step9=["minecraft:chorus_plant", rid("observatory_starblooms"), rid("observatory_astral_vines")],
              particle="minecraft:enchant", particle_chance=0.002)

    # ---- The Corona: Sunscorched Stone over Charred End Stone; Solarite; adapted and dead life
    ore("solarite_surface_vein", "solarite_ore", rid("sunscorched_stone"), size=8)
    surface_feature("corona_solarite_surface", "solarite_surface_vein", count=5, y_offset=-3)
    ore("solarite_deep_vein", "solarite_ore", rid("charred_end_stone"), size=3)
    placed_ore("corona_solarite_deep", "solarite_deep_vein", count=2, min_y=10, max_y=70)
    plant_patch("corona_flareblooms", "flarebloom", rarity=2, count=14, spread=6)
    plant_patch("corona_sunscale_lichen", "sunscale_lichen", rarity=1, count=36, spread=7)
    plant_patch("corona_ashen_lumineshrooms", "ashen_lumineshroom", rarity=2, count=10, spread=5)
    plant_patch("corona_petrified_vines", "petrified_astral_vine", rarity=3, count=6, spread=5)
    plant_patch("corona_pollinator_husks", "pollinator_husk", rarity=12, count=2, spread=3, vertical=1,
                on=[rid("sunscorched_stone"), rid("charred_end_stone")])
    end_biome("corona", "The Corona", fog="#4a2616", water="#d9823b",
              features_step7=[rid("corona_solarite_deep")],
              features_step4=[rid("corona_solarite_surface")],
              features_step9=[rid("corona_sunscale_lichen"), rid("corona_flareblooms"), rid("corona_ashen_lumineshrooms"),
                              rid("corona_petrified_vines"), rid("corona_pollinator_husks")],
              particle="minecraft:white_ash", particle_chance=0.01)


def main():
    # Clear previously generated folders so renamed/removed content doesn't linger.
    for folder in (ASSETS / "blockstates", ASSETS / "models", ASSETS / "items", DATA / "loot_table",
                   DATA / "recipe", RES / "data" / NS / "tags", MC_DATA / "tags", DATA / "worldgen",
                   MC_DATA / "dimension"):
        shutil.rmtree(folder, ignore_errors=True)

    family_variants = {}
    for base, english, has_variants in STONE_FAMILIES:
        cube(base)
        lang[f"block.{NS}.{base}"] = english
        tag("block", "minecraft:mineable/pickaxe", rid(base))
        drop_self(base)
        if has_variants:
            family_variants[base] = stone_family(base, english)

    for name, english in SIMPLE_CUBES:
        cube(name)
        lang[f"block.{NS}.{name}"] = english

    for name, english, texture in PILLARS:
        blockstate(name, {"variants": {
            "axis=x": {"model": rid(f"block/{name}"), "x": 90, "y": 90},
            "axis=y": {"model": rid(f"block/{name}")},
            "axis=z": {"model": rid(f"block/{name}"), "x": 90},
        }})
        block_model(name, {"parent": "minecraft:block/cube_column",
                           "textures": {"end": rid(f"block/{texture}_top"), "side": rid(f"block/{texture}_side")}})
        item_def(name, rid(f"block/{name}"))
        lang[f"block.{NS}.{name}"] = english
        tag("block", "minecraft:mineable/pickaxe", rid(name))
        drop_self(name)

    for name, english in CROSS_PLANTS:
        cross_plant(name)
        lang[f"block.{NS}.{name}"] = english

    # Astral Vine: grows upward like twisting vines (a head block and a body block).
    for name, texture in (("astral_vine", "astral_vine"), ("astral_vine_plant", "astral_vine_plant")):
        blockstate(name, {"variants": {"": {"model": rid(f"block/{name}")}}})
        block_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{texture}")}})
    item_model("astral_vine", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("block/astral_vine")}})
    item_def("astral_vine", rid("item/astral_vine"))
    lang[f"block.{NS}.astral_vine"] = "Astral Vine"
    lang[f"block.{NS}.astral_vine_plant"] = "Astral Vine Plant"
    for name in ("astral_vine", "astral_vine_plant"):
        tag("block", "minecraft:climbable", rid(name))
        loot(name, [{
            "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "condition": {"type": "minecraft:any_of", "terms": [
                    "minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}, "name": rid("astral_vine")},
                {"type": "minecraft:item", "condition": {"type": "minecraft:table_bonus",
                                                         "chances": [0.33, 0.55, 0.77, 1.0],
                                                         "enchantment": "minecraft:fortune"},
                 "name": rid("astral_vine")},
            ]}],
            "rolls": 1,
        }])

    # Flarebloom: a two-block-tall flower (like a lilac). Only the lower half drops the item.
    blockstate("flarebloom", {"variants": {
        "half=lower": {"model": rid("block/flarebloom_bottom")},
        "half=upper": {"model": rid("block/flarebloom_top")},
    }})
    for half in ("bottom", "top"):
        block_model(f"flarebloom_{half}", {"parent": "minecraft:block/cross",
                                           "textures": {"cross": rid(f"block/flarebloom_{half}")}})
    item_model("flarebloom", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("block/flarebloom_top")}})
    item_def("flarebloom", rid("item/flarebloom"))
    lang[f"block.{NS}.flarebloom"] = "Flarebloom"
    loot("flarebloom", [{
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": rid("flarebloom"), "condition": {
            "type": "minecraft:match_block", "blocks": rid("flarebloom"), "state": {"half": "lower"}}}],
        "rolls": 1,
    }])
    tag("block", "minecraft:tall_flowers", rid("flarebloom"))
    tag("item", "minecraft:tall_flowers", rid("flarebloom"))

    # Sunscale Lichen: a carpet.
    blockstate("sunscale_lichen", {"variants": {"": {"model": rid("block/sunscale_lichen")}}})
    block_model("sunscale_lichen", {"parent": "minecraft:block/carpet", "textures": {"wool": rid("block/sunscale_lichen")}})
    item_def("sunscale_lichen", rid("block/sunscale_lichen"))
    lang[f"block.{NS}.sunscale_lichen"] = "Sunscale Lichen"
    drop_self("sunscale_lichen")

    # Comet Cress: a crop that hangs from the underside of Stardust, so its models are
    # turned upside down (x: 180) and it grows downward.
    blockstate("comet_cress", {"variants": {
        f"age={age}": {"model": rid(f"block/comet_cress_stage{age}"), "x": 180} for age in range(4)
    }})
    for age in range(4):
        block_model(f"comet_cress_stage{age}", {"parent": "minecraft:block/crop",
                                                "textures": {"crop": rid(f"block/comet_cress_stage{age}")}})
    lang[f"block.{NS}.comet_cress"] = "Comet Cress"
    mature = {"type": "minecraft:match_block", "blocks": rid("comet_cress"), "state": {"age": "3"}}
    loot("comet_cress", [
        {"entries": [{"type": "minecraft:item", "name": rid("comet_cress")}], "rolls": 1},
        {"condition": mature, "entries": [{
            "type": "minecraft:item",
            "modifier": [
                {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
                {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                 "formula": "minecraft:binomial_with_bonus_count",
                 "parameters": {"extra": 1, "probability": 0.5714286}},
            ],
            "name": rid("comet_cress"),
        }], "rolls": 1},
    ], extra={"modifier": {"type": "minecraft:explosion_decay"}})

    # Items with their own textures
    for name, english in ITEMS:
        item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
        item_def(name, rid(f"item/{name}"))
        lang[f"item.{NS}.{name}"] = english

    # ---------------------------------------------------------------- loot for the rest
    for name in ("chiseled_umbral_stone", "void_crystal_block", "meteoric_iron_block",
                 "raw_meteoric_iron_block", "stardust_block", "nebula_moss", "lumineshroom", "starbloom",
                 "pale_end_stone_tiles", "solarite_block", "raw_solarite_block", "star_inlay_observatory_quartz",
                 "ashen_lumineshroom", "petrified_astral_vine"):
        drop_self(name)
    # Like blue ice and vanilla grass: nothing without Silk Touch / shears.
    for name, tool in (("comet_ice", "minecraft:tool/can_silk_touch"), ("gravity_anomaly", "minecraft:tool/can_silk_touch"),
                       ("shimmer_grass", "minecraft:tool/can_shear")):
        loot(name, [{"condition": tool, "entries": [{"type": "minecraft:item", "name": rid(name)}], "rolls": 1}])
    for ore, drop in (("void_crystal_ore", "void_crystal"), ("meteoric_iron_ore", "raw_meteoric_iron"),
                      ("solarite_ore", "raw_solarite")):
        loot(ore, [{
            "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": rid(ore)},
                {"type": "minecraft:item", "modifier": [
                    {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"type": "minecraft:explosion_decay"},
                ], "name": rid(drop)},
            ]}],
            "rolls": 1,
        }])
    loot("astral_glass", [{
        "condition": "minecraft:tool/can_silk_touch",
        "entries": [{"type": "minecraft:item", "name": rid("astral_glass")}],
        "rolls": 1,
    }])

    # ---------------------------------------------------------------- tags
    for name in ("chiseled_umbral_stone", "void_crystal_ore", "void_crystal_block", "meteoric_iron_ore",
                 "meteoric_iron_block", "raw_meteoric_iron_block"):
        tag("block", "minecraft:mineable/pickaxe", rid(name))
    for name in ("void_crystal_ore", "void_crystal_block", "meteoric_iron_block", "raw_meteoric_iron_block"):
        tag("block", "minecraft:needs_iron_tool", rid(name))
    tag("block", "minecraft:needs_diamond_tool", rid("meteoric_iron_ore"))
    for name in ("pale_end_stone_tiles", "comet_ice", "solarite_ore", "solarite_block", "raw_solarite_block",
                 "star_inlay_observatory_quartz", "gravity_anomaly"):
        tag("block", "minecraft:mineable/pickaxe", rid(name))
    for name in ("solarite_ore", "gravity_anomaly"):
        tag("block", "minecraft:needs_diamond_tool", rid(name))
    for name in ("solarite_block", "raw_solarite_block"):
        tag("block", "minecraft:needs_iron_tool", rid(name))
    tag("block", "minecraft:mineable/hoe", rid("sunscale_lichen"))
    tag("block", "minecraft:replaceable_by_trees", rid("shimmer_grass"))
    tag("block", "minecraft:beacon_base_blocks", rid("solarite_block"))
    tag("block", "minecraft:beacon_base_blocks", rid("meteoric_iron_block"))
    # What the End's plants grow on.
    for name in ("minecraft:end_stone", rid("nebula_moss"), rid("umbral_stone"), rid("pale_end_stone"),
                 rid("charred_end_stone"), rid("sunscorched_stone")):
        tag("block", f"{NS}:astral_soil", name)
    tag("block", "minecraft:mineable/shovel", rid("stardust_block"))
    tag("block", "minecraft:mineable/hoe", rid("nebula_moss"))
    tag("block", "minecraft:small_flowers", rid("starbloom"))
    tag("item", "minecraft:small_flowers", rid("starbloom"))
    tag("block", "minecraft:crops", rid("comet_cress"))

    # ---------------------------------------------------------------- recipes
    shaped("polished_umbral_stone", "polished_umbral_stone", ["SS", "SS"], {"S": rid("umbral_stone")}, count=4)
    shaped("umbral_stone_bricks", "umbral_stone_bricks", ["##", "##"], {"#": rid("polished_umbral_stone")}, count=4)
    shaped("chiseled_umbral_stone", "chiseled_umbral_stone", ["#", "#"], {"#": rid("umbral_stone_slab")})
    for base, (stairs, slab, wall) in family_variants.items():
        stonecutting(base, stairs)
        stonecutting(base, slab, 2)
        stonecutting(base, wall)
    for result in ("polished_umbral_stone", "umbral_stone_bricks", "chiseled_umbral_stone"):
        stonecutting("umbral_stone", result)
    for result, count in (("polished_umbral_stone_stairs", 1), ("polished_umbral_stone_slab", 2),
                          ("polished_umbral_stone_wall", 1), ("umbral_stone_brick_stairs", 1),
                          ("umbral_stone_brick_slab", 2), ("umbral_stone_brick_wall", 1)):
        stonecutting("umbral_stone", result, count)
    stonecutting("polished_umbral_stone", "umbral_stone_bricks")
    for result, count in (("umbral_stone_brick_stairs", 1), ("umbral_stone_brick_slab", 2), ("umbral_stone_brick_wall", 1)):
        stonecutting("polished_umbral_stone", result, count)

    # Phase 3 building blocks
    shaped("pale_end_stone_bricks", "pale_end_stone_bricks", ["##", "##"], {"#": rid("pale_end_stone")}, count=4)
    shaped("pale_end_stone_tiles", "pale_end_stone_tiles", ["##", "##"], {"#": rid("pale_end_stone_bricks")}, count=4)
    shaped("polished_sunscorched_stone", "polished_sunscorched_stone", ["##", "##"], {"#": rid("sunscorched_stone")}, count=4)
    shaped("sunscorched_stone_bricks", "sunscorched_stone_bricks", ["##", "##"], {"#": rid("polished_sunscorched_stone")}, count=4)
    shaped("observatory_quartz_pillar", "observatory_quartz_pillar", ["#", "#"], {"#": rid("observatory_quartz")}, count=2)
    shaped("star_inlay_observatory_quartz", "star_inlay_observatory_quartz", ["#", "#"], {"#": rid("observatory_quartz_slab")})
    for src, results in (
        ("pale_end_stone", ["pale_end_stone_bricks", "pale_end_stone_brick_stairs", "pale_end_stone_brick_slab",
                            "pale_end_stone_brick_wall", "pale_end_stone_tiles"]),
        ("pale_end_stone_bricks", ["pale_end_stone_tiles"]),
        ("sunscorched_stone", ["polished_sunscorched_stone", "polished_sunscorched_stone_stairs", "polished_sunscorched_stone_slab",
                               "polished_sunscorched_stone_wall", "sunscorched_stone_bricks", "sunscorched_stone_brick_stairs",
                               "sunscorched_stone_brick_slab", "sunscorched_stone_brick_wall"]),
        ("polished_sunscorched_stone", ["sunscorched_stone_bricks", "sunscorched_stone_brick_stairs",
                                        "sunscorched_stone_brick_slab", "sunscorched_stone_brick_wall"]),
        ("observatory_quartz", ["observatory_quartz_pillar", "star_inlay_observatory_quartz"]),
    ):
        for result in results:
            stonecutting(src, result, 2 if result.endswith("_slab") else 1)
    # End Stone scorched by a comet or a flare can be made on purpose, too.
    cooking("charred_end_stone", "smelting", "minecraft:end_stone", "charred_end_stone", 0.1, 200)

    shaped("solarite_block", "solarite_block", ["###", "###", "###"], {"#": rid("solarite_ingot")})
    shapeless("solarite_ingot_from_solarite_block", "solarite_ingot", [rid("solarite_block")], count=9)
    shaped("raw_solarite_block", "raw_solarite_block", ["###", "###", "###"], {"#": rid("raw_solarite")})
    shapeless("raw_solarite", "raw_solarite", [rid("raw_solarite_block")], count=9)
    for kind, time in (("smelting", 200), ("blasting", 100)):
        cooking(f"solarite_ingot_from_{kind}_raw_solarite", kind, rid("raw_solarite"), "solarite_ingot", 1.0, time)
        cooking(f"solarite_ingot_from_{kind}_solarite_ore", kind, rid("solarite_ore"), "solarite_ingot", 1.0, time)
    recipe("orange_dye_from_flarebloom", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                          "group": "orange_dye", "ingredients": [rid("flarebloom")],
                                          "result": {"count": 2, "id": "minecraft:orange_dye"}})

    shaped("void_crystal_block", "void_crystal_block", ["###", "###", "###"], {"#": rid("void_crystal")})
    shapeless("void_crystal", "void_crystal", [rid("void_crystal_block")], count=9)
    shaped("meteoric_iron_block", "meteoric_iron_block", ["###", "###", "###"], {"#": rid("meteoric_iron_ingot")})
    shapeless("meteoric_iron_ingot_from_meteoric_iron_block", "meteoric_iron_ingot", [rid("meteoric_iron_block")], count=9)
    shaped("meteoric_iron_ingot_from_nuggets", "meteoric_iron_ingot", ["###", "###", "###"], {"#": rid("meteoric_iron_nugget")}, category="misc")
    shapeless("meteoric_iron_nugget", "meteoric_iron_nugget", [rid("meteoric_iron_ingot")], count=9)
    shaped("raw_meteoric_iron_block", "raw_meteoric_iron_block", ["###", "###", "###"], {"#": rid("raw_meteoric_iron")})
    shapeless("raw_meteoric_iron", "raw_meteoric_iron", [rid("raw_meteoric_iron_block")], count=9)
    for kind, time in (("smelting", 200), ("blasting", 100)):
        cooking(f"meteoric_iron_ingot_from_{kind}_raw_meteoric_iron", kind, rid("raw_meteoric_iron"), "meteoric_iron_ingot", 1.0, time)
        cooking(f"meteoric_iron_ingot_from_{kind}_meteoric_iron_ore", kind, rid("meteoric_iron_ore"), "meteoric_iron_ingot", 1.0, time)
        cooking(f"void_crystal_from_{kind}_void_crystal_ore", kind, rid("void_crystal_ore"), "void_crystal", 1.0, time)
    cooking("astral_glass", "smelting", rid("stardust_block"), "astral_glass", 0.1, 200)
    recipe("yellow_dye_from_starbloom", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                         "group": "yellow_dye", "ingredients": [rid("starbloom")],
                                         "result": {"id": "minecraft:yellow_dye"}})

    worldgen()
    worldgen_phase3()

    # ---------------------------------------------------------------- write tags + lang
    for (registry, name), values in tags.items():
        ns, path = name.split(":")
        folder = RES / "data" / ns / "tags" / registry
        write(folder / f"{path}.json", {"replace": False, "values": sorted(set(values))})
    write(ASSETS / "lang" / "en_us.json", dict(sorted(lang.items())))
    print(f"Wrote {len(written)} files")


if __name__ == "__main__":
    main()
