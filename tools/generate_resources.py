#!/usr/bin/env python3
"""Writes every Astral End blockstate, model, item definition, loot table, recipe, tag
and English name from the lists below, in the same JSON format vanilla 26.3 uses.

Run from the project root:  python3 tools/generate_resources.py
Then check the result with:   python3 tools/check_resources.py

Adding a block: register it in ModBlocks.java, add it to the right list here, draw its
texture in tools/textures/make_textures.py, then run all three scripts.
"""
import json
import re
import shutil
import sys
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
    ("nightstone", "Nightstone", True),
    ("polished_nightstone", "Polished Nightstone", True),
    ("nightstone_bricks", "Nightstone Bricks", True),
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
    ("chiseled_nightstone", "Chiseled Nightstone"),
    ("void_crystal_ore", "Void Crystal Ore"),
    ("void_crystal_block", "Block of Void Crystal"),
    ("meteoric_iron_ore", "Meteoric Iron Ore"),
    ("meteoric_iron_block", "Block of Meteoric Iron"),
    ("raw_meteoric_iron_block", "Block of Raw Meteoric Iron"),
    ("stardust_block", "Stardust"),
    ("astral_glass", "Astral Glass"),
    ("chorus_moss", "Chorus Moss"),
    # Phase 3
    ("pale_end_stone_tiles", "Pale End Stone Tiles"),
    ("solarite_ore", "Solarite Ore"),
    ("solarite_block", "Block of Solarite"),
    ("raw_solarite_block", "Block of Raw Solarite"),
    ("star_inlay_observatory_quartz", "Star Inlay Observatory Quartz"),
    ("upwell_core", "Upwell Core"),
    ("lumine_cap", "Lumine Cap"),
    # Phase 4
    ("spore_lantern", "Spore Lantern"),
    ("whalelight_scale", "Whalelight Scale"),
]
PILLARS = [
    ("leviathan_bone_block", "Leviathan Bone Block", "leviathan_bone"),
    ("observatory_quartz_pillar", "Observatory Quartz Pillar", "observatory_quartz_pillar"),
    ("pollinator_husk", "Pollinator Husk", "pollinator_husk"),
    ("petrified_stalk", "Petrified Stalk", "petrified_stalk"),
]
CROSS_PLANTS = [
    ("lumineshroom", "Lumineshroom"), ("starbloom", "Starbloom"),
    ("shimmer_grass", "Shimmer Grass"), ("ashen_lumineshroom", "Ashen Lumineshroom"),
    ("petrified_astral_vine", "Petrified Astral Vine"),
    ("nebula_tufts", "Nebula Tufts"), ("nullwood_sapling", "Nullwood Sapling"), ("chorus_sprout", "Chorus Sprout"),
]
ITEMS = [
    ("void_crystal", "Void Crystal"),
    ("raw_meteoric_iron", "Raw Meteoric Iron"),
    ("meteoric_iron_ingot", "Meteoric Iron Ingot"),
    ("meteoric_iron_nugget", "Meteoric Iron Nugget"),
    ("comet_cress", "Comet Cress"),
    ("raw_solarite", "Raw Solarite"),
    ("solarite_ingot", "Solarite Ingot"),
    # Phase 4: what the mobs leave behind
    ("glowing_spores", "Glowing Spores"),
    ("chart_fragment", "Chart Fragment"),
    ("cracked_lens", "Cracked Lens"),
    ("abyss_shard", "Abyss Shard"),
    ("umbral_membrane", "Umbral Membrane"),
    ("ray_film", "Ray Film"),
    ("cinder", "Cinder"),
    ("star_chart", "Star Chart"),
    ("upwell_mote", "Upwell Mote"),
    ("resonant_key", "Resonant Key"),
]

PLURAL = {"nightstone_bricks": "nightstone_brick", "pale_end_stone_bricks": "pale_end_stone_brick",
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


def stone_family(base, english, texture=None, with_wall=True, stem=None, tool="pickaxe"):
    """Stairs, slab and (usually) wall for a base block. stem: name prefix for them, if not the base's."""
    tex = rid(f"block/{texture or base}")
    if stem:
        stairs, slab, wall = f"{stem}_stairs", f"{stem}_slab", f"{stem}_wall"
    else:
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

    kinds = (("stairs", stairs), ("slab", slab)) + ((("wall", wall),) if with_wall else ())
    if not with_wall:
        wall = None
    parts = [{"apply": {"model": rid(f"block/{wall}_post")}, "when": {"up": "true"}}]
    for height, model in (("low", f"{wall}_side"), ("tall", f"{wall}_side_tall")) if wall else ():
        for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            apply = {"model": rid(f"block/{model}"), "uvlock": True}
            if y:
                apply["y"] = y
            parts.append({"apply": apply, "when": {side: height}})
    if wall:
        blockstate(wall, {"multipart": parts})
        for suffix, parent in (("_post", "template_wall_post"), ("_side", "template_wall_side"),
                               ("_side_tall", "template_wall_side_tall"), ("_inventory", "wall_inventory")):
            block_model(wall + suffix, {"parent": f"minecraft:block/{parent}", "textures": {"wall": tex}})
        item_def(wall, rid(f"block/{wall}_inventory"))

    for kind, bid in kinds:
        lang[f"block.{NS}.{bid}"] = english_variant(english, kind)
        tag("block", f"minecraft:mineable/{tool}", rid(bid))
        tag_name = {"stairs": "minecraft:stairs", "slab": "minecraft:slabs", "wall": "minecraft:walls"}[kind]
        tag("block", tag_name, rid(bid))
        tag("item", tag_name, rid(bid))
    drop_self(stairs)
    if wall:
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
    if wall:
        shaped(wall, wall, ["###", "###"], {"#": rid(base)}, count=6, category="misc")
    return stairs, slab, wall


# ------------------------------------------------------------------ wood sets (Phase 3.5)
#
# (name, log word, wood word, English name, English log, English wood). Mushroom wood uses "stem" and
# "hyphae", like vanilla's crimson and warped wood. Must match the woodSet(...) calls in ModBlocks.java.
WOOD_SETS = [
    ("lumine", "stem", "hyphae", "Lumine", "Lumine Stem", "Lumine Hyphae"),
    ("nullwood", "log", "wood", "Nullwood", "Nullwood Log", "Nullwood"),
    ("chorus", "stalk", "wood", "Chorus", "Chorus Stalk", "Chorus Wood"),
]

FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def wood_set(n, log_word, wood_word, english, english_log, english_wood):
    log, wood = f"{n}_{log_word}", f"{n}_{wood_word}"
    slog, swood = f"stripped_{log}", f"stripped_{wood}"
    planks = f"{n}_planks"
    tex = lambda t: rid(f"block/{t}")
    # nullwood's "wood" block is just "nullwood", so its textures are named after the log
    for name, side, end in ((log, log, f"{log}_top"), (wood, log, log), (slog, slog, f"{slog}_top"), (swood, slog, slog)):
        blockstate(name, {"variants": {
            "axis=x": {"model": rid(f"block/{name}"), "x": 90, "y": 90},
            "axis=y": {"model": rid(f"block/{name}")},
            "axis=z": {"model": rid(f"block/{name}"), "x": 90},
        }})
        block_model(name, {"parent": "minecraft:block/cube_column", "textures": {"end": tex(end), "side": tex(side)}})
        item_def(name, rid(f"block/{name}"))
        drop_self(name)
    lang[f"block.{NS}.{log}"] = english_log
    lang[f"block.{NS}.{wood}"] = english_wood
    lang[f"block.{NS}.{slog}"] = f"Stripped {english_log}"
    lang[f"block.{NS}.{swood}"] = f"Stripped {english_wood}"
    cube(planks)
    lang[f"block.{NS}.{planks}"] = f"{english} Planks"
    drop_self(planks)
    stairs, slab, _ = stone_family(planks, f"{english} Planks", with_wall=False, stem=n, tool="axe")
    lang[f"block.{NS}.{stairs}"] = f"{english} Stairs"
    lang[f"block.{NS}.{slab}"] = f"{english} Slab"
    ptex = tex(planks)

    fence, gate = f"{n}_fence", f"{n}_fence_gate"
    parts = [{"apply": {"model": rid(f"block/{fence}_post")}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": rid(f"block/{fence}_side"), "uvlock": True}
        if y:
            apply["y"] = y
        parts.append({"apply": apply, "when": {side: "true"}})
    blockstate(fence, {"multipart": parts})
    for suffix, parent in (("_post", "fence_post"), ("_side", "fence_side"), ("_inventory", "fence_inventory")):
        block_model(fence + suffix, {"parent": f"minecraft:block/{parent}", "textures": {"texture": ptex}})
    item_def(fence, rid(f"block/{fence}_inventory"))

    gate_y = {"south": 0, "west": 90, "north": 180, "east": 270}
    variants = {}
    for facing, y in gate_y.items():
        for in_wall in ("false", "true"):
            for open_ in ("false", "true"):
                model = gate + ("_wall" if in_wall == "true" else "") + ("_open" if open_ == "true" else "")
                v = {"model": rid(f"block/{model}"), "uvlock": True}
                if y:
                    v["y"] = y
                variants[f"facing={facing},in_wall={in_wall},open={open_}"] = v
    blockstate(gate, {"variants": variants})
    for suffix, parent in (("", "template_fence_gate"), ("_open", "template_fence_gate_open"),
                           ("_wall", "template_fence_gate_wall"), ("_wall_open", "template_fence_gate_wall_open")):
        block_model(gate + suffix, {"parent": f"minecraft:block/{parent}", "textures": {"texture": ptex}})
    item_def(gate, rid(f"block/{gate}"))

    door = f"{n}_door"
    variants = {}
    base_y = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y0 in base_y.items():
        for half, part in (("lower", "bottom"), ("upper", "top")):
            for hinge in ("left", "right"):
                for open_ in ("false", "true"):
                    model = f"{door}_{part}_{hinge}" + ("_open" if open_ == "true" else "")
                    y = y0 if open_ == "false" else (y0 + (90 if hinge == "left" else 270)) % 360
                    v = {"model": rid(f"block/{model}")}
                    if y:
                        v["y"] = y
                    variants[f"facing={facing},half={half},hinge={hinge},open={open_}"] = v
    blockstate(door, {"variants": variants})
    for part in ("bottom", "top"):
        for hinge in ("left", "right"):
            for op in ("", "_open"):
                block_model(f"{door}_{part}_{hinge}{op}", {"parent": f"minecraft:block/door_{part}_{hinge}{op}",
                                                          "textures": {"bottom": tex(f"{door}_bottom"), "top": tex(f"{door}_top")}})
    item_model(door, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{door}")}})
    item_def(door, rid(f"item/{door}"))
    loot(door, [{"condition": {"type": "minecraft:survives_explosion"},
                 "entries": [{"type": "minecraft:item", "name": rid(door), "condition": {
                     "type": "minecraft:match_block", "blocks": rid(door), "state": {"half": "lower"}}}],
                 "rolls": 1}])

    trap = f"{n}_trapdoor"
    variants = {}
    for facing, y in FACING_Y.items():
        for half in ("bottom", "top"):
            for open_ in ("false", "true"):
                if open_ == "false":
                    v = {"model": rid(f"block/{trap}_{half}")}
                    if y:
                        v["y"] = y
                elif half == "bottom":
                    v = {"model": rid(f"block/{trap}_open")}
                    if y:
                        v["y"] = y
                else:
                    v = {"model": rid(f"block/{trap}_open"), "x": 180}
                    if (y + 180) % 360:
                        v["y"] = (y + 180) % 360
                variants[f"facing={facing},half={half},open={open_}"] = v
    blockstate(trap, {"variants": variants})
    for suffix in ("bottom", "top", "open"):
        block_model(f"{trap}_{suffix}", {"parent": f"minecraft:block/template_orientable_trapdoor_{suffix}",
                                         "textures": {"texture": tex(trap)}})
    item_def(trap, rid(f"block/{trap}_bottom"))

    button = f"{n}_button"
    variants = {}
    ceiling_y = {"south": 0, "west": 90, "north": 180, "east": 270}
    for face in ("floor", "wall", "ceiling"):
        for facing, y in FACING_Y.items():
            for powered in ("false", "true"):
                v = {"model": rid(f"block/{button}" + ("_pressed" if powered == "true" else ""))}
                if face == "wall":
                    v["x"] = 90
                    v["uvlock"] = True
                elif face == "ceiling":
                    v["x"] = 180
                    y = ceiling_y[facing]
                if y:
                    v["y"] = y
                variants[f"face={face},facing={facing},powered={powered}"] = v
    blockstate(button, {"variants": variants})
    for suffix, parent in (("", "button"), ("_pressed", "button_pressed"), ("_inventory", "button_inventory")):
        block_model(button + suffix, {"parent": f"minecraft:block/{parent}", "textures": {"texture": ptex}})
    item_def(button, rid(f"block/{button}_inventory"))

    plate = f"{n}_pressure_plate"
    blockstate(plate, {"variants": {"powered=false": {"model": rid(f"block/{plate}")},
                                    "powered=true": {"model": rid(f"block/{plate}_down")}}})
    block_model(plate, {"parent": "minecraft:block/pressure_plate_up", "textures": {"texture": ptex}})
    block_model(plate + "_down", {"parent": "minecraft:block/pressure_plate_down", "textures": {"texture": ptex}})
    item_def(plate, rid(f"block/{plate}"))

    for name, english_name in ((fence, f"{english} Fence"), (gate, f"{english} Fence Gate"), (door, f"{english} Door"),
                               (trap, f"{english} Trapdoor"), (button, f"{english} Button"),
                               (plate, f"{english} Pressure Plate")):
        lang[f"block.{NS}.{name}"] = english_name
        if name != door:
            drop_self(name)

    # Tags: axes mine it; it counts as logs, planks, and each kind of wooden block for vanilla's
    # recipes and mechanics (sticks, crafting tables, villagers opening doors...). End wood doesn't burn.
    logs_tag = f"{NS}:{n}_{log_word}s"
    for t in ("block", "item"):
        for name in (log, wood, slog, swood):
            tag(t, logs_tag, rid(name))
        tag(t, "minecraft:logs", f"#{logs_tag}")
        tag(t, "minecraft:planks", rid(planks))
        tag(t, "minecraft:wooden_stairs", rid(stairs))
        tag(t, "minecraft:wooden_slabs", rid(slab))
        tag(t, "minecraft:wooden_fences", rid(fence))
        tag(t, "minecraft:fence_gates", rid(gate))
        tag(t, "minecraft:wooden_doors", rid(door))
        tag(t, "minecraft:wooden_trapdoors", rid(trap))
        tag(t, "minecraft:wooden_buttons", rid(button))
        tag(t, "minecraft:wooden_pressure_plates", rid(plate))
    for name in (log, wood, slog, swood, planks, stairs, slab, fence, gate, door, trap, button, plate):
        tag("block", "minecraft:mineable/axe", rid(name))
        tag("item", "minecraft:non_flammable_wood", rid(name))

    # Recipes, exactly like vanilla's wood.
    recipe(planks, {"type": "minecraft:crafting_shapeless", "category": "building", "group": "planks",
                    "ingredients": [f"#{logs_tag}"], "result": {"count": 4, "id": rid(planks)}})
    for result, src in ((wood, log), (swood, slog)):
        recipe(result, {"type": "minecraft:crafting_shaped", "category": "building", "group": "bark",
                        "key": {"#": rid(src)}, "pattern": ["##", "##"], "result": {"count": 3, "id": rid(result)}})
    recipe(door, {"type": "minecraft:crafting_shaped", "category": "redstone", "group": "wooden_door", "key": {"#": rid(planks)},
                  "pattern": ["##", "##", "##"], "result": {"count": 3, "id": rid(door)}})
    recipe(trap, {"type": "minecraft:crafting_shaped", "category": "redstone", "group": "wooden_trapdoor", "key": {"#": rid(planks)},
                  "pattern": ["###", "###"], "result": {"count": 2, "id": rid(trap)}})
    recipe(fence, {"type": "minecraft:crafting_shaped", "category": "misc", "group": "wooden_fence",
                   "key": {"#": "minecraft:stick", "W": rid(planks)}, "pattern": ["W#W", "W#W"],
                   "result": {"count": 3, "id": rid(fence)}})
    recipe(gate, {"type": "minecraft:crafting_shaped", "category": "redstone", "group": "wooden_fence_gate",
                  "key": {"#": "minecraft:stick", "W": rid(planks)}, "pattern": ["#W#", "#W#"], "result": {"id": rid(gate)}})
    recipe(button, {"type": "minecraft:crafting_shapeless", "category": "redstone", "group": "wooden_button",
                    "ingredients": [rid(planks)], "result": {"id": rid(button)}})
    recipe(plate, {"type": "minecraft:crafting_shaped", "category": "redstone", "group": "wooden_pressure_plate",
                   "key": {"#": rid(planks)}, "pattern": ["##"], "result": {"id": rid(plate)}})


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
#    in Nebula Fields with Chorus Moss.
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


def ground_cover(name, block, count, on=None):
    """Plants scattered evenly over the whole chunk (not in clumps), like grass on a plains."""
    feature(block, {"type": "minecraft:simple_block", "to_place": {"id": rid(block)}})
    placed(name, {"feature": rid(block), "placement": [
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
            {"type": "minecraft:would_survive", "state": rid(block)},
        ] + ([{"type": "minecraft:matching_blocks", "blocks": on, "offset": [0, -1, 0]}] if on else [])}},
    ]})


def big_feature(name, feature_name, count=1, rarity=1, heightmap="MOTION_BLOCKING", groves=None):
    """A large Java feature (tree, spire...) placed on the surface: `count` per chunk, in 1 of `rarity` chunks.
    groves=(noise_level, sparse, dense): instead of a fixed count, vanilla's biome noise (patches a hundred
    or two blocks across) decides - `dense` per chunk where it's above noise_level, `sparse` elsewhere."""
    feature(feature_name, {"type": rid(feature_name)})
    placement = []
    if rarity > 1:
        placement.append({"type": "minecraft:rarity_filter", "chance": rarity})
    if groves:
        level, sparse, dense = groves
        placement.append({"type": "minecraft:noise_threshold_count", "noise_level": level, "below_noise": sparse,
                          "above_noise": dense})
    elif count > 1:
        placement.append({"type": "minecraft:count", "count": count})
    placement += [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": heightmap},
                  {"type": "minecraft:biome"}]
    placed(name, {"feature": rid(feature_name), "placement": placement})


# ---- Terrain
#
# Region noises (also read by the biome source through the noise router; the thresholds here
# must match AstralRegions.java):
REGION_NEBULA = 0.18      # nebula > this: Nebula Fields
REGION_COLD = -0.2        # nebula < this: the "cold" band (Ashen Barrens / Comet Wake)
REGION_COLD_BELT = 0.2    # |belt| beyond this in the cold band
REGION_MID_BELT = 0.3     # |belt| beyond this in the middle band (Hollow Isles / Corona)
REGION_THICKET = 0.22     # belt < -this in the nebula band: the Chorus Thicket
REGION_CRAGS = 0.45       # ruin > this, in what would be vanilla: the Glimmering Crags
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
    # Region noises: large and slow, so each biome is a territory a few thousand blocks across.
    df("region/nebula", {"type": "minecraft:noise", "noise": "minecraft:vegetation", "xz_scale": 0.08, "y_scale": 0.0})
    df("region/belt", {"type": "minecraft:noise", "noise": "minecraft:temperature", "xz_scale": 0.3, "y_scale": 0.0})
    df("region/ruin", {"type": "minecraft:noise", "noise": "minecraft:ridge", "xz_scale": 0.05, "y_scale": 0.0})

    # Weights (0..1) of each biome's terrain. They are 0 within 1,024 blocks of the centre, so the
    # main island and the gap around it are exactly vanilla.
    outer = clamp(mul(add({"type": "minecraft:distance_to_point", "metric": "euclidean", "point": [0, 0, 0]},
                          -1024.0), 1.0 / 64), 0.0, 1.0)
    df("terrain/outer", outer)
    not_nebula = sub(1.0, ramp(nebula, REGION_NEBULA))
    cold = mul(ramp(mul(nebula, -1.0), -REGION_COLD), not_nebula)
    middle = mul(ramp(nebula, REGION_COLD), not_nebula)
    raw = {
        "ashen_barrens": mul(cold, ramp(belt, REGION_COLD_BELT)),
        "comet_wake": mul(cold, ramp(mul(belt, -1.0), REGION_COLD_BELT)),
        "hollow_isles": mul(middle, ramp(belt, REGION_MID_BELT)),
        "corona": mul(middle, ramp(mul(belt, -1.0), REGION_MID_BELT)),
    }
    vanilla_share = sub(sub(sub(sub(not_nebula, raw["ashen_barrens"]), raw["comet_wake"]), raw["hollow_isles"]), raw["corona"])
    raw["glimmering_crags"] = mul(vanilla_share, ramp(ruin, REGION_CRAGS))
    thicket = ramp(mul(belt, -1.0), REGION_THICKET)
    raw["nebula_fields"] = mul(ramp(nebula, REGION_NEBULA), sub(1.0, thicket))
    raw["chorus_thicket"] = mul(ramp(nebula, REGION_NEBULA), thicket)
    for biome, weight in raw.items():
        df(f"terrain/weight/{biome}", mul(rid("terrain/outer"), weight))

    islands, noise, vanilla = "minecraft:end/islands", "minecraft:end/base_3d_noise", "minecraft:end/sloped_cheese"
    y = y_line(0, 0.0, 128, 128.0)
    # Big landmasses: where a slow noise ("mask", features ~500 blocks) is high, an island with a
    # surface at `top` and a tapering underside whose depth grows toward the island's middle.
    df("terrain/mask", {"type": "minecraft:noise", "noise": "minecraft:erosion", "xz_scale": 1.0, "y_scale": 0.0})
    df("terrain/hills", {"type": "minecraft:noise", "noise": "minecraft:surface", "xz_scale": 1.0, "y_scale": 0.0})
    mask, hills = rid("terrain/mask"), rid("terrain/hills")

    def landmass(top, thickness):
        return mul(dmin(sub(top, y), sub(y, sub(top, thickness))), 0.4)

    def depth_from(m, scale, most):
        return clamp(mul(m, scale), 0.0, most)

    # Nebula Fields: rolling hills of moss on wide islands.
    df("terrain/nebula_fields", dmax(vanilla, landmass(add(60.0, mul(hills, 7.0)), depth_from(add(mask, -0.02), 120.0, 40.0))))

    # Ashen Barrens: wide, thin, flat-topped shelves (level at y=54) that thin to a see-through edge.
    shelf_islands = mul(add(add(islands, 0.12), mul(noise, 0.2)), 14.0)
    shelves = dmin(y_line(0, 54.5 * 0.25, 128, (54.5 - 128) * 0.25),
                   add(y_line(0, -54.5 * 0.25, 128, (128 - 54.5) * 0.25), mul(shelf_islands, 0.25)))
    df("terrain/ashen_barrens", dmax(shelves, landmass(54.5, depth_from(add(mask, 0.05), 40.0, 10.0))))

    # Comet Wake: broad scorched islands, plus a river of small floating debris between y=30 and 70.
    band = dmin(y_line(30, 0.0, 45, 1.0), y_line(55, 1.0, 70, 0.0))
    debris = sub(add(mul({"type": "minecraft:noise", "noise": "minecraft:ridge", "xz_scale": 4.0, "y_scale": 4.0}, 3.0), -1.7),
                 mul(sub(1.0, band), 5.0))
    df("terrain/comet_wake", dmax(dmax(vanilla, debris), landmass(add(58.0, mul(hills, 3.0)), depth_from(add(mask, -0.02), 100.0, 36.0))))

    # Hollow Isles: ring archipelagos. Land follows a band along the mask's contour, which traces
    # broken rings and arcs around empty centres; vanilla islands are hollowed out the same way.
    ring = clamp(mul(sub(0.1, {"type": "minecraft:abs", "input": add(mask, -0.18)}), 200.0), 0.0, 30.0)
    hollowed = sub(add(vanilla, mul(noise, 0.5)), mul(dmax(add(islands, -0.2), 0.0), 6.0))
    df("terrain/hollow_isles", dmax(hollowed, landmass(add(58.0, mul(hills, 4.0)), ring)))

    # The Corona: flat-topped mesas with sheer cliffs, so there's little natural shelter.
    df("terrain/corona", dmax(vanilla, landmass(add(62.0, mul(hills, 1.5)), depth_from(add(mask, -0.02), 400.0, 30.0))))

    # Chorus Thicket: like the Nebula Fields, but gentler: low, broad mounds for the chorus to stand on.
    df("terrain/chorus_thicket", dmax(vanilla, landmass(add(60.0, mul(hills, 4.0)), depth_from(add(mask, -0.02), 110.0, 38.0))))

    # Glimmering Crags: islands bristling with jagged peaks. A fast noise lifts the top surface into
    # spires up to ~50 blocks high (and deepens the island under them, so they stand on something).
    df("terrain/crag_peaks", {"type": "minecraft:noise", "noise": "minecraft:ridge", "xz_scale": 6.0, "y_scale": 0.0})
    peaks = clamp(mul(add(rid("terrain/crag_peaks"), -0.15), 90.0), 0.0, 48.0)
    df("terrain/glimmering_crags", dmax(vanilla, landmass(add(add(56.0, mul(hills, 4.0)), peaks),
                                                          add(depth_from(add(mask, 0.04), 110.0, 40.0), mul(peaks, 0.6)))))

    shaped = vanilla
    for biome in ("nebula_fields", "chorus_thicket", "corona", "hollow_isles", "comet_wake", "ashen_barrens", "glimmering_crags"):
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

    # Surface: the top block of every island floor in Nebula Fields becomes Chorus Moss;
    # everything else is End Stone, exactly like vanilla.
    def in_biome(biome, rule):
        return {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [rid(biome)]},
                "then_run": rule}

    def block(name):
        return {"type": "minecraft:block", "result_state": rid(name)}

    write(WG / "material_rule" / "end.json", {"type": "minecraft:sequence", "sequence": [
        in_biome("nebula_fields", {"type": "minecraft:condition", "if_true": "minecraft:on_floor",
                                   "then_run": block("chorus_moss")}),
        in_biome("chorus_thicket", {"type": "minecraft:condition", "if_true": "minecraft:on_floor",
                                    "then_run": block("chorus_moss")}),
        in_biome("glimmering_crags", block("nightstone")),
        # Whole islands of a different stone:
        in_biome("ashen_barrens", block("pale_end_stone")),
        in_biome("comet_wake", block("charred_end_stone")),
        in_biome("hollow_isles", block("nightstone")),
        # The Corona: a bleached crust of Sunscorched Stone over Charred End Stone.
        in_biome("corona", {"type": "minecraft:sequence", "sequence": [
            {"type": "minecraft:condition", "if_true": "minecraft:under_floor", "then_run": block("sunscorched_stone")},
            block("charred_end_stone")]}),
        "minecraft:end",
    ]})
    terrain()

    # Plants of the Nebula Fields.
    plant_patch("nebula_lumineshrooms", "lumineshroom", rarity=1, count=40, spread=6)
    plant_patch("nebula_starblooms", "starbloom", rarity=1, count=32, spread=6)
    # A forest of giant Lumineshrooms over a carpet of Nebula Tufts, with tendrils hanging below.
    big_feature("nebula_giant_lumineshrooms", "giant_lumineshroom", count=1)
    # Oases: ponds sunk into the moss, reeds and lumineshrooms crowding their banks.
    big_feature("nebula_oases", "oasis", rarity=4, heightmap="WORLD_SURFACE_WG")
    ground_cover("nebula_tufts", "nebula_tufts", 110)
    feature("tendril_underside", {"type": rid("tendril_underside")})
    placed("nebula_tendrils", {"feature": rid("tendril_underside"), "placement": [
        {"type": "minecraft:count", "count": 14}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
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
            {"type": "minecraft:matching_blocks", "blocks": [rid("chorus_moss")], "offset": [0, -1, 0]},
        ]}},
    ]})

    # The biome itself. Endermen spawn as in the rest of the End; violet fog and drifting motes.
    write(WG / "biome" / "nebula_fields.json", {
        "attributes": {
            "minecraft:gameplay/natural_mob_spawns": mob_spawns("nebula_fields"),
            **background_music("nebula_fields"),
            "minecraft:visual/ambient_particles": {"argument": [
                {"particle": {"type": "minecraft:reverse_portal"}, "probability": 0.012}], "modifier": "append"},
            "minecraft:visual/fog_color": "#2b1d45",
        },
        "carvers": [],
        "downfall": 0.5,
        "effects": {"water_color": "#7a5fc4"},
        # Features by generation step. Step 4 (surface structures) has the return gateway, the
        # same as in the End Highlands; step 9 (vegetal decoration) has the plants.
        "features": [[], [rid("nebula_oases")], [], [], ["minecraft:end_gateway_return"], [], [], [], [],
                     [rid("nebula_giant_lumineshrooms"), rid("nebula_lumineshrooms"), rid("nebula_starblooms"),
                      rid("nebula_astral_vines"), rid("nebula_tufts"), rid("nebula_tendrils"), "minecraft:chorus_plant"]],
        "has_precipitation": False,
        "temperature": 0.5,
    })

    tag("worldgen/biome", "minecraft:is_end", rid("nebula_fields"))
    # Biomes in this tag get the End's low gravity. To turn it off, override this tag with an
    # empty list in a datapack.
    tag("worldgen/biome", f"{NS}:low_gravity", rid("nebula_fields"))
    # Phase 9: data-pack switches for the two events (empty a tag to turn its event off).
    tag("worldgen/biome", f"{NS}:sunstorms", rid("corona"))
    for comet_biome in ("nebula_fields", "ashen_barrens", "comet_wake", "hollow_isles", "corona", "chorus_thicket", "glimmering_crags",
                        "minecraft:end_highlands", "minecraft:end_midlands", "minecraft:end_barrens", "minecraft:small_end_islands"):
        tag("worldgen/biome", f"{NS}:live_comets", comet_biome if ":" in comet_biome else rid(comet_biome))
    # Biomes where Upwell Cores flip gravity. Empty this tag in a datapack to turn it off.
    tag("worldgen/biome", f"{NS}:upwells", rid("hollow_isles"))
    # Chorus grows in the living astral biomes too: on Chorus Moss and Nightstone as on End Stone.
    for support in ("supports_chorus_plant", "supports_chorus_flower"):
        for ground in ("chorus_moss", "nightstone"):
            tag("block", f"minecraft:{support}", rid(ground))
    # Lights an Umbral Stalker puts out while it hunts (lit ones go dark, the rest drop). A data
    # pack can add to it or empty it.
    for light in ("minecraft:torch", "minecraft:wall_torch", "minecraft:soul_torch", "minecraft:soul_wall_torch",
                  "minecraft:copper_torch", "minecraft:copper_wall_torch", "minecraft:lantern", "minecraft:soul_lantern",
                  "#minecraft:candles", "#minecraft:campfires", rid("null_lantern"), rid("spore_lantern")):
        tag("block", f"{NS}:snuffed_by_stalkers", light)
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


# Phase 4 mobs, by biome and spawn category (on top of the End's Endermen). The Drift Whale, the
# Watcher and the Hollow aren't here: MobSpawning.java places them one at a time.
MOB_SPAWNS = {
    "nebula_fields": {"creature": [("pollinator", 12, 3)]},
    "chorus_thicket": {"creature": [("pollinator", 8, 2)]},
    "hollow_isles": {"monster": [("umbral_stalker", 4, 1)], "creature": [("void_wanderer", 4, 1)]},
    "corona": {"monster": [("cinder_mite", 8, 4)], "creature": [("void_ray", 10, 3)]},
}


# Endermen live where the End is alive; the dead Ashen Barrens and the sun-scorched Corona have none.
NO_ENDERMEN = ("ashen_barrens", "corona")


def mob_spawns(biome):
    spawns = {"monster": [] if biome in NO_ENDERMEN else [{"type": "minecraft:enderman", "count": 4, "weight": 10}]}
    for category, entries in MOB_SPAWNS.get(biome, {}).items():
        spawns.setdefault(category, [])
        spawns[category] += [{"type": rid(mob), "count": count, "weight": weight} for mob, weight, count in entries]
    return {"argument": {"spawn_costs": {}, "spawns_by_category": spawns}, "modifier": "overlay"}


# Which music each astral biome plays (sounds in ModSounds.java; who made it in sounds/credits.txt).
# The Ashen Barrens keep the End's own music: the dead country gets nothing new.
BIOME_MUSIC = {"nebula_fields": "music.nebula_fields", "chorus_thicket": "music.chorus_thicket",
               "glimmering_crags": "music.glimmering_crags", "hollow_isles": "music.outer_end",
               "comet_wake": "music.outer_end", "corona": "music.outer_end"}


def background_music(biome):
    sound = BIOME_MUSIC.get(biome)
    if not sound:
        return {}
    music = {"sound": rid(sound), "min_delay": 3000, "max_delay": 12000, "replace_current_music": False}
    return {"minecraft:audio/background_music": {"default": music}}


def end_biome(name, english, fog, water, features_step4=(), features_step7=(), features_step9=(),
              particle=None, particle_chance=0.0):
    attributes = {
        "minecraft:gameplay/natural_mob_spawns": mob_spawns(name),
        "minecraft:visual/fog_color": fog,
        **background_music(name),
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

    # ---- Ashen Barrens: the dead country. Pale End Stone shelves under the auroras, thin Shimmer Grass,
    # standing stones, the ribs of things long dead, and withered Lumineshrooms. Nothing lives here.
    ground_cover("aurora_shimmer_grass", "shimmer_grass", 90)
    plant_patch("aurora_shimmer_reeds", "shimmer_reed", rarity=3, count=24, spread=6)
    plant_patch("ashen_barrens_withered_lumineshrooms", "ashen_lumineshroom", rarity=2, count=8, spread=5,
                on=[rid("pale_end_stone")])
    big_feature("aurora_ribcages", "leviathan_ribcage", rarity=5, heightmap="WORLD_SURFACE_WG")
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
    end_biome("ashen_barrens", "Ashen Barrens", fog="#1b2329", water="#5a7e80",
              features_step4=[rid("aurora_ribcages"), rid("aurora_standing_stones"), rid("aurora_leviathan_ribs")],
              features_step9=[rid("aurora_shimmer_reeds"), rid("aurora_shimmer_grass"), rid("ashen_barrens_withered_lumineshrooms")],
              particle="minecraft:white_ash", particle_chance=0.012)

    # ---- Comet Wake: Charred End Stone, Stardust veins, blue ice, Comet Cores, Comet Cress
    ore("stardust_vein", "stardust_block", rid("charred_end_stone"), size=14)
    placed_ore("comet_wake_stardust_veins", "stardust_vein", count=7, min_y=20, max_y=80)
    feature("comet_ice_shard", {"type": "minecraft:block_blob",
                                "can_place_on": {"type": "minecraft:matching_blocks", "blocks": [rid("charred_end_stone")]},
                                "state": "minecraft:blue_ice"})
    surface_feature("comet_wake_ice_shards", "comet_ice_shard", rarity=5)
    java_feature("comet_core")
    surface_feature("comet_wake_comet_cores", "comet_core", rarity=6)
    big_feature("comet_wake_craters", "crater", rarity=5, heightmap="WORLD_SURFACE_WG")
    big_feature("comet_wake_ice_spires", "comet_ice_spire", rarity=3, heightmap="WORLD_SURFACE_WG")
    java_feature("stardust_underhang")
    placed("comet_wake_stardust_underhangs", {"feature": rid("stardust_underhang"), "placement": [
        {"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
    end_biome("comet_wake", "Comet Wake", fog="#2d2533", water="#8f7fb0",
              features_step4=[rid("comet_wake_craters"), rid("comet_wake_comet_cores"), rid("comet_wake_ice_spires"),
                              rid("comet_wake_ice_shards")],
              features_step7=[rid("comet_wake_stardust_veins")],
              features_step9=[rid("comet_wake_stardust_underhangs")], particle="minecraft:ash", particle_chance=0.01)

    # ---- Hollow Isles: Nightstone rings, Void Crystal Ore, Upwells
    # (Void Crystal grows only under the Glimmering Crags now.) Voidgel drips from the undersides.
    feature("voidgel_drip", {"type": rid("underside"), "kind": "voidgel"})
    placed("hollow_isles_voidgel", {"feature": rid("voidgel_drip"), "placement": [
        {"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
    java_feature("upwell")
    placed("hollow_isles_upwells", {"feature": rid("upwell"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
    big_feature("hollow_isles_nullwood_trees", "nullwood_tree", count=3)
    end_biome("hollow_isles", "Hollow Isles", fog="#2c2150", water="#3a2a66",
              features_step4=[rid("hollow_isles_upwells")],
              features_step9=[rid("hollow_isles_nullwood_trees"), "minecraft:chorus_plant", rid("hollow_isles_voidgel")],
              particle="minecraft:reverse_portal", particle_chance=0.002)

    # ---- The Corona: Sunscorched Stone over Charred End Stone; Solarite; adapted and dead life
    ore("solarite_surface_vein", "solarite_ore", rid("sunscorched_stone"), size=8)
    surface_feature("corona_solarite_surface", "solarite_surface_vein", count=5, y_offset=-3)
    ore("solarite_deep_vein", "solarite_ore", rid("charred_end_stone"), size=3)
    placed_ore("corona_solarite_deep", "solarite_deep_vein", count=2, min_y=10, max_y=70)
    plant_patch("corona_flareblooms", "flarebloom", rarity=1, count=40, spread=7)
    ground_cover("corona_sunscale_lichen", "sunscale_lichen", 70)
    big_feature("corona_petrified_stalks", "petrified_stalk", rarity=2)
    plant_patch("corona_ashen_lumineshrooms", "ashen_lumineshroom", rarity=2, count=10, spread=5)
    plant_patch("corona_petrified_vines", "petrified_astral_vine", rarity=3, count=6, spread=5)
    plant_patch("corona_pollinator_husks", "pollinator_husk", rarity=12, count=2, spread=3, vertical=1,
                on=[rid("sunscorched_stone"), rid("charred_end_stone")])
    end_biome("corona", "The Corona", fog="#4a2616", water="#d9823b",
              features_step7=[rid("corona_solarite_deep")],
              features_step4=[rid("corona_solarite_surface")],
              features_step9=[rid("corona_petrified_stalks"), rid("corona_sunscale_lichen"), rid("corona_flareblooms"), rid("corona_ashen_lumineshrooms"),
                              rid("corona_petrified_vines"), rid("corona_pollinator_husks")],
              particle="minecraft:white_ash", particle_chance=0.03)


    # ---- Chorus Thicket (Phase 10): the Nebula Fields' sister. Chorus trees crowded on Chorus Moss,
    # vanilla chorus between them, and the Endermen who eat it.
    # Dense groves of chorus trees with clearings between them (a lone tree now and then).
    big_feature("chorus_thicket_trees", "chorus_tree", groves=(0.15, 0, 7))
    big_feature("chorus_thicket_lone_trees", "chorus_tree", rarity=3)
    plant_patch("chorus_thicket_sprouts", "chorus_sprout", rarity=3, count=6, spread=5)
    plant_patch("chorus_thicket_starblooms", "starbloom", rarity=2, count=16, spread=6)
    ground_cover("chorus_thicket_carpet", "chorus_moss_carpet", 20, on=[rid("chorus_moss")])
    placed("chorus_thicket_chorus", {"feature": "minecraft:chorus_plant", "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 6}},
        {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"}]})
    end_biome("chorus_thicket", "Chorus Thicket", fog="#3a1c3e", water="#c06ad0",
              features_step9=[rid("chorus_thicket_trees"), rid("chorus_thicket_lone_trees"), rid("chorus_thicket_chorus"), rid("chorus_thicket_sprouts"),
                              rid("chorus_thicket_starblooms"), rid("chorus_thicket_carpet")],
              particle="minecraft:portal", particle_chance=0.006)

    # ---- Glimmering Crags (Phase 10): jagged Nightstone peaks grown through with teal Glimmer Crystal.
    # Void Crystal grows here and only here: in the very bottoms of the islands.
    big_feature("glimmering_crags_spires", "glimmer_spire", count=2, heightmap="WORLD_SURFACE_WG")
    plant_patch("glimmering_crags_clusters", "glimmer_cluster", rarity=1, count=12, spread=6, on=[rid("nightstone")])
    feature("void_crystal_underside", {"type": rid("underside"), "kind": "void_crystal"})
    placed("glimmering_crags_void_crystal", {"feature": rid("void_crystal_underside"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
    placed("glimmering_crags_voidgel", {"feature": rid("voidgel_drip"), "placement": [
        {"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}]})
    end_biome("glimmering_crags", "Glimmering Crags", fog="#0f3a3c", water="#3fd6c4",
              features_step4=[rid("glimmering_crags_spires")],
              features_step9=[rid("glimmering_crags_clusters"), rid("glimmering_crags_void_crystal"), rid("glimmering_crags_voidgel")],
              particle="minecraft:glow", particle_chance=0.004)


# ------------------------------------------------------------------ Phase 4: mobs
MOBS = [
    # (id, English name)
    ("pollinator", "End Pollinator"),
    ("void_wanderer", "Void Wanderer"),
    ("umbral_stalker", "Umbral Stalker"),
    ("cinder_mite", "Cinder Mite"),
    ("void_ray", "Void Ray"),
    ("hollow", "The Hollow"),
    ("drift_whale", "Drift Whale"),
    ("watcher", "The Watcher"),
    ("live_comet", "Live Comet"),
    ("spire_wyrm", "Spire Wyrm"),
]


def entity_loot(mob, pools):
    obj = {"type": "minecraft:entity", "pools": pools, "random_sequence": f"{NS}:entities/{mob}"}
    write(DATA / "loot_table" / "entities" / f"{mob}.json", obj)


def player_kill_drop(item, count_max=1, looting=True):
    modifiers = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": count_max}}]
    if looting:
        modifiers.append({"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
                          "enchantment": "minecraft:looting"})
    return {"condition": {"type": "minecraft:killed_by_player"}, "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": rid(item), "modifier": modifiers}]}


def advancement(name, icon, title, description, parent="minecraft:end/root", frame="task", hidden=False, criteria=None,
                requirements=None):
    """An advancement in the End tab. With no criteria it's granted from code (ModAdvancements.award);
    otherwise it uses vanilla triggers. requirements: "any" (one of the criteria) or None (all of them)."""
    criteria = criteria or {"done": {"trigger": "minecraft:impossible"}}
    reqs = [list(criteria)] if requirements == "any" else [[k] for k in criteria]
    write(DATA / "advancement" / "end" / f"{name}.json", {
        "parent": parent,
        "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.{NS}.{name}.title"},
                    "description": {"translate": f"advancements.{NS}.{name}.description"},
                    "frame": frame, "hidden": hidden, "show_toast": True, "announce_to_chat": frame != "task" or not hidden},
        "criteria": criteria,
        "requirements": reqs,
    })
    lang[f"advancements.{NS}.{name}.title"] = title
    lang[f"advancements.{NS}.{name}.description"] = description


def mobs_phase4():
    for mob, english in MOBS:
        lang[f"entity.{NS}.{mob}"] = english
        egg = f"{mob}_spawn_egg"
        item_model(egg, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{egg}")}})
        item_def(egg, rid(f"item/{egg}"))
        lang[f"item.{NS}.{egg}"] = f"{english.replace('The ', '')} Spawn Egg"

    # Drops. Killing a Pollinator or a Void Ray gives nothing useful - there's no reason to.
    entity_loot("pollinator", [])
    entity_loot("void_ray", [])
    # A Drift Whale never fights back; killing one gets you a few scales, and nothing else.
    entity_loot("drift_whale", [player_kill_drop("whalelight_scale", 3, looting=False)])
    entity_loot("watcher", [])
    entity_loot("live_comet", [])   # what it gives, it gives while it flies (see LiveComet)
    entity_loot("void_wanderer", [player_kill_drop("cracked_lens", 1, looting=False)])
    entity_loot("umbral_stalker", [player_kill_drop("umbral_membrane", 1)])
    entity_loot("hollow", [{"condition": {"type": "minecraft:killed_by_player"}, "rolls": 1,
                                         "entries": [{"type": "minecraft:item", "name": rid("abyss_shard")}]}])
    entity_loot("cinder_mite", [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid("cinder"), "modifier": [
        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}},
        {"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
         "enchantment": "minecraft:looting"}]}]}])

    # Flying and floating mobs don't take fall damage.
    for mob in ("pollinator", "void_wanderer", "void_ray", "hollow", "drift_whale", "watcher", "live_comet"):
        tag("entity_type", "minecraft:fall_damage_immune", rid(mob))

    # What End Pollinators visit (and spread when they pollinate it).
    for plant in ("lumineshroom", "starbloom", "shimmer_grass", "nebula_tufts"):
        tag("block", f"{NS}:pollinator_flora", rid(plant))

    # Blocks
    drop_self("spore_lantern")
    drop_self("whalelight_scale")
    tag("block", "minecraft:mineable/pickaxe", rid("whalelight_scale"))
    shaped("spore_lantern", "spore_lantern", [" S ", "SGS", " S "], {"S": rid("glowing_spores"), "G": rid("astral_glass")})
    recipe("glow_ink_sac_from_glowing_spores", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                "ingredients": [rid("glowing_spores"), rid("glowing_spores"), "minecraft:ink_sac"],
                                                "result": {"id": "minecraft:glow_ink_sac"}})

    # Star Chart messages
    lang[f"item.{NS}.star_chart.marks"] = "Marks a far-off region: %s"
    lang[f"item.{NS}.star_chart.blank"] = "The chart is blank."
    lang[f"item.{NS}.star_chart.direction"] = "The chart marks the %s, %s blocks %s of here."
    for d, english in (("north", "north"), ("north_east", "north-east"), ("east", "east"), ("south_east", "south-east"),
                       ("south", "south"), ("south_west", "south-west"), ("west", "west"), ("north_west", "north-west")):
        lang[f"item.{NS}.star_chart.{d}"] = english

    # Advancements the mobs grant (see ModAdvancements.java).
    advancement("something_big_out_there", rid("drift_whale_spawn_egg"), "Something Big Out There", "See a Drift Whale")
    advancement("whale_song", rid("whalelight_scale"), "Whale Song", "Be close to a Drift Whale while it hums",
                parent=f"{NS}:end/something_big_out_there")
    advancement("tagalong", "minecraft:elytra", "Tagalong", "Fly alongside a Drift Whale for a whole minute",
                parent=f"{NS}:end/whale_song", frame="challenge")
    advancement("something_looks_back", rid("star_chart"), "Something Looks Back",
                "Catch the Watcher in a spyglass", frame="goal", hidden=True)


def flares_phase5():
    """Phase 5: Sunstorms - the Flarebloom's and lichen's flare states, the Sunshade, the Collector
    Pedestal, the flare items, Sun Ward, and the Sunburn damage type."""
    # Flarebloom: shut between flares (a bud), open and glowing during one; an open one gives pollen.
    blockstate("flarebloom", {"variants": {
        "half=lower,open=false": {"model": rid("block/flarebloom_bottom")},
        "half=lower,open=true": {"model": rid("block/flarebloom_bottom")},
        "half=upper,open=false": {"model": rid("block/flarebloom_top")},
        "half=upper,open=true": {"model": rid("block/flarebloom_top_open")},
    }})
    block_model("flarebloom_top_open", {"parent": "minecraft:block/cross", "textures": {"cross": rid("block/flarebloom_top_open")}})
    item_model("flarebloom", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("block/flarebloom_top_open")}})
    loot("flarebloom", [
        {"condition": {"type": "minecraft:survives_explosion"}, "rolls": 1,
         "entries": [{"type": "minecraft:item", "name": rid("flarebloom"), "condition": {
             "type": "minecraft:match_block", "blocks": rid("flarebloom"), "state": {"half": "lower"}}}]},
        {"rolls": 1, "conditions": [{"condition": "minecraft:block_state_property", "block": rid("flarebloom"),
                                     "properties": {"half": "lower", "open": "true"}}],
         "entries": [{"type": "minecraft:item", "name": rid("flare_pollen"), "functions": [
             {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]},
    ])

    # Sunscale Lichen: brighter once a flare has charged it.
    blockstate("sunscale_lichen", {"variants": {
        "charged=false": {"model": rid("block/sunscale_lichen")},
        "charged=true": {"model": rid("block/sunscale_lichen_charged")},
    }})
    block_model("sunscale_lichen_charged", {"parent": "minecraft:block/carpet", "textures": {"wool": rid("block/sunscale_lichen_charged")}})

    # Sunshade: a thin canopy at the top of its block space.
    blockstate("sunshade", {"variants": {"": {"model": rid("block/sunshade")}}})
    tex = rid("block/sunshade")
    block_model("sunshade", {"textures": {"shade": tex, "particle": tex}, "elements": [{
        "from": [0, 14, 0], "to": [16, 16, 16],
        "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#shade"}, "down": {"uv": [0, 0, 16, 16], "texture": "#shade"},
                  **{side: {"uv": [0, 0, 16, 2], "texture": "#shade"} for side in ("north", "south", "east", "west")}}}]})
    item_def("sunshade", rid("block/sunshade"))
    lang[f"block.{NS}.sunshade"] = "Sunshade"
    drop_self("sunshade")
    shaped("sunshade", "sunshade", ["FFF", "S S"], {"F": rid("ray_film"), "S": rid("solarite_ingot")}, count=4)

    # Collector Pedestal: a stone base, a column with a sun emblem, a solarite-banded top; what it holds
    # sits on top (a cross of two planes, like a plant).
    side, top = rid("block/collector_pedestal_side"), rid("block/collector_pedestal_top")

    def box(frm, to, side_uv, top_uv=None):
        faces = {d: {"uv": side_uv, "texture": "#side"} for d in ("north", "south", "east", "west")}
        faces["up"] = {"uv": top_uv or [frm[0], frm[2], to[0], to[2]], "texture": "#top"}
        faces["down"] = {"uv": [frm[0], frm[2], to[0], to[2]], "texture": "#top"}
        return {"from": frm, "to": to, "faces": faces}

    block_model("collector_pedestal", {"textures": {"side": side, "top": top, "particle": side}, "elements": [
        box([1, 0, 1], [15, 2, 15], [1, 14, 15, 16]),
        box([3, 2, 3], [13, 10, 13], [3, 4, 13, 12]),
        box([1, 10, 1], [15, 13, 15], [1, 0, 15, 3], [0, 0, 16, 16]),
    ]})
    parts = [{"apply": {"model": rid("block/collector_pedestal")}}]
    for holding in ("void_crystal", "charged_void_crystal", "flare_bottle", "bottled_flare"):
        t = rid(f"block/pedestal_{holding}")
        planes = []
        for angle in (45, -45):
            planes.append({"from": [2, 13, 8], "to": [14, 25, 8], "shade": False,
                           "rotation": {"origin": [8, 13, 8], "axis": "y", "angle": angle, "rescale": True},
                           "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#item"},
                                     "south": {"uv": [0, 0, 16, 16], "texture": "#item"}}})
        block_model(f"collector_pedestal_{holding}", {"textures": {"item": t, "particle": t}, "elements": planes})
        parts.append({"when": {"holding": holding}, "apply": {"model": rid(f"block/collector_pedestal_{holding}")}})
    blockstate("collector_pedestal", {"multipart": parts})
    item_def("collector_pedestal", rid("block/collector_pedestal"))
    lang[f"block.{NS}.collector_pedestal"] = "Collector Pedestal"
    drop_self("collector_pedestal")
    tag("block", "minecraft:mineable/pickaxe", rid("collector_pedestal"))
    shaped("collector_pedestal", "collector_pedestal", ["SGS", " P ", "PPP"],
           {"S": rid("solarite_ingot"), "G": rid("astral_glass"), "P": rid("polished_sunscorched_stone")})

    # Items
    for name, english in (("flare_pollen", "Flare Pollen"), ("charged_void_crystal", "Charged Void Crystal"),
                          ("flare_bottle", "Flare Bottle"), ("bottled_flare", "Bottled Flare")):
        item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
        item_def(name, rid(f"item/{name}"))
        lang[f"item.{NS}.{name}"] = english
    shaped("flare_bottle", "flare_bottle", ["S", "G", "S"], {"S": rid("solarite_ingot"), "G": rid("astral_glass")})

    # Sun Ward, the potion and its effect
    lang[f"effect.{NS}.sun_ward"] = "Sun Ward"
    for kind, english in (("potion", "Potion of Sun Ward"), ("splash_potion", "Splash Potion of Sun Ward"),
                          ("lingering_potion", "Lingering Potion of Sun Ward"), ("tipped_arrow", "Arrow of Sun Ward")):
        lang[f"item.minecraft.{kind}.effect.sun_ward"] = english

    # Brewing (26.3 brews from data, like crafting): Awkward + Ray Film -> Sun Ward;
    # Sun Ward + Flare Pollen -> the long one; gunpowder and dragon's breath as for any potion.
    def brew(name, container, potion, reagent, out_container, out_potion):
        recipe(f"brewing/{name}", {"type": "minecraft:brewing",
            "input": {"item": container, "potion_contents": {"potions": potion}},
            "reagent": {"item": reagent},
            "output": {"id": out_container, "components": {"minecraft:potion_contents": {"potion": out_potion}}}})
    for kind in ("potion", "splash_potion", "lingering_potion"):
        c = f"minecraft:{kind}"
        brew(f"{kind}_awkward_ray_film", c, "minecraft:awkward", rid("ray_film"), c, rid("sun_ward"))
        brew(f"{kind}_sun_ward_flare_pollen", c, rid("sun_ward"), rid("flare_pollen"), c, rid("long_sun_ward"))
    for potion in ("sun_ward", "long_sun_ward"):
        brew(f"potion_{potion}_gunpowder", "minecraft:potion", rid(potion), "minecraft:gunpowder",
             "minecraft:splash_potion", rid(potion))
        brew(f"splash_potion_{potion}_dragon_breath", "minecraft:splash_potion", rid(potion), "minecraft:dragon_breath",
             "minecraft:lingering_potion", rid(potion))

    # Sunburn: armour doesn't help (only cover, a Sunshade, Sun Ward)
    write(DATA / "damage_type" / "sunburn.json",
          {"message_id": f"{NS}.sunburn", "exhaustion": 0.0, "scaling": "never", "effects": "burning"})
    tag("damage_type", "minecraft:bypasses_armor", rid("sunburn"))
    tag("damage_type", "minecraft:no_knockback", rid("sunburn"))
    lang[f"death.attack.{NS}.sunburn"] = "%1$s was burned by a Sunstorm"
    lang[f"death.attack.{NS}.sunburn.player"] = "%1$s was burned by a Sunstorm while fighting %2$s"

    # What the Corona's players are told
    lang["flare.astralend.warning"] = "The Remnant is brightening - get under cover!"
    lang["flare.astralend.flare"] = "Sunstorm! Open sky burns."
    lang["flare.astralend.afterglow"] = "The flare is passing. The Corona glows."


def chest_loot(name, pools):
    write(DATA / "loot_table" / "chests" / f"{name}.json",
          {"type": "minecraft:chest", "pools": pools, "random_sequence": f"{NS}:chests/{name}"})


def entry(item, weight=1, count=None):
    e = {"type": "minecraft:item", "name": item if ":" in item else rid(item), "weight": weight}
    if count:
        e["functions"] = [{"function": "minecraft:set_count", "add": False,
                           "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}}]
    return e


def chance(item, p, count=None):
    """A pool that gives the item with probability p."""
    return {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": p}],
            "entries": [entry(item, 1, count)]}


def stairs_variants_for(name, model_base):
    variants = stairs_variants(model_base)
    return variants


def structures_phase6():
    """Phase 6: the structures (templates and worldgen JSON come from tools/structures/make_structures.py),
    their loot, the Temporal Crystal, and Star Charts that mark structures."""
    sys.path.insert(0, str(Path(__file__).resolve().parent / "structures"))
    import make_structures
    make_structures.write_data()

    item_model("temporal_crystal", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/temporal_crystal")}})
    item_def("temporal_crystal", rid("item/temporal_crystal"))
    lang[f"item.{NS}.temporal_crystal"] = "Temporal Crystal"
    lang[f"item.{NS}.star_chart.marks_structure"] = "Marks a site: %s"
    for name, english in (("comet_crater", "Comet Crater"), ("wanderer_relay", "Wanderer Relay"), ("whalefall", "Whalefall"),
                          ("shattered_observatory", "Shattered Observatory"), ("resonant_spire", "Resonant Spire"), ("solar_array", "Solar Array Ruins"),
                          ("rift_maw", "Rift Maw"), ("constellation_maze", "Constellation Maze")):
        lang[f"structure.{NS}.{name}"] = english
        tag("worldgen/structure", rid(name), rid(name))      # a Star Chart looks each one up by its own tag

    # ---- Batch C: the Rift Maw and the Constellation Maze
    facings = {"north": 0, "east": 90, "south": 180, "west": 270}
    for lit, front in ((False, "maw_eye"), (True, "maw_eye_lit")):
        block_model(front, {"parent": "minecraft:block/orientable", "textures": {
            "front": rid(f"block/{front}"), "side": rid("block/leviathan_bone_side"), "top": rid("block/leviathan_bone_top")}})
    blockstate("maw_eye", {"variants": {f"facing={f},lit={str(l).lower()}": {"model": rid("block/maw_eye_lit" if l else "block/maw_eye"), "y": r}
                                        for f, r in facings.items() for l in (False, True)}})
    item_def("maw_eye", rid("block/maw_eye"))
    cube("maw_throat")
    blockstate("wormhole", {"variants": {"": {"model": rid("block/wormhole")}}})
    block_model("wormhole", {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/wormhole")}})
    lang[f"block.{NS}.maw_eye"] = "Maw Eye Socket"
    lang[f"block.{NS}.maw_throat"] = "Dormant Maw Throat"
    lang[f"block.{NS}.wormhole"] = "Rift"
    # Star-bridges: nothing at all while dark (a model with no faces), a pane of starlight while lit.
    block_model("star_bridge_dark", {"textures": {"particle": rid("block/star_bridge")}, "elements": []})
    block_model("star_bridge", {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/star_bridge")}})
    blockstate("star_bridge", {"multipart": [
        {"when": {"lit": "false"}, "apply": {"model": rid("block/star_bridge_dark")}},
        {"when": {"lit": "true"}, "apply": {"model": rid("block/star_bridge")}}]})
    lang[f"block.{NS}.star_bridge"] = "Star-Bridge"
    # The Portal Lens.
    item_model("portal_lens", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/portal_lens")}})
    item_def("portal_lens", rid("item/portal_lens"))
    lang[f"item.{NS}.portal_lens"] = "Portal Lens"
    # A spyglass fitted with a Void Wanderer's lens and a crystal to see through time and the void; the
    # Wanderers' Chart Fragments show it the way.
    shapeless("portal_lens", "portal_lens", ["minecraft:spyglass", rid("cracked_lens"), rid("void_crystal"), rid("temporal_crystal"),
                                             rid("chart_fragment"), rid("chart_fragment")], category="equipment")
    lang.update({
        "maw.astralend.eyes_dark": "The Maw's eyes are dark (%s of %s lit). Set Charged Void Crystals in them.",
        "maw.astralend.woken": "The Maw groans open. Its throat is a road of stars.",
        "maw.astralend.attuned": "The Maw knows you now.",
        "maw.astralend.already": "This Maw already knows you.",
        "maw.astralend.not_attuned": "The Maw doesn't know you. Use a Portal Lens on its throat.",
        "maw.astralend.nowhere": "The Maw has nowhere to send you. Wake another.",
        "maw.astralend.price": "The Maw wants an ender pearl, or four chorus fruit.",
        "maw.astralend.arrived": "The Maw spits you out.",
        "maze.astralend.aimed": "The way on from here answers to the %s, %s. Find it through your spyglass.",
        "maze.astralend.focusing": "The stars of the %s grow clearer... hold steady",
        "maze.astralend.lit": "The %s blazes, and a bridge of light answers below!",
        "maze.astralend.nothing": "The %s shines, but nothing answers here.",
        "maze.astralend.caught": "The maze's starlight catches you.",
        "maze.astralend.constellation.leviathan": "Leviathan",
        "maze.astralend.constellation.maw": "Maw",
        "maze.astralend.constellation.wanderer": "Wanderer",
        "maze.astralend.constellation.watcher": "Watcher",
        "maze.astralend.dir.north": "high in the north", "maze.astralend.dir.north_east": "high in the north-east",
        "maze.astralend.dir.east": "high in the east", "maze.astralend.dir.south_east": "high in the south-east",
        "maze.astralend.dir.south": "high in the south", "maze.astralend.dir.south_west": "high in the south-west",
        "maze.astralend.dir.west": "high in the west", "maze.astralend.dir.north_west": "high in the north-west",
    })

    # ---- Phase 7: live comets
    item_model("stardust_mote", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/stardust_mote")}})
    item_def("stardust_mote", rid("item/stardust_mote"))
    lang[f"item.{NS}.stardust_mote"] = "Stardust Mote"
    shapeless("stardust_block_from_motes", "stardust_block", [rid("stardust_mote")] * 4, category="building")
    advancement("catch_a_falling_star", rid("stardust_mote"), "Catch a Falling Star",
                "Glide through a live comet's tail with an elytra and catch a Stardust Mote")

    # A crater's cache: what the comet brought, sometimes a Temporal Crystal, sometimes a chart.
    chest_loot("comet_crater", [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            entry("raw_meteoric_iron", 6, (1, 4)), entry("meteoric_iron_nugget", 5, (2, 7)),
            entry("minecraft:blue_ice", 4, (2, 6)), entry("stardust_block", 3, (1, 4)), entry("comet_cress", 3, (1, 3)),
            entry("void_crystal", 2, (1, 2)), entry("meteoric_iron_ingot", 1, (1, 2))]},
        chance("temporal_crystal", 0.25),
        chance("star_chart", 0.3),
    ])
    # The Whalefall's lore chest: a chart, the light of the bones, and what the whale carried.
    chest_loot("whalefall", [
        {"rolls": 1, "entries": [entry("star_chart")]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            entry("whalelight_scale", 5, (1, 3)), entry("leviathan_bone_block", 4, (1, 3)),
            entry("glowing_spores", 3, (2, 5)), entry("void_crystal", 2, (1, 3)),
            entry("minecraft:nautilus_shell", 1, (1, 1))]},
        chance("temporal_crystal", 0.15),
    ])
    # The Constellation Maze's reward: a Temporal Crystal, and a Star Chart that marks a Rift Maw.
    chest_loot("constellation_maze", [
        {"rolls": 1, "entries": [entry("temporal_crystal")]},
        {"rolls": 1, "entries": [entry("starforge_template")]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid("star_chart"), "functions": [
            {"function": "minecraft:set_components", "components": {"minecraft:custom_data": {"Structure": "rift_maw"}}}]}]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            entry("charged_void_crystal", 3, (1, 2)), entry("chart_fragment", 3, (1, 3)), entry("void_crystal", 4, (2, 5)),
            entry("star_inlay_observatory_quartz", 2, (2, 6)), entry("minecraft:ender_pearl", 3, (2, 4)),
            entry("minecraft:experience_bottle", 2, (2, 5))]},
    ])
    # Observatory Ruins: the vaults (opened with a Chart Fragment) hold the Cartographers' best finds; the
    # chest holds what was left lying about.
    chest_loot("observatory_vault", [
        {"rolls": 1, "entries": [entry("temporal_crystal", 3), entry("charged_void_crystal", 2, (1, 2)),
                                 entry("star_chart", 2), entry("void_crystal_block", 1)]},
        chance("starforge_template", 0.5),
        chance("rift_pearl", 0.05),
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 3}, "entries": [
            entry("void_crystal", 4, (2, 5)), entry("meteoric_iron_ingot", 3, (1, 3)), entry("chart_fragment", 2, (1, 2)),
            entry("minecraft:spyglass", 2), entry("minecraft:ender_pearl", 3, (1, 3)), entry("star_inlay_observatory_quartz", 2, (2, 6))]},
    ])
    chest_loot("observatory_ruins", [
        {"rolls": 1, "entries": [entry("star_chart")]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            entry("observatory_quartz", 5, (2, 8)), entry("cracked_lens", 3, (1, 2)), entry("glowing_spores", 3, (2, 5)),
            entry("void_crystal", 3, (1, 3)), entry("minecraft:paper", 4, (2, 6)), entry("minecraft:book", 2, (1, 2)),
            entry("minecraft:compass", 1)]},
        chance("temporal_crystal", 0.1),
    ])
    # Solar Array Ruins: the best loot in the End (and a Bottled Flare sits in the console).
    chest_loot("solar_array", [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            entry("solarite_ingot", 6, (2, 6)), entry("raw_solarite", 5, (2, 6)), entry("solarite_block", 2, (1, 2)),
            entry("flare_bottle", 3, (1, 2)), entry("flare_pollen", 4, (2, 6)), entry("charged_void_crystal", 2, (1, 1)),
            entry("minecraft:golden_apple", 2), entry("sunshade", 3, (2, 4))]},
        chance("temporal_crystal", 0.4),
        chance("bottled_flare", 0.25),
        chance("starforge_template", 0.35),
    ])


# ------------------------------------------------------------------ Phase 8: Starforging

UPGRADES = ("voidwalker", "meteoric", "stardust", "temporal", "singularity", "radiant")

# The vanilla items an upgrade can go on, with the kind of accent they get (tools/textures/
# starforge_textures.py draws one accent per kind) and the model whose display transforms it shares.
STARFORGE_ITEMS = {}
for _mat in ("diamond", "netherite"):
    for _piece in ("helmet", "chestplate", "leggings", "boots"):
        STARFORGE_ITEMS[f"{_mat}_{_piece}"] = (_piece, "minecraft:item/generated")
    for _tool in ("sword", "pickaxe", "axe", "shovel", "hoe"):
        STARFORGE_ITEMS[f"{_mat}_{_tool}"] = (_tool, "minecraft:item/handheld")
STARFORGE_ITEMS.update({"bow": ("bow", "minecraft:item/bow"), "crossbow": ("crossbow", "minecraft:item/crossbow"),
                        "trident": ("trident", "minecraft:item/trident"), "mace": ("mace", "minecraft:item/mace")})
TRIM_MATERIALS = ("quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst", "resin")


def vanilla_item_definition(name):
    """The item's own vanilla item model (26.3), which an upgrade's accent is layered over."""
    model = lambda m: {"type": "minecraft:model", "model": m}  # noqa: E731
    kind = STARFORGE_ITEMS[name][0]
    if kind in ("helmet", "chestplate", "leggings", "boots"):
        return {"type": "minecraft:select", "property": "minecraft:trim_material",
                "cases": [{"model": model(f"minecraft:item/{name}_{m}_trim"), "when": f"minecraft:{m}"} for m in TRIM_MATERIALS],
                "fallback": model(f"minecraft:item/{name}")}
    if name == "bow":
        return {"type": "minecraft:condition", "property": "minecraft:using_item",
                "on_false": model("minecraft:item/bow"),
                "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
                            "entries": [{"model": model("minecraft:item/bow_pulling_1"), "threshold": 0.65},
                                        {"model": model("minecraft:item/bow_pulling_2"), "threshold": 0.9}],
                            "fallback": model("minecraft:item/bow_pulling_0")}}
    if name == "crossbow":
        return {"type": "minecraft:select", "property": "minecraft:charge_type",
                "cases": [{"model": model("minecraft:item/crossbow_arrow"), "when": "arrow"},
                          {"model": model("minecraft:item/crossbow_firework"), "when": "rocket"}],
                "fallback": {"type": "minecraft:condition", "property": "minecraft:using_item",
                             "on_false": model("minecraft:item/crossbow"),
                             "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:crossbow/pull",
                                         "entries": [{"model": model("minecraft:item/crossbow_pulling_1"), "threshold": 0.58},
                                                     {"model": model("minecraft:item/crossbow_pulling_2"), "threshold": 1.0}],
                                         "fallback": model("minecraft:item/crossbow_pulling_0")}}}
    if name == "trident":
        flip = {"left_rotation": [0.0, 0.0, 0.0, 1.0], "right_rotation": [0.0, 0.0, 0.0, 1.0],
                "scale": [1.0, -1.0, -1.0], "translation": [0.0, 0.0, 0.0]}
        return {"type": "minecraft:select", "property": "minecraft:display_context",
                "cases": [{"model": model("minecraft:item/trident"), "when": ["gui", "ground", "fixed", "on_shelf"]}],
                "fallback": {"type": "minecraft:condition", "property": "minecraft:using_item",
                             "on_false": {"type": "minecraft:special", "base": "minecraft:item/trident_in_hand",
                                          "model": {"type": "minecraft:trident"}},
                             "on_true": {"type": "minecraft:special", "base": "minecraft:item/trident_throwing",
                                         "model": {"type": "minecraft:trident"}},
                             "transformation": flip}}
    return model(f"minecraft:item/{name}")


def starforging_phase8():
    """Phase 8: the Astral Forge, Starforge Templates and Alloy, Rift Pearls and Rift Tiles, which items
    take upgrades, the accent each upgrade leaves on an item, and the Smelting/Pull loot modifier."""
    # ---- the Astral Forge: like a smithing table, its forge mouth on two faces
    block_model("astral_forge", {"parent": "minecraft:block/cube", "textures": {
        "particle": rid("block/astral_forge_side"), "north": rid("block/astral_forge_front"),
        "south": rid("block/astral_forge_front"), "east": rid("block/astral_forge_side"), "west": rid("block/astral_forge_side"),
        "up": rid("block/astral_forge_top"), "down": rid("block/observatory_quartz")}})
    blockstate("astral_forge", {"variants": {"": {"model": rid("block/astral_forge")}}})
    item_def("astral_forge", rid("block/astral_forge"))
    drop_self("astral_forge")
    tag("block", "minecraft:mineable/pickaxe", rid("astral_forge"))
    tag("block", "minecraft:needs_iron_tool", rid("astral_forge"))
    lang[f"block.{NS}.astral_forge"] = "Astral Forge"
    shaped("astral_forge", "astral_forge", [" V ", "QMQ", "QQQ"],
           {"V": rid("void_crystal"), "M": rid("meteoric_iron_block"), "Q": rid("observatory_quartz")}, category="misc")

    # ---- Rift Tiles: no item, no drops; translucent, cracking with age
    variants = {}
    for age in range(4):
        block_model(f"rift_tile_{age}", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                          "textures": {"all": rid(f"block/rift_tile_{age}")}})
        for lava in ("false", "true"):
            variants[f"age={age},over_lava={lava}"] = {"model": rid(f"block/rift_tile_{age}")}
    blockstate("rift_tile", {"variants": variants})
    lang[f"block.{NS}.rift_tile"] = "Rift Tile"

    # ---- the items
    for name, english in (("starforge_template", "Starforge Template"), ("starforge_alloy", "Starforge Alloy"),
                          ("rift_pearl", "Rift Pearl")):
        item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
        item_def(name, rid(f"item/{name}"))
        lang[f"item.{NS}.{name}"] = english
    lang.update({
        f"item.{NS}.starforge_template.kind": "Astral Forge Template",
        f"item.{NS}.starforge_template.applies_to": "Diamond and Netherite Gear, Bows, Crossbows, Tridents, Maces",
        f"item.{NS}.starforge_template.ingredients": "Starforge Alloy and an Upgrade Material",
    })
    # Alloy like a Netherite Ingot: crafted, not forged.
    # Dragon's Breath binds it: the alloy needs the dragon beaten (and its breath bottled) first.
    shapeless("starforge_alloy", "starforge_alloy", [rid("void_crystal"), rid("meteoric_iron_ingot"), rid("meteoric_iron_ingot"),
                                                     "minecraft:dragon_breath", rid("glowing_spores")])
    # Templates copy like smithing templates.
    shaped("starforge_template", "starforge_template", ["DTD", "DQD", "DDD"],
           {"D": "minecraft:diamond", "T": rid("starforge_template"), "Q": rid("observatory_quartz")}, count=2, category="misc")

    # ---- what can be upgraded (a data pack can add to these)
    for mat in ("diamond", "netherite"):
        for piece in ("helmet", "chestplate", "leggings", "boots"):
            tag("item", rid("starforgeable/armor"), f"minecraft:{mat}_{piece}")
        for weapon in ("sword", "axe"):
            tag("item", rid("starforgeable/melee"), f"minecraft:{mat}_{weapon}")
        for tool in ("pickaxe", "shovel", "axe", "hoe"):
            tag("item", rid("starforgeable/tools"), f"minecraft:{mat}_{tool}")
    for weapon in ("mace", "trident"):
        tag("item", rid("starforgeable/melee"), f"minecraft:{weapon}")
    for weapon in ("bow", "crossbow", "trident"):
        tag("item", rid("starforgeable/ranged"), f"minecraft:{weapon}")

    # ---- each upgrade's accent on each item: the vanilla item model, with the accent layered on top
    for kind, parent in {v for v in STARFORGE_ITEMS.values()}:
        for up in UPGRADES:
            item_model(f"starforge/{kind}_{up}", {"parent": parent, "textures": {"layer0": rid(f"item/starforge/{kind}_{up}")}})
    for name, (kind, _parent) in STARFORGE_ITEMS.items():
        base = vanilla_item_definition(name)
        cases = []
        for up in UPGRADES:
            accent = {"type": "minecraft:model", "model": rid(f"item/starforge/{kind}_{up}")}
            if kind == "trident":   # in the hand it's the 3D trident: the flat accent only goes on the icon
                accent = {"type": "minecraft:select", "property": "minecraft:display_context",
                          "cases": [{"model": accent, "when": ["gui", "ground", "fixed", "on_shelf"]}],
                          "fallback": {"type": "minecraft:empty"}}
            cases.append({"when": up, "model": {"type": "minecraft:composite", "models": [base, accent]}})
        write(RES / "assets" / "minecraft" / "items" / f"{name}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:component", "component": rid("starforge_upgrade"),
            "cases": cases, "fallback": base}})

    # ---- Smelting and Pull: one global loot modifier
    write(DATA / "loot_modifiers" / "starforge_tools.json", {"type": rid("starforge_tools"), "conditions": []})
    # (listed in global_loot_modifiers.json by mechanics_phase10)

    # ---- words
    lang.update({
        f"container.{NS}.astral_forge": "Astral Forge",
        f"container.{NS}.astral_forge.error": "This upgrade can't go on that item (or it already has it)",
        f"container.{NS}.astral_forge.slot.0": "Add a Starforge Template",
        f"container.{NS}.astral_forge.slot.1": "Add diamond or netherite armor, weapon or tool, or a bow, crossbow, trident or mace",
        f"container.{NS}.astral_forge.slot.2": "Add Starforge Alloy",
        f"container.{NS}.astral_forge.slot.3": "Add the upgrade's material: Void Crystal and Umbral Membrane, Meteoric Iron, "
                                                 "Stardust, a Temporal Crystal, a Abyss Shard, or Solarite and a Charged Void Crystal",
        f"container.{NS}.astral_forge.slot.4": "Voidwalker and Radiant take a second material here",
        f"upgrade.{NS}.header": "Starforged:",
        f"upgrade.{NS}.line": "%s: %s",
        f"upgrade.{NS}.pearl_loaded": "Holds a Rift Pearl",
        f"upgrade.{NS}.pearl_empty": "No Rift Pearl (right-click one onto it)",
        f"upgrade.{NS}.voidwalker": "Voidwalker", f"upgrade.{NS}.meteoric": "Meteoric", f"upgrade.{NS}.stardust": "Stardust",
        f"upgrade.{NS}.temporal": "Temporal", f"upgrade.{NS}.singularity": "Singularity", f"upgrade.{NS}.radiant": "Radiant",
        f"upgrade.{NS}.effect.rift_walker": "Rift Walker", f"upgrade.{NS}.effect.void_recall": "Void Recall",
        f"upgrade.{NS}.effect.heat_shield": "Heat Shield", f"upgrade.{NS}.effect.smelting": "Smelting",
        f"upgrade.{NS}.effect.light_step": "Lighter Gravity", f"upgrade.{NS}.effect.light_step_boots": "Lighter Gravity, Step Up",
        f"upgrade.{NS}.effect.flatter_shots": "Flatter Shots",
        f"upgrade.{NS}.effect.time_stretch": "Time Stretch", f"upgrade.{NS}.effect.echo_strike": "Echo Strike",
        f"upgrade.{NS}.effect.lensing": "Lensing", f"upgrade.{NS}.effect.sweep_pull": "Sweep Pull", f"upgrade.{NS}.effect.pull": "Pull",
        f"upgrade.{NS}.effect.soak_light": "Soak Light", f"upgrade.{NS}.effect.glow": "Glow",
    })
    advancement("starforged", rid("starforge_template"), "Forged in Starlight", "Forge a Starforge upgrade onto a piece of gear")
    advancement("void_recall", rid("rift_pearl"), "Not Today, Void",
                "Fall into the void and be pulled back by a Rift Pearl", parent=f"{NS}:end/starforged", frame="challenge", hidden=True)


def round_out_phase9():
    """Phase 9: what Mojang would ship with the rest - moss carpet for Chorus Moss, a stew for the
    End's food plants, a compass that works in the End, and a second use for Cinder."""
    # Chorus Moss Carpet, like moss carpet: two moss make three.
    blockstate("chorus_moss_carpet", {"variants": {"": {"model": rid("block/chorus_moss_carpet")}}})
    block_model("chorus_moss_carpet", {"parent": "minecraft:block/carpet", "textures": {"wool": rid("block/chorus_moss")}})
    item_def("chorus_moss_carpet", rid("block/chorus_moss_carpet"))
    drop_self("chorus_moss_carpet")
    tag("block", "minecraft:mineable/hoe", rid("chorus_moss_carpet"))
    lang[f"block.{NS}.chorus_moss_carpet"] = "Chorus Moss Carpet"
    shaped("chorus_moss_carpet", "chorus_moss_carpet", ["##"], {"#": rid("chorus_moss")}, count=3, category="building")

    # Glowing Stew: Lumineshroom, Comet Cress and a bowl. Food, and 30 seconds of Night Vision.
    item_model("glowing_stew", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/glowing_stew")}})
    item_def("glowing_stew", rid("item/glowing_stew"))
    lang[f"item.{NS}.glowing_stew"] = "Glowing Stew"
    shapeless("glowing_stew", "glowing_stew", [rid("lumineshroom"), rid("comet_cress"), "minecraft:bowl"], category="misc")

    # The Star Compass: a compass with four Starblooms round it. Its needle works like a lodestone
    # compass's (the 32 vanilla needle pictures), with the Starbloom points drawn over the rim.
    entries = [{"threshold": 0.0, "model": {"type": "minecraft:model", "model": rid("item/star_compass_16")}}]
    entries += [{"threshold": k - 0.5, "model": {"type": "minecraft:model", "model": rid(f"item/star_compass_{(16 + k) % 32:02d}")}}
                for k in range(1, 32)]
    entries += [{"threshold": 31.5, "model": {"type": "minecraft:model", "model": rid("item/star_compass_16")}}]
    for frame in range(32):
        item_model(f"star_compass_{frame:02d}", {"parent": "minecraft:item/generated", "textures": {
            "layer0": f"minecraft:item/compass_{frame:02d}", "layer1": rid("item/star_compass_overlay")}})
    write(ASSETS / "items" / "star_compass.json", {"model": {
        "type": "minecraft:range_dispatch", "property": "minecraft:compass", "target": "lodestone", "scale": 32.0,
        "entries": entries}})
    lang[f"item.{NS}.star_compass"] = "Star Compass"
    lang[f"item.{NS}.star_compass.points"] = "Points to the End's central island"
    shaped("star_compass", "star_compass", [" S ", "SCS", " S "], {"S": rid("starbloom"), "C": "minecraft:compass"}, category="equipment")

    # Cinder, besides burning: like blaze powder, it makes fire charges.
    write(DATA / "recipe" / "fire_charge_from_cinder.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [rid("cinder"), "minecraft:gunpowder"], "result": {"count": 2, "id": "minecraft:fire_charge"}})


ASTRAL_BIOMES = ("nebula_fields", "ashen_barrens", "comet_wake", "hollow_isles", "corona", "chorus_thicket", "glimmering_crags")


def has_item(item):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}


def killed(entity):
    return {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": entity}}}}


def in_biome(biome):
    return {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"biomes": biome}}}}}


def advancements_phase9():
    """Phase 9: the astral End's advancements, as one tree in the End tab, growing from "Remote
    Getaway" (going through an End Gateway). Most use vanilla triggers; the rest are granted by the
    thing they're for (ModAdvancements.award)."""
    biomes = {b: in_biome(rid(b)) for b in ASTRAL_BIOMES}
    root = rid("end/astral_horizons")
    advancement("astral_horizons", rid("starbloom"), "Astral Horizons", "Set foot in one of the astral biomes of the outer End",
                parent="minecraft:end/enter_end_gateway", criteria=biomes, requirements="any")
    advancement("chart_the_stars", rid("star_chart"), "Chart the Stars", "Visit every astral biome of the outer End",
                parent=root, frame="challenge", criteria=biomes)
    # Materials
    advancement("falling_upward", rid("stardust_block"), "Falling Upward", "Pick up Stardust, the sand that falls up",
                parent=root, criteria={"stardust": has_item(rid("stardust_block"))})
    advancement("catch_a_falling_star", rid("stardust_mote"), "Catch a Falling Star",
                "Glide through a live comet's tail with an elytra and catch a Stardust Mote", parent=rid("end/falling_upward"))
    advancement("iron_from_the_sky", rid("meteoric_iron_ingot"), "Iron From the Sky", "Smelt a Meteoric Iron Ingot",
                parent=root, criteria={"ingot": has_item(rid("meteoric_iron_ingot"))})
    advancement("starforged", rid("starforge_template"), "Forged in Starlight", "Forge a Starforge upgrade onto a piece of gear",
                parent=rid("end/iron_from_the_sky"))
    advancement("head_to_toe", rid("starforge_alloy"), "Starforged From Head to Toe", "Wear four pieces of Starforged armor at once",
                parent=rid("end/starforged"), frame="goal")
    advancement("void_recall", rid("rift_pearl"), "Not Today, Void", "Fall into the void and be pulled back by a Rift Pearl",
                parent=rid("end/starforged"), frame="challenge", hidden=True)
    # The Cartographers
    advancement("hold_still", rid("chart_fragment"), "Hold Still", "Stand still while a Void Wanderer charts you",
                parent=root)
    advancement("connect_the_dots", rid("star_inlay_observatory_quartz"), "Connect the Dots",
                "Cross a Constellation Maze and open the chest on its dais", parent=rid("end/hold_still"),
                criteria={"maze": {"trigger": "minecraft:player_generates_container_loot",
                                   "conditions": {"loot_tables": [rid("chests/constellation_maze")]}}})
    advancement("say_ahh", rid("portal_lens"), "Say Ahh", "Wake a Rift Maw", parent=rid("end/hold_still"), frame="goal")
    advancement("something_looks_back", rid("star_chart"), "Something Looks Back",
                "Catch a glimpse of the Watcher through a spyglass", parent=rid("end/hold_still"), hidden=True)
    # The Corona
    advancement("weather_the_storm", rid("sunshade"), "Weather the Storm", "Be in the Corona when a Sunstorm dies down",
                parent=root)
    advancement("harvest_moon", "minecraft:shears", "Harvest Moon", "Shear the film from a Void Ray's wings",
                parent=rid("end/weather_the_storm"))
    advancement("bottled_sunlight", rid("bottled_flare"), "Bottled Sunlight", "Fill a Flare Bottle with a Sunstorm",
                parent=rid("end/weather_the_storm"), criteria={"bottle": has_item(rid("bottled_flare"))})
    advancement("into_the_hollow", rid("abyss_shard"), "Into the Hollow", "Defeat the Hollow",
                parent=root, frame="goal", criteria={"guardian": killed(rid("hollow"))})
    # The Drift Whales
    advancement("something_big_out_there", rid("whalelight_scale"), "Something Big Out There", "See a Drift Whale", parent=root)
    advancement("whale_song", rid("whalelight_scale"), "Whale Song", "Be close to a Drift Whale while it hums",
                parent=rid("end/something_big_out_there"))
    advancement("tagalong", "minecraft:elytra", "Tagalong", "Fly alongside a Drift Whale for a whole minute",
                parent=rid("end/whale_song"), frame="challenge")



def mechanics_phase10():
    """Phase 10, batch B: the Lodestar, the Chorus Flower on a Stick, and Starforge Templates in End
    City treasure."""
    # The Lodestar: Lodestone ringed with Meteoric Iron. Comets don't land within 48 blocks of one.
    blockstate("lodestar", {"variants": {"": {"model": rid("block/lodestar")}}})
    block_model("lodestar", {"parent": "minecraft:block/cube_column",
                             "textures": {"end": rid("block/lodestar_top"), "side": rid("block/lodestar_side")}})
    item_def("lodestar", rid("block/lodestar"))
    lang[f"block.{NS}.lodestar"] = "Lodestar"
    drop_self("lodestar")
    tag("block", "minecraft:mineable/pickaxe", rid("lodestar"))
    shaped("lodestar", "lodestar", [" M ", "MLM", " M "], {"M": rid("meteoric_iron_ingot"), "L": "minecraft:lodestone"},
           category="misc")

    # The Chorus Flower on a Stick: steers a Drift Whale, like a carrot on a stick steers a pig.
    item_model("chorus_flower_on_a_stick", {"parent": "minecraft:item/handheld_rod",
                                            "textures": {"layer0": rid("item/chorus_flower_on_a_stick")}})
    item_def("chorus_flower_on_a_stick", rid("item/chorus_flower_on_a_stick"))
    lang[f"item.{NS}.chorus_flower_on_a_stick"] = "Chorus Flower on a Stick"
    shaped("chorus_flower_on_a_stick", "chorus_flower_on_a_stick", ["R ", " C"],
           {"R": "minecraft:fishing_rod", "C": "minecraft:chorus_flower"}, category="equipment")
    advancement("whale_rider", rid("chorus_flower_on_a_stick"), "Whale Rider", "Ride a Drift Whale",
                parent=rid("end/whale_song"), frame="goal")

    # Starforge Templates turn up in End City treasure (a global loot modifier: StarforgeLoot.Add).
    write(DATA / "loot_modifiers" / "end_city_templates.json", {
        "type": rid("add_item"), "item": rid("starforge_template"), "chance": 0.2,
        "conditions": [{"type": "forge:loot_table_id", "loot_table_id": "minecraft:chests/end_city_treasure"}]})
    write(RES / "data" / "forge" / "loot_modifiers" / "global_loot_modifiers.json",
          {"replace": False, "entries": [rid("starforge_tools"), rid("end_city_templates")]})



def content_phase10():
    """Phase 10, batch C: the Chorus Thicket's wood and bloom, the Glimmering Crags' crystal, Voidgel
    and End Chimes. (The wood set itself is in WOOD_SETS.)"""
    # Chorus Bloom: leaves. Shears or Silk Touch keep them; otherwise Chorus Sprouts, sticks and now
    # and then a Chorus Fruit.
    blockstate("chorus_bloom", {"variants": {"": {"model": rid("block/chorus_bloom")}}})
    block_model("chorus_bloom", {"parent": "minecraft:block/leaves", "textures": {"all": rid("block/chorus_bloom")}})
    item_def("chorus_bloom", rid("block/chorus_bloom"))
    lang[f"block.{NS}.chorus_bloom"] = "Chorus Bloom"
    keep = {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}
    loot("chorus_bloom", [
        {"entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": keep, "name": rid("chorus_bloom")},
            {"type": "minecraft:item", "condition": {"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:survives_explosion"},
                {"type": "minecraft:table_bonus", "chances": [0.05, 0.0625, 0.083333336, 0.1], "enchantment": "minecraft:fortune"}]},
             "name": rid("chorus_sprout")}]}], "rolls": 1},
        {"condition": {"type": "minecraft:inverted", "term": keep}, "entries": [{
            "type": "minecraft:item",
            "condition": {"type": "minecraft:table_bonus", "chances": [0.03, 0.04, 0.05, 0.08], "enchantment": "minecraft:fortune"},
            "name": "minecraft:chorus_fruit"}], "rolls": 1},
    ])
    drop_self("chorus_sprout")
    for kind in ("block", "item"):
        tag(kind, "minecraft:leaves", rid("chorus_bloom"))
        tag(kind, "minecraft:saplings", rid("chorus_sprout"))
        for log in ("chorus_stalk", "chorus_wood", "stripped_chorus_stalk", "stripped_chorus_wood"):
            tag(kind, f"{NS}:chorus_logs", rid(log))
        tag(kind, "minecraft:logs_that_burn", f"#{NS}:chorus_logs")
    tag("block", "minecraft:mineable/hoe", rid("chorus_bloom"))

    # Glimmer Crystal and Glimmer Clusters (they point the way they grow, like amethyst clusters).
    cube("glimmer_crystal")
    lang[f"block.{NS}.glimmer_crystal"] = "Glimmer Crystal"
    drop_self("glimmer_crystal")
    block_model("glimmer_cluster", {"parent": "minecraft:block/cross", "textures": {"cross": rid("block/glimmer_cluster")}})
    turns = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90},
             "west": {"x": 90, "y": 270}}
    blockstate("glimmer_cluster", {"variants": {
        f"facing={f}": {"model": rid("block/glimmer_cluster"), **r} for f, r in turns.items()}})
    item_model("glimmer_cluster", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("block/glimmer_cluster")}})
    item_def("glimmer_cluster", rid("item/glimmer_cluster"))
    lang[f"block.{NS}.glimmer_cluster"] = "Glimmer Cluster"
    drop_self("glimmer_cluster")
    for name in ("glimmer_crystal", "glimmer_cluster"):
        tag("block", "minecraft:mineable/pickaxe", rid(name))
    shaped("glimmer_crystal", "glimmer_crystal", ["CC", "CC"], {"C": rid("glimmer_cluster")}, category="building")

    # Voidgel: see-through jelly.
    blockstate("voidgel", {"variants": {"": {"model": rid("block/voidgel")}}})
    block_model("voidgel", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                            "textures": {"all": rid("block/voidgel")}})
    item_def("voidgel", rid("block/voidgel"))
    lang[f"block.{NS}.voidgel"] = "Voidgel"
    drop_self("voidgel")

    # End Chimes: hung under a block. End Rods and amethyst.
    blockstate("end_chimes", {"variants": {"": {"model": rid("block/end_chimes")}}})
    block_model("end_chimes", {"parent": "minecraft:block/cross", "textures": {"cross": rid("block/end_chimes")}})
    item_model("end_chimes", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("block/end_chimes")}})
    item_def("end_chimes", rid("item/end_chimes"))
    lang[f"block.{NS}.end_chimes"] = "End Chimes"
    drop_self("end_chimes")
    shaped("end_chimes", "end_chimes", ["AAA", "R R", "R R"], {"A": "minecraft:amethyst_shard", "R": "minecraft:end_rod"},
           category="misc")
    # The Potion of Upwelling: an Upwell Mote in an Awkward Potion; redstone makes it last longer.
    lang[f"effect.{NS}.upwelling"] = "Upwelling"
    for kind, english in (("potion", "Potion of Upwelling"), ("splash_potion", "Splash Potion of Upwelling"),
                          ("lingering_potion", "Lingering Potion of Upwelling"), ("tipped_arrow", "Arrow of Upwelling")):
        lang[f"item.minecraft.{kind}.effect.upwelling"] = english

    def brew(name, container, potion, reagent, out_container, out_potion):
        recipe(f"brewing/{name}", {"type": "minecraft:brewing",
            "input": {"item": container, "potion_contents": {"potions": potion}},
            "reagent": {"item": reagent},
            "output": {"id": out_container, "components": {"minecraft:potion_contents": {"potion": out_potion}}}})
    for kind in ("potion", "splash_potion", "lingering_potion"):
        c = f"minecraft:{kind}"
        brew(f"{kind}_awkward_upwell_mote", c, "minecraft:awkward", rid("upwell_mote"), c, rid("upwelling"))
        brew(f"{kind}_upwelling_redstone", c, rid("upwelling"), "minecraft:redstone", c, rid("long_upwelling"))
    for potion in ("upwelling", "long_upwelling"):
        brew(f"potion_{potion}_gunpowder", "minecraft:potion", rid(potion), "minecraft:gunpowder",
             "minecraft:splash_potion", rid(potion))
        brew(f"splash_potion_{potion}_dragon_breath", "minecraft:splash_potion", rid(potion), "minecraft:dragon_breath",
             "minecraft:lingering_potion", rid(potion))
    advancement("what_goes_up", rid("upwell_mote"), "What Goes Up", "Catch an Upwell Mote",
                parent=rid("end/astral_horizons"), criteria={"mote": has_item(rid("upwell_mote"))})
    # ---- the Resonant Spires and the Spire Wyrm
    entity_loot("spire_wyrm", [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid("resonant_key")}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid("rift_pearl")}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid("glimmer_cluster"), "functions": [
            {"function": "minecraft:set_count", "add": False, "count": {"type": "minecraft:uniform", "min": 2, "max": 5}}]}]},
    ])

    def potion(name, weight=1):
        return {"type": "minecraft:item", "name": "minecraft:potion", "weight": weight,
                "functions": [{"function": "minecraft:set_potion", "id": rid(name)}]}
    # At the foot of the spire: the means to climb it.
    chest_loot("resonant_spire_foot", [
        {"rolls": 2, "entries": [potion("upwelling", 3), potion("long_upwelling", 1)]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            entry("glimmer_cluster", 3, (1, 3)), entry("minecraft:arrow", 3, (4, 12)), entry("upwell_mote", 1, (1, 2)),
            entry("minecraft:golden_carrot", 2, (2, 5)), entry("minecraft:shulker_shell", 1)]},
    ])
    chest_loot("resonant_spire", [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            entry("glimmer_crystal", 3, (2, 6)), entry("void_crystal", 2, (1, 2)), entry("meteoric_iron_ingot", 3, (1, 3)),
            entry("minecraft:ender_pearl", 3, (1, 3)), entry("minecraft:diamond", 1, (1, 2)), entry("upwell_mote", 2, (1, 3)),
            entry("minecraft:experience_bottle", 2, (2, 5)), entry("end_chimes", 1)]},
        {"rolls": 1, "entries": [potion("upwelling", 2), potion("long_upwelling", 1)]},
        chance("starforge_template", 0.15),
    ])
    # The Resonance Vault, opened with the Resonant Key: one roll of treasure per player.
    chest_loot("resonance_vault", [
        {"rolls": 1, "entries": [entry("wyrm_banner_pattern", 1), entry("spire_banner_pattern", 1)]},
        {"rolls": 1, "entries": [entry("eclipse_armor_trim_smithing_template", 1)]},
        {"rolls": 1, "entries": [entry("music_disc_observing_the_star", 1)]},
        {"rolls": 1, "entries": [entry("starforge_template", 1)]},
        chance("rift_pearl", 0.5),
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            entry("void_crystal", 3, (1, 3)), entry("charged_void_crystal", 2, (1, 2)), entry("minecraft:diamond", 2, (2, 4)),
            entry("minecraft:netherite_scrap", 1, (1, 2)), entry("temporal_crystal", 1)]},
    ])
    # Banner patterns (the Wyrm coiled, and the Spire) and the Eclipse armour trim.
    colours = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
               "purple", "blue", "brown", "green", "red", "black")
    for pattern, english in (("wyrm", "Wyrm"), ("spire", "Spire")):
        write(DATA / "banner_pattern" / f"{pattern}.json", {"asset_id": rid(pattern), "translation_key": f"block.{NS}.banner.{pattern}"})
        tag("banner_pattern", f"{NS}:pattern_item/{pattern}", rid(pattern))
        item = f"{pattern}_banner_pattern"
        item_model(item, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        item_def(item, rid(f"item/{item}"))
        lang[f"item.{NS}.{item}"] = f"{english} Banner Pattern"
        lang[f"item.{NS}.{item}.desc"] = english
        for c in colours:
            lang[f"block.{NS}.banner.{pattern}.{c}"] = f"{c.replace('_', ' ').title()} {english}"
    write(DATA / "trim_pattern" / "eclipse.json", {"asset_id": rid("eclipse"),
                                                    "description": {"translate": f"trim_pattern.{NS}.eclipse"}, "decal": False})
    lang[f"trim_pattern.{NS}.eclipse"] = "Eclipse Armor Trim"
    template = "eclipse_armor_trim_smithing_template"
    item_model(template, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{template}")}})
    item_def(template, rid(f"item/{template}"))
    lang[f"item.{NS}.{template}"] = "Smithing Template"
    tag("item", "minecraft:trim_templates", rid(template))
    recipe(f"{template}_smithing_trim", {"type": "minecraft:smithing_trim", "template": rid(template),
                                         "base": "#minecraft:trimmable_armor", "addition": "#minecraft:trim_materials",
                                         "pattern": rid("eclipse")})
    shaped(template, template, ["DTD", "DND", "DDD"], {"D": "minecraft:diamond", "T": rid(template), "N": rid("nightstone")},
           count=2, category="misc")
    # Music: sounds.json for the biome tracks and the disc; the disc's jukebox song.
    tracks = {"music.nebula_fields": "music/ambient_relaxing_loop", "music.chorus_thicket": "music/heavenly_loop",
              "music.glimmering_crags": "music/space_music", "music.outer_end": "music/out_there",
              "music_disc.observing_the_star": "records/observing_the_star"}
    sounds = {event: {"sounds": [{"name": rid(path), "stream": True}]} for event, path in tracks.items()}
    # The Spire Wyrm's voice (tools/sounds/wyrm_sounds.py): each sound and how many variants it has.
    for name, count in {"roar": 2, "ambient": 3, "hurt": 3, "death": 1, "charge": 2, "breath": 2, "slam": 2, "snore": 2}.items():
        sounds[f"entity.spire_wyrm.{name}"] = {"subtitle": f"subtitles.{NS}.entity.spire_wyrm.{name}",
                                                "sounds": [rid(f"mob/spire_wyrm/{name}{i}") for i in range(1, count + 1)]}
    lang.update({f"subtitles.{NS}.entity.spire_wyrm.roar": "Spire Wyrm roars", f"subtitles.{NS}.entity.spire_wyrm.ambient": "Spire Wyrm growls",
                 f"subtitles.{NS}.entity.spire_wyrm.hurt": "Spire Wyrm hurts", f"subtitles.{NS}.entity.spire_wyrm.death": "Spire Wyrm dies",
                 f"subtitles.{NS}.entity.spire_wyrm.charge": "Spire Wyrm rears up", f"subtitles.{NS}.entity.spire_wyrm.breath": "Spire Wyrm breathes",
                 f"subtitles.{NS}.entity.spire_wyrm.slam": "Spire Wyrm crashes down", f"subtitles.{NS}.entity.spire_wyrm.snore": "Spire Wyrm snores"})
    write(ASSETS / "sounds.json", sounds)
    write(DATA / "jukebox_song" / "observing_the_star.json", {
        "sound_event": rid("music_disc.observing_the_star"),
        "description": {"translate": f"jukebox_song.{NS}.observing_the_star"},
        "length_in_seconds": 134.4, "comparator_output": 7})
    lang[f"jukebox_song.{NS}.observing_the_star"] = "madeso - Observing the Star"
    disc = "music_disc_observing_the_star"
    item_model(disc, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{disc}")}})
    item_def(disc, rid(f"item/{disc}"))
    lang[f"item.{NS}.{disc}"] = "Music Disc"
    advancement("wyrmslayer", rid("resonant_key"), "Wyrmslayer", "Defeat a Spire Wyrm atop its Resonant Spire",
                parent=rid("end/astral_horizons"), frame="challenge", criteria={"wyrm": killed(rid("spire_wyrm"))})
    advancement("ring_out", "minecraft:bell", "Ring Out", "Stun a Spire Wyrm with the ringing of a bell",
                parent=rid("end/wyrmslayer"))
    advancement("still_air", rid("end_chimes"), "Still the Air", "Hang End Chimes, and stop an Enderman's teleport with them",
                parent=rid("end/astral_horizons"))


def recipe_unlocks():
    """Every recipe unlocks in the recipe book once you hold one of its ingredients, the way vanilla's
    do (data/astralend/advancement/recipes/...)."""
    folders = {"building": "building_blocks", "misc": "misc", "equipment": "tools", "redstone": "redstone", "food": "food",
               "blocks": "building_blocks"}
    for file in sorted((DATA / "recipe").glob("*.json")):
        obj = json.loads(file.read_text())
        name = file.stem
        ingredients = []
        if "key" in obj:
            ingredients = list(obj["key"].values())
        elif "ingredients" in obj:
            ingredients = obj["ingredients"]
        elif "ingredient" in obj:
            ingredients = [obj["ingredient"]]
        elif "base" in obj:
            ingredients = [obj["base"]]
        items = []
        for ing in ingredients:
            if isinstance(ing, str) and not ing.startswith("#") and ing not in items and ing != "minecraft:bowl":
                items.append(ing)
        criteria = {"has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipes": rid(name)}}}
        for item in items[:3]:
            criteria[f"has_{item.split(':')[1]}"] = has_item(item)
        folder = folders.get(obj.get("category", "misc"), "misc")
        write(DATA / "advancement" / "recipes" / folder / f"{name}.json", {
            "parent": "minecraft:recipes/root", "criteria": criteria, "requirements": [list(criteria)],
            "rewards": {"recipes": [rid(name)]}})


def main():
    # Clear previously generated folders so renamed/removed content doesn't linger.
    for folder in (ASSETS / "blockstates", ASSETS / "models", ASSETS / "items", DATA / "loot_table",
                   RES / "assets" / "minecraft" / "items", DATA / "loot_modifiers", RES / "data" / "forge",
                   DATA / "recipe", RES / "data" / NS / "tags", MC_DATA / "tags", DATA / "worldgen",
                   MC_DATA / "dimension", DATA / "advancement"):
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

    for spec in WOOD_SETS:
        wood_set(*spec)

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

    # Two-block-tall plants (like a lilac). Only the lower half drops the item.
    for name, english in (("flarebloom", "Flarebloom"), ("shimmer_reed", "Shimmer Reed")):
        blockstate(name, {"variants": {
            "half=lower": {"model": rid(f"block/{name}_bottom")},
            "half=upper": {"model": rid(f"block/{name}_top")},
        }})
        for half in ("bottom", "top"):
            block_model(f"{name}_{half}", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{name}_{half}")}})
        item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"block/{name}_top")}})
        item_def(name, rid(f"item/{name}"))
        lang[f"block.{NS}.{name}"] = english
        loot(name, [{
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{"type": "minecraft:item", "name": rid(name), "condition": {
                "type": "minecraft:match_block", "blocks": rid(name), "state": {"half": "lower"}}}],
            "rolls": 1,
        }])
    tag("block", "minecraft:tall_flowers", rid("flarebloom"))
    tag("item", "minecraft:tall_flowers", rid("flarebloom"))
    tag("block", "minecraft:replaceable_by_trees", rid("shimmer_reed"))

    # Nebula Tendrils: hang down like weeping vines; climbable.
    for name, texture in (("nebula_tendril", "nebula_tendril"), ("nebula_tendril_plant", "nebula_tendril_plant")):
        blockstate(name, {"variants": {"": {"model": rid(f"block/{name}")}}})
        block_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{texture}")}})
        tag("block", "minecraft:climbable", rid(name))
        loot(name, [{"entries": [{"type": "minecraft:item", "condition": {"type": "minecraft:any_of", "terms": [
            "minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}, "name": rid("nebula_tendril")}], "rolls": 1}])
    item_model("nebula_tendril", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("block/nebula_tendril")}})
    item_def("nebula_tendril", rid("item/nebula_tendril"))
    lang[f"block.{NS}.nebula_tendril"] = "Nebula Tendril"
    lang[f"block.{NS}.nebula_tendril_plant"] = "Nebula Tendril Plant"

    # Nullwood leaves: like vanilla leaves (shears or Silk Touch keep them; otherwise saplings and sticks).
    blockstate("nullwood_leaves", {"variants": {"": {"model": rid("block/nullwood_leaves")}}})
    block_model("nullwood_leaves", {"parent": "minecraft:block/leaves", "textures": {"all": rid("block/nullwood_leaves")}})
    item_def("nullwood_leaves", rid("block/nullwood_leaves"))
    lang[f"block.{NS}.nullwood_leaves"] = "Nullwood Leaves"
    keep = {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}
    loot("nullwood_leaves", [
        {"entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": keep, "name": rid("nullwood_leaves")},
            {"type": "minecraft:item", "condition": {"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:survives_explosion"},
                {"type": "minecraft:table_bonus", "chances": [0.05, 0.0625, 0.083333336, 0.1], "enchantment": "minecraft:fortune"}]},
             "name": rid("nullwood_sapling")}]}], "rolls": 1},
        {"condition": {"type": "minecraft:inverted", "term": keep}, "entries": [{
            "type": "minecraft:item",
            "condition": {"type": "minecraft:table_bonus", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1],
                          "enchantment": "minecraft:fortune"},
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "max": 2, "min": 1}},
                         {"type": "minecraft:explosion_decay"}],
            "name": "minecraft:stick"}], "rolls": 1},
    ])
    for t in ("block", "item"):
        tag(t, "minecraft:leaves", rid("nullwood_leaves"))
        tag(t, "minecraft:saplings", rid("nullwood_sapling"))
    tag("block", "minecraft:mineable/hoe", rid("nullwood_leaves"))
    # Null Lantern: a glowing fruit on a short stem, hanging under Nullwood leaves.
    blockstate("null_lantern", {"variants": {"": {"model": rid("block/null_lantern")}}})
    block_model("null_lantern", {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {"particle": rid("block/null_lantern_side"), "side": rid("block/null_lantern_side"),
                     "end": rid("block/null_lantern_end"), "stem": rid("block/null_lantern_stem")},
        "elements": [
            {"from": [4, 2, 4], "to": [12, 10, 12], "faces": {
                "down": {"uv": [4, 4, 12, 12], "texture": "#end"}, "up": {"uv": [4, 4, 12, 12], "texture": "#end"},
                "north": {"uv": [4, 6, 12, 14], "texture": "#side"}, "south": {"uv": [4, 6, 12, 14], "texture": "#side"},
                "west": {"uv": [4, 6, 12, 14], "texture": "#side"}, "east": {"uv": [4, 6, 12, 14], "texture": "#side"}}},
            {"from": [8, 10, 0], "to": [8, 16, 16], "shade": False,
             "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45},
             "faces": {"west": {"uv": [0, 0, 16, 6], "texture": "#stem"}, "east": {"uv": [0, 0, 16, 6], "texture": "#stem"}}},
            {"from": [0, 10, 8], "to": [16, 16, 8], "shade": False,
             "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45},
             "faces": {"north": {"uv": [0, 0, 16, 6], "texture": "#stem"}, "south": {"uv": [0, 0, 16, 6], "texture": "#stem"}}},
        ],
    })
    item_def("null_lantern", rid("block/null_lantern"))
    lang[f"block.{NS}.null_lantern"] = "Null Lantern"
    drop_self("null_lantern")
    tag("block", "minecraft:mineable/hoe", rid("null_lantern"))
    tag("block", "minecraft:mineable/axe", rid("lumine_cap"))
    tag("block", "minecraft:mineable/pickaxe", rid("petrified_stalk"))

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
    for name in ("chiseled_nightstone", "void_crystal_block", "meteoric_iron_block",
                 "raw_meteoric_iron_block", "stardust_block", "chorus_moss", "lumineshroom", "starbloom",
                 "pale_end_stone_tiles", "solarite_block", "raw_solarite_block", "star_inlay_observatory_quartz",
                 "ashen_lumineshroom", "petrified_astral_vine", "lumine_cap", "nullwood_sapling"):
        drop_self(name)
    # Like vanilla grass: nothing without Silk Touch / shears.
    for name, tool in (("upwell_core", "minecraft:tool/can_silk_touch"),
                       ("shimmer_grass", "minecraft:tool/can_shear"), ("nebula_tufts", "minecraft:tool/can_shear")):
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
    for name in ("chiseled_nightstone", "void_crystal_ore", "void_crystal_block", "meteoric_iron_ore",
                 "meteoric_iron_block", "raw_meteoric_iron_block"):
        tag("block", "minecraft:mineable/pickaxe", rid(name))
    for name in ("void_crystal_ore", "void_crystal_block", "meteoric_iron_block", "raw_meteoric_iron_block"):
        tag("block", "minecraft:needs_iron_tool", rid(name))
    tag("block", "minecraft:needs_diamond_tool", rid("meteoric_iron_ore"))
    for name in ("pale_end_stone_tiles", "solarite_ore", "solarite_block", "raw_solarite_block",
                 "star_inlay_observatory_quartz", "upwell_core"):
        tag("block", "minecraft:mineable/pickaxe", rid(name))
    for name in ("solarite_ore", "upwell_core"):
        tag("block", "minecraft:needs_diamond_tool", rid(name))
    for name in ("solarite_block", "raw_solarite_block"):
        tag("block", "minecraft:needs_iron_tool", rid(name))
    tag("block", "minecraft:mineable/hoe", rid("sunscale_lichen"))
    tag("block", "minecraft:replaceable_by_trees", rid("shimmer_grass"))
    tag("block", "minecraft:beacon_base_blocks", rid("solarite_block"))
    tag("block", "minecraft:beacon_base_blocks", rid("meteoric_iron_block"))
    # Minecraft 26.3 decides which blocks "block motion" (heightmaps, so world generation and mob
    # spawning; suffocation; fluid flow...) with the block tag #minecraft:blocks_motion_no_leaves,
    # not from the block's shape. The other motion tags include it. Every solid block of the mod
    # belongs in it, like vanilla's stone, glass, stairs, slabs and walls; plants, vines, carpets
    # and crops don't (vanilla leaves out grass and moss carpet too).
    not_solid = {"lumineshroom", "starbloom", "astral_vine", "astral_vine_plant", "comet_cress", "shimmer_grass",
                 "flarebloom", "sunscale_lichen", "ashen_lumineshroom", "petrified_astral_vine", "nebula_tufts",
                 "nebula_tendril", "nebula_tendril_plant", "nullwood_leaves", "nullwood_sapling", "shimmer_reed",
                 "null_lantern", "chorus_bloom", "chorus_sprout", "glimmer_cluster", "end_chimes"}
    solid = sorted(set(re.findall(r'\b(?:register|registerNoItem|stairs|slab|wall)\("([a-z0-9_]+)"',
                                  (ROOT / "src/main/java/com/ggrantrichards/astralend/ModBlocks.java").read_text()))
                   - not_solid - {"name"})
    solid = [b for b in solid if not b.endswith("_")]  # "stripped_" + name, in the wood-set helper
    for n, log_word, wood_word, *_ in WOOD_SETS:
        solid += [f"{n}_{log_word}", f"{n}_{wood_word}", f"stripped_{n}_{log_word}", f"stripped_{n}_{wood_word}",
                  f"{n}_planks", f"{n}_stairs", f"{n}_slab", f"{n}_fence", f"{n}_fence_gate", f"{n}_door", f"{n}_trapdoor"]
    for name in solid:
        tag("block", "minecraft:blocks_motion_no_leaves", rid(name))

    # What the End's plants grow on.
    for name in ("minecraft:end_stone", rid("chorus_moss"), rid("nightstone"), rid("pale_end_stone"),
                 rid("charred_end_stone"), rid("sunscorched_stone")):
        tag("block", f"{NS}:astral_soil", name)
    tag("block", "minecraft:mineable/shovel", rid("stardust_block"))
    tag("block", "minecraft:mineable/hoe", rid("chorus_moss"))
    tag("block", "minecraft:small_flowers", rid("starbloom"))
    tag("item", "minecraft:small_flowers", rid("starbloom"))
    tag("block", "minecraft:crops", rid("comet_cress"))

    # ---------------------------------------------------------------- recipes
    shaped("polished_nightstone", "polished_nightstone", ["SS", "SS"], {"S": rid("nightstone")}, count=4)
    shaped("nightstone_bricks", "nightstone_bricks", ["##", "##"], {"#": rid("polished_nightstone")}, count=4)
    shaped("chiseled_nightstone", "chiseled_nightstone", ["#", "#"], {"#": rid("nightstone_slab")})
    for base, (stairs, slab, wall) in family_variants.items():
        stonecutting(base, stairs)
        stonecutting(base, slab, 2)
        stonecutting(base, wall)
    for result in ("polished_nightstone", "nightstone_bricks", "chiseled_nightstone"):
        stonecutting("nightstone", result)
    for result, count in (("polished_nightstone_stairs", 1), ("polished_nightstone_slab", 2),
                          ("polished_nightstone_wall", 1), ("nightstone_brick_stairs", 1),
                          ("nightstone_brick_slab", 2), ("nightstone_brick_wall", 1)):
        stonecutting("nightstone", result, count)
    stonecutting("polished_nightstone", "nightstone_bricks")
    for result, count in (("nightstone_brick_stairs", 1), ("nightstone_brick_slab", 2), ("nightstone_brick_wall", 1)):
        stonecutting("polished_nightstone", result, count)

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
    mobs_phase4()
    flares_phase5()
    structures_phase6()
    starforging_phase8()
    round_out_phase9()
    advancements_phase9()
    mechanics_phase10()
    content_phase10()
    recipe_unlocks()      # last: it reads every recipe written above

    # ---------------------------------------------------------------- write tags + lang
    for (registry, name), values in tags.items():
        ns, path = name.split(":")
        folder = RES / "data" / ns / "tags" / registry
        write(folder / f"{path}.json", {"replace": False, "values": sorted(set(values))})
    write(ASSETS / "lang" / "en_us.json", dict(sorted(lang.items())))
    print(f"Wrote {len(written)} files")


if __name__ == "__main__":
    main()
