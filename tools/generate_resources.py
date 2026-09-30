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
]
PILLARS = [("leviathan_bone_block", "Leviathan Bone Block", "leviathan_bone")]
CROSS_PLANTS = [("lumineshroom", "Lumineshroom"), ("starbloom", "Starbloom")]
ITEMS = [
    ("void_crystal", "Void Crystal"),
    ("raw_meteoric_iron", "Raw Meteoric Iron"),
    ("meteoric_iron_ingot", "Meteoric Iron Ingot"),
    ("meteoric_iron_nugget", "Meteoric Iron Nugget"),
    ("comet_cress", "Comet Cress"),
]

PLURAL = {"umbral_stone_bricks": "umbral_stone_brick"}  # "bricks" -> "brick_stairs", like vanilla


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


def main():
    # Clear previously generated folders so renamed/removed content doesn't linger.
    for folder in (ASSETS / "blockstates", ASSETS / "models", ASSETS / "items", DATA / "loot_table",
                   DATA / "recipe", RES / "data" / NS / "tags", MC_DATA / "tags"):
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
                 "raw_meteoric_iron_block", "stardust_block", "nebula_moss", "lumineshroom", "starbloom"):
        drop_self(name)
    for ore, drop in (("void_crystal_ore", "void_crystal"), ("meteoric_iron_ore", "raw_meteoric_iron")):
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

    # ---------------------------------------------------------------- write tags + lang
    for (registry, name), values in tags.items():
        ns, path = name.split(":")
        folder = RES / "data" / ns / "tags" / registry
        write(folder / f"{path}.json", {"replace": False, "values": sorted(set(values))})
    write(ASSETS / "lang" / "en_us.json", dict(sorted(lang.items())))
    print(f"Wrote {len(written)} files")


if __name__ == "__main__":
    main()
