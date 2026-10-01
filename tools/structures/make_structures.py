#!/usr/bin/env python3
"""The Astral End's structures (Phase 6), built block by block in Python and saved as vanilla
structure templates (data/astralend/structure/*.nbt), with the worldgen data that places them:
a jigsaw structure, its template pool, a structure set (how often) and a biome tag (where).

    python3 tools/structures/make_structures.py            # write everything
    python3 tools/structures/make_structures.py --preview  # also draw docs/images/structures/*.png
    python3 tools/structures/make_structures.py --preview --cut  # and craters cut in half

tools/generate_resources.py calls write_data() too, because it rewrites the worldgen folder.
Every structure is built from a seed, so it comes out the same every time.
"""
import json
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from template import Template  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/astralend"
NS = "astralend"


def a(name):
    return f"{NS}:{name}"


def chest(t, x, y, z, loot, facing="north", kind="chest"):
    t.set(x, y, z, f"minecraft:{kind}[facing={facing}]" if kind == "chest" else f"minecraft:{kind}[facing=up]",
          nbt={"LootTable": a(f"chests/{loot}")})


# ------------------------------------------------------------------ Comet Crater

def comet_crater(seed, hot):
    """A bowl blasted into an island by a comet: a raised rim of Charred End Stone thrown up by the
    impact, a scorched floor glazed with Comet Ice, and the Comet Core (a boulder with a heart of
    Meteoric Iron Ore) half sunk at the bottom, with a cache buried under it. Stardust only lies
    under the floor, where it can't fall up. Hot craters still smoulder: magma in the floor's cracks
    and fires burning along the rim, and no ice."""
    t = Template(29, 14, 29, seed)
    rnd = t.rnd
    c = 14
    ground = 8                      # the island's surface, inside the template
    r_bowl, r_rim = 10.5, 13.0
    tops = {}
    for x in range(29):
        for z in range(29):
            d = math.hypot(x - c, z - c) + rnd.uniform(-0.45, 0.45)
            if d > r_rim:
                continue
            if d < r_bowl:
                top = ground - round(6.5 * (1 - (d / r_bowl) ** 2))
            else:
                top = ground + round(2.4 * math.sin((d - r_bowl) / (r_rim - r_bowl) * math.pi))
            tops[(x, z)] = (top, d)
            for y in range(max(0, top - 3), top + 1):   # a skin over the island; the world fills below
                state = a("charred_end_stone")
                if y == top and d < r_bowl:
                    roll = rnd.random()
                    if hot and roll < 0.16 and d < r_bowl - 1:
                        state = "minecraft:magma_block"
                    elif not hot and roll < 0.2 + 0.25 * (1 - d / r_bowl):
                        state = a("comet_ice")
                    elif roll > 0.94:
                        state = a("meteoric_iron_ore")
                elif y == top - 1 and d < r_bowl - 1 and rnd.random() < 0.45:
                    state = a("stardust_block")
                elif y < top - 1 and d < 6 and rnd.random() < 0.12:
                    state = a("meteoric_iron_ore")
                t.set(x, y, z, state)
            for y in range(top + 1, 14):
                t.set(x, y, z, "minecraft:air")
    # Hot craters: fire burns along the rim, kept alight by magma under it.
    if hot:
        for (x, z), (top, d) in tops.items():
            if r_bowl + 0.6 < d < r_rim - 0.6 and rnd.random() < 0.22:
                t.set(x, top, z, "minecraft:magma_block")
                t.set(x, top + 1, z, "minecraft:fire")
    floor = ground - 6
    # The Comet Core: a scorched boulder with a heart of Meteoric Iron, half sunk in the floor.
    for x in range(c - 3, c + 4):
        for y in range(floor - 1, floor + 5):
            for z in range(c - 3, c + 4):
                dist = math.sqrt((x - c) ** 2 + ((y - floor - 1.0) * 1.1) ** 2 + (z - c) ** 2)
                if dist <= 2.9 + rnd.uniform(-0.4, 0.2):
                    roll = rnd.random()
                    if dist < 1.4:
                        state = a("raw_meteoric_iron_block")
                    elif roll < 0.45:
                        state = a("meteoric_iron_ore")
                    elif roll < 0.6:
                        state = a("raw_meteoric_iron_block")
                    elif hot and roll < 0.8:
                        state = "minecraft:magma_block"
                    elif not hot and roll < 0.72:
                        state = a("comet_ice")
                    else:
                        state = a("charred_end_stone")
                    t.set(x, y, z, state)
    chest(t, c, floor - 2, c, "comet_crater")
    # Shards of the comet's ice thrown out of the impact, leaning away from it (cold craters only).
    if not hot:
        for i in range(6):
            ang = i / 6 * math.tau + rnd.uniform(-0.4, 0.4)
            dist = rnd.uniform(5.0, 8.5)
            x0, z0 = c + math.cos(ang) * dist, c + math.sin(ang) * dist
            bottom = ground - round(6.5 * (1 - (dist / r_bowl) ** 2)) + 1
            h = rnd.randint(2, 4)
            t.line((x0, bottom - 1, z0), (x0 + math.cos(ang) * h * 0.6, bottom + h, z0 + math.sin(ang) * h * 0.6),
                   a("comet_ice"))
    return t


# ------------------------------------------------------------------ Wanderer Relay

def wanderer_relay(seed):
    """A thin spire floating over the edge of the Dark Matter Belt, where Void Wanderers dock: a
    lump of Umbral Stone hanging in the air, a tapering tower of Umbral bricks banded with chiseled
    stone, two docking rings hung with Null Lanterns, and a Void Crystal light at the top. It holds
    no loot; Wanderers spawn around it."""
    t = Template(17, 44, 17, seed)
    rnd = t.rnd
    c = 8
    # The floating rock: an upside-down cone, rough at the edges.
    for y in range(0, 8):
        radius = 1.0 + (y / 7) ** 0.8 * 5.0
        for x in range(17):
            for z in range(17):
                d = math.hypot(x - c, z - c)
                if d <= radius + rnd.uniform(-0.5, 0.3):
                    state = a("umbral_stone")
                    if rnd.random() < 0.06:
                        state = a("void_crystal_ore")
                    t.set(x, y, z, state)
    for x in range(17):
        for z in range(17):
            if math.hypot(x - c, z - c) <= 5.2:
                t.set(x, 8, z, a("polished_umbral_stone"))
    # The spire.
    for y in range(9, 38):
        half = 1 if y < 30 else 0
        band = (y - 9) % 7 == 6
        for x in range(c - half, c + half + 1):
            for z in range(c - half, c + half + 1):
                t.set(x, y, z, a("chiseled_umbral_stone") if band else a("umbral_stone_bricks"))
    # Docking rings with spokes; lanterns hang under the ring at the four corners.
    for ring_y, radius in ((17, 5), (27, 4)):
        for i in range(64):
            ang = i / 64 * math.tau
            t.set(c + round(math.cos(ang) * radius), ring_y, c + round(math.sin(ang) * radius),
                  a("polished_umbral_stone_slab") + "[type=bottom]")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for k in range(2, radius):
                t.set(c + dx * k, ring_y, c + dz * k, a("polished_umbral_stone_slab") + "[type=bottom]")
            t.set(c + dx * radius, ring_y - 1, c + dz * radius, a("null_lantern"))
    # The light at the top, behind glass.
    t.set(c, 38, c, a("void_crystal_block"))
    t.set(c, 39, c, a("void_crystal_block"))
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        t.set(c + dx, 38, c + dz, a("astral_glass"))
        t.set(c + dx, 39, c + dz, a("astral_glass"))
    t.set(c, 40, c, a("chiseled_umbral_stone"))
    t.set(c, 41, c, a("null_lantern"))
    return t


# ------------------------------------------------------------------ Whalefall

def whalefall(seed):
    """The fossil of a Leviathan that fell here long ago: a spine of Leviathan Bone arched over the
    island, ribs curving down to the ground on both sides, a blunt skull at one end and a tapering
    tail at the other. Whalelight Scales still grow on the bones, and someone left a chest inside."""
    t = Template(50, 20, 19, seed)
    rnd = t.rnd
    cz = 9
    head, tail = 39, 3
    bone_x = a("leviathan_bone_block") + "[axis=x]"
    bone_y = a("leviathan_bone_block") + "[axis=y]"
    bone_z = a("leviathan_bone_block") + "[axis=z]"

    def spine_y(x):
        return 3 + round(10 * math.sin((x - tail) / (head - tail) * math.pi * 0.95 + 0.1))

    for x in range(tail, head + 1):
        t.set(x, spine_y(x), cz, bone_x)
        if (x - tail) % 3 == 0:
            t.set(x, spine_y(x) + 1, cz, bone_y)          # the knobs of the vertebrae
    # Ribs: arcs down both sides, longest in the middle of the body.
    for x in range(tail + 8, head - 6, 3):
        top = spine_y(x)
        width = 2 + round(6 * math.sin((x - tail) / (head - tail) * math.pi))
        for side in (-1, 1):
            prev = None
            snapped = 21 if rnd.random() < 0.6 else rnd.randint(8, 16)   # some ribs broke off long ago
            for s in range(0, snapped):
                ang = s / 20 * math.pi * 0.55
                z = cz + side * round(math.sin(ang) * width * 1.15)
                y = top - round((1 - math.cos(ang)) * top * 0.95)
                if y < 1:
                    break
                state = bone_y if s > 6 else bone_z
                t.set(x, y, z, state)
                prev = (x, y, z)
            if prev and rnd.random() < 0.6:
                t.set(prev[0], prev[1] + 1, prev[2] + side, a("whalelight_scale"))
    # The skull: long and blunt, with eye sockets, half buried, and the lower jaw fallen open beside it.
    for x in range(head - 2, head + 8):
        for y in range(0, 7):
            for z in range(cz - 4, cz + 5):
                dx, dy, dz = (x - head - 2.5) / 5.2, (y - 2.5) / 3.2, (z - cz) / (3.8 - max(0, x - head - 3) * 0.35)
                if dx * dx + dy * dy + dz * dz <= 1.0:
                    t.set(x, y, z, bone_x)
    for z in (cz - 2, cz + 2):
        t.set(head + 2, 4, z, "minecraft:air")
        t.set(head + 2, 3, z, "minecraft:air")
    for x in range(head + 4, head + 9):                          # the open mouth
        for z in range(cz - 2, cz + 3):
            t.set(x, 1, z, "minecraft:air")
    for x in range(head + 1, head + 10):                         # the lower jaw, lying on the ground
        for z in (cz - 3 + (x - head) // 4, cz + 3 - (x - head) // 4):
            t.set(x, 0, z, bone_x)
    # Scales on the spine.
    for x in range(tail + 4, head - 2, 4):
        if rnd.random() < 0.7:
            t.set(x, spine_y(x) + (2 if (x - tail) % 3 == 0 else 1), cz, a("whalelight_scale"))
    chest(t, 22, 1, cz, "whalefall", facing="south")
    return t


# ------------------------------------------------------------------ worldgen data

def structure_json(name, height, heightmap, spawn_overrides, adaptation, ground):
    """An astralend:island structure (it checks the ground first, see IslandStructure.java) wrapping the
    jigsaw that builds it. ground = (min_surface, max_surface, reach)."""
    biomes = f"#{NS}:has_structure/{name}"
    jigsaw = {
        "type": "minecraft:jigsaw",
        "biomes": biomes,
        "max_distance_from_center": 80,
        "size": 1,
        "spawn_overrides": {},
        "start_height": height,
        "start_pool": a(f"{name}/start"),
        "use_expansion_hack": False,
    }
    if heightmap:
        jigsaw["project_start_to_heightmap"] = heightmap
    obj = {
        "type": a("island"),
        "biomes": biomes,
        "step": "surface_structures",
        "spawn_overrides": spawn_overrides or {},
        "min_surface": ground[0],
        "max_surface": ground[1],
        "reach": ground[2],
        "structure": jigsaw,
    }
    if adaptation:
        obj["terrain_adaptation"] = adaptation
    return obj


# Each structure: its builder variants, biomes, spread (spacing, separation, salt), start height and
# heightmap, spawn overrides, terrain adaptation (beard_thin builds the island up under a piece, so a
# crater never overhangs an edge) and the ground it needs (min surface, max surface, reach).
STRUCTURES = [
    dict(name="comet_crater", variants=[lambda: comet_crater(11, False), lambda: comet_crater(12, False),
                                        lambda: comet_crater(13, True)],
         biomes=[a("comet_wake")], spread=(9, 3, 61829341), height={"absolute": -8}, heightmap="WORLD_SURFACE_WG",
         spawns=None, adaptation="beard_thin", ground=(16, 4096, 12)),
    # Relays float out over the void, so they want open space (or low ground) under them.
    dict(name="wanderer_relay", variants=[lambda: wanderer_relay(21)],
         biomes=[a("dark_matter_belt")], spread=(18, 7, 51273391),
         height={"type": "minecraft:uniform", "min_inclusive": {"absolute": 66}, "max_inclusive": {"absolute": 92}},
         heightmap=None, spawns={"creature": {"bounding_box": "piece", "spawns": [
             {"type": a("void_wanderer"), "weight": 1, "minCount": 1, "maxCount": 2}]}},
         adaptation=None, ground=(-4096, 40, 7)),
    dict(name="whalefall", variants=[lambda: whalefall(31)],
         biomes=[a("aurora_shallows")], spread=(36, 14, 70127811), height={"absolute": -1}, heightmap="WORLD_SURFACE_WG",
         spawns=None, adaptation=None, ground=(16, 4096, 14)),
]


def files_of(st):
    n = len(st["variants"])
    return [f"{st['name']}_{i + 1}" for i in range(n)] if n > 1 else [st["name"]]


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def write_templates(preview=False):
    out = DATA / "structure"
    out.mkdir(parents=True, exist_ok=True)
    made = []
    for st in STRUCTURES:
        for build, file in zip(st["variants"], files_of(st)):
            t = build()
            t.save(out / f"{file}.nbt")
            made.append(file)
            if preview:
                import preview as pv
                pv.save(t, file)
                if "--cut" in sys.argv and st["name"] == "comet_crater":
                    pv.save(t, file + "_cut", cut=("z", t.size[2] // 2))
    return made


def write_data():
    """The worldgen JSON (structure, pool, set, biome tag). Called by generate_resources.py too."""
    for st in STRUCTURES:
        name = st["name"]
        spacing, separation, salt = st["spread"]
        write_json(DATA / "worldgen/structure" / f"{name}.json",
                   structure_json(name, st["height"], st["heightmap"], st["spawns"], st["adaptation"], st["ground"]))
        write_json(DATA / "worldgen/template_pool" / name / "start.json", {
            "elements": [{"element": {"element_type": "minecraft:single_pool_element", "location": a(f),
                                      "processors": "minecraft:empty", "projection": "rigid"}, "weight": 1}
                         for f in files_of(st)],
            "fallback": "minecraft:empty",
        })
        write_json(DATA / "worldgen/structure_set" / f"{name}.json", {
            "placement": {"type": "minecraft:random_spread", "salt": salt, "separation": separation, "spacing": spacing},
            "structures": [{"structure": a(name), "weight": 1}],
        })
        write_json(DATA / "tags/worldgen/biome/has_structure" / f"{name}.json", {"values": st["biomes"]})


def main():
    made = write_templates(preview="--preview" in sys.argv)
    write_data()
    print(f"Wrote {len(made)} structure templates: {', '.join(made)}")


if __name__ == "__main__":
    main()
