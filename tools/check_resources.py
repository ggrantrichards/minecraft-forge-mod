#!/usr/bin/env python3
"""Checks that every block and item the Java code registers has everything it needs to
show up correctly in game, and that every JSON file only points at things that exist.

A missing or misspelled model/texture is what gives the purple-and-black checkerboard,
and a missing name shows up as "block.astralend.something". This catches both before
you ever launch the game.

Run from the project root:  python3 tools/check_resources.py   (exit code 1 on problems)
"""
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
    "minecraft:item/generated", "minecraft:item/handheld",
}

errors = []


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
blocks_with_item -= {"name"}
blocks = blocks_with_item | blocks_no_item
plain_items = set(re.findall(r'\b(?:simple|ITEMS\.register)\("([a-z0-9_]+)"', items_src))
items = blocks_with_item | plain_items

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
}

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
    if not loot.exists():
        err(f"block {NS}:{name}: missing loot table {loot.relative_to(ROOT)} (it would drop nothing)")

# ------------------------------------------------------------------ items

for name in sorted(items):
    idef = ASSETS / "items" / f"{name}.json"
    if not idef.exists():
        err(f"item {NS}:{name}: missing item definition {idef.relative_to(ROOT)} (would show as missing texture)")
    else:
        data = load(idef) or {}
        model = data.get("model", {})
        if model.get("type") != "minecraft:model" or "model" not in model:
            err(f"{idef.relative_to(ROOT)}: expected {{\"model\": {{\"type\": \"minecraft:model\", \"model\": ...}}}}")
        else:
            check_model(model["model"], str(idef.relative_to(ROOT)))
    key = f"block.{NS}.{name}" if name in blocks_with_item else f"item.{NS}.{name}"
    if key not in lang:
        err(f"item {NS}:{name}: missing English name {key}")

# ------------------------------------------------------------------ data files only reference real things

known = {f"{NS}:{n}" for n in blocks | items}
for file in sorted((RES / "data").rglob("*.json")):
    text = file.read_text()
    load(file)
    for ref in set(re.findall(rf'"({NS}:[a-z0-9_/]+)"', text)):
        if ref.startswith(f"{NS}:blocks/"):
            continue  # loot table random_sequence names
        if ref not in known:
            err(f"{file.relative_to(ROOT)}: refers to {ref}, which the Java code doesn't register")

for file in sorted(ASSETS.rglob("*.json")):
    load(file)
for extra in sorted(set(lang) - {f"block.{NS}.{n}" for n in blocks} - {f"item.{NS}.{n}" for n in items}
                    - {"itemGroup.astralend"}):
    err(f"lang/en_us.json: {extra} doesn't match anything registered")

if errors:
    print(f"{len(errors)} problem(s):")
    for e in errors:
        print("  -", e)
    sys.exit(1)
print(f"OK: {len(blocks)} blocks and {len(items)} items all have blockstates, models, textures, names and loot tables.")
