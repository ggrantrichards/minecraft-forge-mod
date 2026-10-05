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
from nbt import Byte, Int  # noqa: E402

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
    impact, a scorched floor glazed with blue ice, and the Comet Core (a boulder with a heart of
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
                        state = "minecraft:blue_ice"
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
                        state = "minecraft:blue_ice"
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
                   "minecraft:blue_ice")
    return t


# ------------------------------------------------------------------ Wanderer Relay

def wanderer_relay(seed):
    """A thin spire floating over the edge of the Hollow Isles, where Void Wanderers dock: a
    lump of Nightstone hanging in the air, a tapering tower of Umbral bricks banded with chiseled
    stone, two mooring rings hung with Null Lanterns, and a Void Crystal light at the top. It holds
    no loot, but Void Wanderers dock at it."""
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
                    state = a("nightstone")
                    if rnd.random() < 0.06:
                        state = a("void_crystal_ore")
                    t.set(x, y, z, state)
    for x in range(17):
        for z in range(17):
            if math.hypot(x - c, z - c) <= 5.2:
                t.set(x, 8, z, a("polished_nightstone"))
    # The spire.
    for y in range(9, 38):
        half = 1 if y < 30 else 0
        band = (y - 9) % 7 == 6
        for x in range(c - half, c + half + 1):
            for z in range(c - half, c + half + 1):
                t.set(x, y, z, a("chiseled_nightstone") if band else a("nightstone_bricks"))
    # Mooring rings with spokes; lanterns hang under the ring at the four corners.
    for ring_y, radius in ((17, 5), (27, 4)):
        for i in range(64):
            ang = i / 64 * math.tau
            t.set(c + round(math.cos(ang) * radius), ring_y, c + round(math.sin(ang) * radius),
                  a("polished_nightstone_slab") + "[type=bottom]")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for k in range(2, radius):
                t.set(c + dx * k, ring_y, c + dz * k, a("polished_nightstone_slab") + "[type=bottom]")
            t.set(c + dx * radius, ring_y - 1, c + dz * radius, a("null_lantern"))
    # The light at the top, behind glass.
    t.set(c, 38, c, a("void_crystal_block"))
    t.set(c, 39, c, a("void_crystal_block"))
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        t.set(c + dx, 38, c + dz, a("astral_glass"))
        t.set(c + dx, 39, c + dz, a("astral_glass"))
    t.set(c, 40, c, a("chiseled_nightstone"))
    t.set(c, 41, c, a("null_lantern"))
    # Two Void Wanderers docked at the lower ring.
    for dx, dz in ((3, 0), (-3, 0)):
        t.entity(c + dx + 0.5, 17.5, c + dz + 0.5, {"id": a("void_wanderer"), "PersistenceRequired": Byte(1)})
    return t


# ------------------------------------------------------------------ Whalefall

def vault(t, x, y, z, loot, facing):
    """A vanilla vault that opens for a Chart Fragment, like a trial key."""
    t.set(x, y, z, f"minecraft:vault[facing={facing},ominous=false,vault_state=inactive]", nbt={
        "config": {"loot_table": a(f"chests/{loot}"), "key_item": {"id": a("chart_fragment"), "count": Int(1)}}})


# Constellations laid into the observatory floor (offsets from the centre, joined in order).
FLOOR_CONSTELLATIONS = [
    [(-9, -3), (-7, -6), (-4, -8), (-1, -9)],
    [(4, -8), (7, -6), (8, -2), (6, -4), (4, -8)],
    [(-8, 4), (-6, 7), (-2, 9), (1, 7)],
    [(5, 5), (8, 3), (9, 6), (6, 8)],
]


class Shifted:
    """Draws onto part of a bigger template, as if it were its own (x and z moved by dx, dz)."""
    def __init__(self, t, dx, dz):
        self.t, self.dx, self.dz, self.rnd = t, dx, dz, t.rnd

    def set(self, x, y, z, state, nbt=None):
        self.t.set(x + self.dx, y, z + self.dz, state, nbt=nbt)

    def get(self, x, y, z):
        return self.t.get(x + self.dx, y, z + self.dz)

    def fill(self, x0, y0, z0, x1, y1, z1, state):
        self.t.fill(x0 + self.dx, y0, z0 + self.dz, x1 + self.dx, y1, z1 + self.dz, state)

    def line(self, a, b, state, thickness=0.0):
        self.t.line((a[0] + self.dx, a[1], a[2] + self.dz), (b[0] + self.dx, b[1], b[2] + self.dz), state, thickness)


def shattered_observatory(seed):
    """The Shattered Observatory (a biome until Phase 10; a landmark structure now): the Cartographers'
    domed hall in the middle of a great ring of quartz pillars, most of them fallen, with Starblooms
    and Astral Vines grown over the ground between."""
    size, c, floor = 61, 30, 2
    t = Template(size, 28, size, seed)
    rnd = t.rnd
    q, pillar = a("observatory_quartz"), a("observatory_quartz_pillar")
    bricks = a("pale_end_stone_bricks")
    # The ground: a disc of End Stone, its edge ragged, so the island under it is levelled.
    for x in range(size):
        for z in range(size):
            d = math.hypot(x - c, z - c)
            if d > 29.5 - rnd.random() * 2.5:
                continue
            for y in range(0, floor + 1):
                t.set(x, y, z, "minecraft:end_stone")
            for y in range(floor + 1, 28):
                t.set(x, y, z, "minecraft:air")
            # Overgrown: Chorus Moss in drifts, Starblooms on it, now and then an Astral Vine.
            if 14.5 < d < 28 and rnd.random() < 0.55 + 0.35 * math.sin(x * 0.4) * math.cos(z * 0.35):
                t.set(x, floor, z, a("chorus_moss"))
                roll = rnd.random()
                if roll < 0.22:
                    t.set(x, floor + 1, z, a("starbloom"))
                elif roll < 0.25:
                    tall = rnd.randint(1, 4)
                    for y in range(floor + 1, floor + tall):
                        t.set(x, y, z, a("astral_vine_plant"))
                    t.set(x, floor + tall, z, a("astral_vine") + f"[age={rnd.randint(17, 25)}]")
    # The great ring: sixteen pillars joined by lintels, a few still standing.
    R, count, tall = 25, 16, 10
    bases, heights = [], []
    for k in range(count):
        ang = k / count * math.tau + 0.1
        x, z = round(c + math.cos(ang) * R), round(c + math.sin(ang) * R)
        roll = rnd.random()
        h = tall if roll < 0.4 else rnd.randint(2, tall - 2) if roll < 0.8 else 1
        t.fill(x - 1, floor, z - 1, x + 1, floor, z + 1, bricks)
        for y in range(floor + 1, floor + 1 + h):
            t.set(x, y, z, pillar + "[axis=y]")
        if h < tall and rnd.random() < 0.6:
            # The fallen top lies beside it, pointing away from the ring.
            dx, dz = (1, 0) if abs(math.cos(ang)) > abs(math.sin(ang)) else (0, 1)
            sign = 1 if (math.cos(ang) if dx else math.sin(ang)) > 0 else -1
            axis = "x" if dx else "z"
            for i in range(2, 2 + min(4, tall - h)):
                t.set(x + dx * sign * i, floor + 1, z + dz * sign * i, pillar + f"[axis={axis}]")
        bases.append((x, z))
        heights.append(h)
    for k in range(count):
        j = (k + 1) % count
        if heights[k] == tall and heights[j] == tall:
            (x0, z0), (x1, z1) = bases[k], bases[j]
            t.line((x0, floor + tall + 1, z0), (x1, floor + tall + 1, z1), q)
    # Shulkers keep watch from the tops of the pillars still standing (up to three of them).
    watch = [k for k in range(count) if heights[k] == tall][:3]
    for k in watch:
        x, z = bases[k]
        top = floor + tall + 1
        if t.get(x, top, z) is None:
            t.set(x, top, z, q)            # a capstone to perch on
        t.entity(x + 0.5, top + 1, z + 0.5, {"id": "minecraft:shulker", "AttachFace": Byte(0),
                                             "PersistenceRequired": Byte(1)})
    # A path of quartz from the ring to the hall's broken east side.
    for x in range(c + 14, c + R):
        for z in (c - 1, c, c + 1):
            if rnd.random() < 0.8:
                t.set(x, floor, z, a("observatory_quartz_slab") + "[type=top]" if rnd.random() < 0.3 else q)
    observatory_hall(Shifted(t, c - 16, c - 16))
    return t


def observatory_hall(t):
    """The Cartographers' observatory, long since shattered: a round hall of pale stone ringed with
    quartz pillars, its dome broken open on one side, a star map laid into the floor, and the great
    telescope still on its mount, aimed through the hole at the sky. Two vaults at the foot of the
    mount open for Chart Fragments. Drawn in a 33x26x33 space."""
    rnd = t.rnd
    c, floor = 16, 2
    R = 13
    q, pillar = a("observatory_quartz"), a("observatory_quartz_pillar")
    bricks, tiles = a("pale_end_stone_bricks"), a("pale_end_stone_tiles")
    broken_toward = 0.0                         # the dome is open to the east (+x), where the telescope points

    def angle_gap(ang):
        return abs((ang - broken_toward + math.pi) % math.tau - math.pi)

    # Foundation and floor.
    for x in range(33):
        for z in range(33):
            d = math.hypot(x - c, z - c)
            if d > R + 0.5:
                continue
            for y in range(0, floor):
                t.set(x, y, z, bricks)
            t.set(x, floor, z, tiles if d < R - 0.5 else bricks)
            for y in range(floor + 1, 26):
                t.set(x, y, z, "minecraft:air")
            if 9.3 < d < 10.3:
                t.set(x, floor, z, q)                # the ring around the star map
    # The star map.
    for cons in FLOOR_CONSTELLATIONS:
        for (x0, z0), (x1, z1) in zip(cons, cons[1:]):
            t.line((c + x0, floor, c + z0), (c + x1, floor, c + z1), q)
        for x0, z0 in cons:
            t.set(c + x0, floor, c + z0, a("star_inlay_observatory_quartz"))
    # Walls with pillars and tall windows; lower and gapped where the dome came down.
    for i in range(160):
        ang = i / 160 * math.tau
        x, z = round(c + math.cos(ang) * R), round(c + math.sin(ang) * R)
        gap = angle_gap(ang)
        height = 7 if gap > 1.0 else max(1, round(7 * (gap / 1.0) ** 1.5) + rnd.randint(-1, 1))
        for y in range(floor + 1, floor + 1 + height):
            window = y in (floor + 3, floor + 4, floor + 5) and (i % 20) in range(7, 13)
            t.set(x, y, z, a("astral_glass") if window else bricks)
    for k in range(12):
        ang = k / 12 * math.tau
        x, z = round(c + math.cos(ang) * R), round(c + math.sin(ang) * R)
        gap = angle_gap(ang)
        top = floor + 8 if gap > 1.0 else floor + max(2, round(8 * gap))
        for y in range(floor + 1, top + 1):
            t.set(x, y, z, pillar + "[axis=y]")
        if gap > 1.0:
            t.set(x, top + 1, z, q)
            hx, hz = round(c + math.cos(ang) * (R - 1)), round(c + math.sin(ang) * (R - 1))
            t.set(hx, top, hz, a("null_lantern"))
    # The dome: a shell of quartz with glass between the ribs, broken open to the east.
    cy = floor + 8
    for x in range(33):
        for y in range(cy, 26):
            for z in range(33):
                d = math.sqrt((x - c) ** 2 + (y - cy) ** 2 * 1.15 + (z - c) ** 2)
                if not (R - 0.6 <= d <= R + 0.4):
                    continue
                ang = math.atan2(z - c, x - c)
                gap = angle_gap(ang)
                if gap < 1.25 + rnd.uniform(-0.25, 0.25) or rnd.random() < 0.07:
                    continue
                rib = min(abs((ang / (math.pi / 4)) - round(ang / (math.pi / 4))), 1) < 0.09 or y == cy
                t.set(x, y, z, q if rib or y > cy + 9 else a("astral_glass"))
    # Rubble from the dome, heaped on the floor on the broken side.
    for i in range(70):
        ang = broken_toward + rnd.uniform(-1.1, 1.1)
        dist = rnd.uniform(5, R - 1)
        x, z = round(c + math.cos(ang) * dist), round(c + math.sin(ang) * dist)
        if math.hypot(x - c, z - c) < 4:
            continue
        y = floor + 1
        while t.get(x, y, z) not in (None, "minecraft:air") and y < floor + 4:
            y += 1
        choice = rnd.random()
        state = q if choice < 0.4 else (a("observatory_quartz_slab") + "[type=bottom]" if choice < 0.7
                                        else a("astral_glass") if choice < 0.8 else bricks)
        t.set(x, y, z, state)
    # The telescope: a squat mount, and a long tube of Meteoric Iron aimed through the gap.
    t.fill(c - 1, floor + 1, c - 1, c + 1, floor + 1, c + 1, a("polished_nightstone"))
    for y in range(floor + 2, floor + 5):
        t.set(c, y, c, pillar + "[axis=y]")
    t.set(c, floor + 5, c, a("chiseled_nightstone"))
    lo, hi = (c - 4, floor + 4, c), (c + 6, floor + 12, c)
    t.line(lo, hi, a("meteoric_iron_block"), thickness=1.0)
    t.line((c + 6, floor + 12, c), (c + 7, floor + 13, c), a("astral_glass"), thickness=1.0)   # the lens
    t.set(c - 5, floor + 3, c, a("polished_nightstone"))                                    # the eyepiece
    t.set(c + 1, floor + 6, c, a("meteoric_iron_block"))
    # The vaults, either side of the mount, and lecterns for the star charts that were kept here.
    vault(t, c, floor + 1, c - 3, "observatory_vault", "north")
    vault(t, c, floor + 1, c + 3, "observatory_vault", "south")
    chest(t, c - 3, floor + 1, c - 3, "observatory_ruins", facing="west")


# ------------------------------------------------------------------ Solar Array Ruins

def mirror(t, x, ground, z, facing_angle, state, rnd):
    """One Cartographer mirror: a post, and a 3x3 dish of Solarite leaning back so it faces up and
    toward the array's centre (turned to the nearest compass direction, so the panel stays clean).
    state is "whole", "cracked" (a corner broken off), "fallen" (the dish lying by its post) or "stump"."""
    post = a("sunscorched_stone_brick_wall")
    height = 3 if state != "stump" else rnd.randint(1, 2)
    for y in range(ground + 1, ground + 1 + height):
        t.set(x, y, z, post)
    if state == "stump":
        return
    fx, fz = round(math.cos(facing_angle)), round(math.sin(facing_angle))
    if fx and fz:
        fz = 0                                          # snap diagonals to one axis
    wx, wz = -fz, fx                                    # across the dish
    panel = a("solarite_block")
    top = ground + 1 + height
    if state == "fallen":
        for w in (-1, 0, 1):
            for back in (1, 2, 3):
                if rnd.random() < 0.8:
                    t.set(x + wx * w - fx * back, ground + 1, z + wz * w - fz * back, panel)
        return
    t.set(x, top, z, a("polished_sunscorched_stone"))                       # the mount
    for row in range(3):
        lean = 1 - row                                   # bottom row toward the centre, top row back
        for w in (-1, 0, 1):
            if state == "cracked" and (row, w) in ((2, 1), (2, 0), (1, 1)):
                continue
            t.set(x + wx * w + fx * lean, top + 1 + row, z + wz * w + fz * lean, panel)


def solar_array(seed):
    """The Solar Array: a field of mirrors that once gathered flare light, most of them cracked or
    fallen, raised high on a stepped crimson mesa toward the sun, round a plaza ringed with lava and
    braziers, over a chamber where a Bottled Flare still sits in its pedestal. The mirrors still focus
    the flares: Sunburn is doubled on the open field."""
    S = 49
    t = Template(S, 30, S, seed)
    rnd = t.rnd
    c, ground, base = S // 2, 20, 8
    o = ground - 8               # (the chamber's heights, below, were drawn for a ground of 8)
    # The mesa: solid Sunscorched Stone Bricks from the island up to the plaza, stepped in every four
    # blocks toward the top, a stairway cut into its south-east face.
    for y in range(base, ground):
        r = 22 + (ground - 1 - y) // 4
        for x in range(S):
            for z in range(S):
                if math.hypot(x - c, z - c) <= r + 0.5:
                    t.set(x, y, z, a("sunscorched_stone_bricks"))
    for i in range(ground - base):
        y, x = ground - 1 - i, c + 3 + i
        for z in (c + 20, c + 21):
            t.set(x, y, z, a("polished_sunscorched_stone_stairs") + "[facing=west,half=bottom,shape=straight]")
            for above in range(y + 1, ground + 3):
                t.set(x, above, z, "minecraft:air")
    # Paving: a cracked plaza around the room and four causeways out to the edge.
    for x in range(S):
        for z in range(S):
            d = math.hypot(x - c, z - c)
            on_path = (abs(x - c) <= 1 or abs(z - c) <= 1) and d < S / 2
            if (d < 8.5 or on_path) and rnd.random() < 0.86:
                t.set(x, ground, z, a("polished_sunscorched_stone") if d < 8.5 else a("sunscorched_stone_bricks"))
    # The mirrors, on two rings around the centre, all turned to face it.
    for ring, count in ((14, 10), (21, 16)):
        for i in range(count):
            ang = i / count * math.tau + (0.5 / count * math.tau if ring == 21 else 0) + rnd.uniform(-0.06, 0.06)
            x, z = round(c + math.cos(ang) * ring), round(c + math.sin(ang) * ring)
            if abs(x - c) <= 2 or abs(z - c) <= 2:
                continue                                            # keep the causeways clear
            roll = rnd.random()
            state = "whole" if roll < 0.3 else "cracked" if roll < 0.6 else "fallen" if roll < 0.82 else "stump"
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    t.set(x + dx, ground, z + dz, a("sunscorched_stone_bricks"))
            mirror(t, x, ground, z, ang + math.pi, state, rnd)
    # The sunken chamber, 11 x 11, its roof level with the plaza and a glass skylight in it.
    lo, hi = c - 5, c + 5
    t.fill(lo, o, lo, hi, ground, hi, a("sunscorched_stone_bricks"))
    t.fill(lo + 1, o + 1, lo + 1, hi - 1, o + 1, hi - 1, a("polished_sunscorched_stone"))
    t.fill(lo + 1, o + 2, lo + 1, hi - 1, ground - 1, hi - 1, "minecraft:air")
    t.fill(lo, ground, lo, hi, ground, hi, a("polished_sunscorched_stone"))
    t.fill(c - 1, ground, c - 1, c + 1, ground, c + 1, a("astral_glass"))
    for x, z in ((lo + 1, lo + 1), (lo + 1, hi - 1), (hi - 1, lo + 1), (hi - 1, hi - 1)):
        for y in range(o + 2, ground):
            t.set(x, y, z, a("sunscorched_stone_brick_wall"))
        t.set(x, ground - 1, z, a("null_lantern"))
    # A stairway down from the south causeway.
    for step in range(6):
        z = hi + 1 + (5 - step)
        y = ground - step
        for x in (c - 1, c, c + 1):
            t.set(x, y, z, a("polished_sunscorched_stone_stairs") + "[facing=north,half=bottom,shape=straight]")
            for above in range(y + 1, ground + 4):
                t.set(x, above, z, "minecraft:air")
            t.set(x, y - 1, z, a("sunscorched_stone_bricks"))
        for x in (c - 2, c + 2):
            for yy in range(y - 1, ground + 1):
                t.set(x, yy, z, a("sunscorched_stone_bricks"))
    for x in (c - 1, c, c + 1):
        for y in range(o + 2, o + 5):
            t.set(x, y, hi, "minecraft:air")                         # the doorway
    # The console: a ring of pedestals, emptied long ago, around the one that still holds a cell.
    t.set(c, o + 2, c, a("collector_pedestal") + "[holding=bottled_flare]",
          nbt={"held": {"id": a("bottled_flare"), "count": Int(1)}, "charge": Int(0)})
    for dx, dz in ((-2, 0), (2, 0), (0, -2)):
        t.set(c + dx, o + 2, c + dz, a("collector_pedestal") + "[holding=none]")
    chest(t, lo + 2, o + 2, lo + 1, "solar_array", facing="south")
    chest(t, hi - 2, o + 2, lo + 1, "solar_array", facing="south")
    # A ring of lava round the plaza (bridged by the causeways, walled in), and four braziers burning
    # between the mirror rings.
    for x in range(S):
        for z in range(S):
            d = math.hypot(x - c, z - c)
            on_path = abs(x - c) <= 1 or abs(z - c) <= 1
            if 8.5 <= d < 9.5:
                t.set(x, ground, z, a("polished_sunscorched_stone"))
            elif 9.5 <= d < 11.0:
                t.set(x, ground, z, a("sunscorched_stone_bricks") if on_path else "minecraft:lava")
                t.set(x, ground - 1, z, a("sunscorched_stone_bricks"))
            elif 11.0 <= d < 12.0 and not on_path:
                t.set(x, ground, z, a("sunscorched_stone_bricks"))
                t.set(x, ground + 1, z, a("sunscorched_stone_brick_wall"))
    for ang in (math.pi / 4, 3 * math.pi / 4, 5 * math.pi / 4, 7 * math.pi / 4):
        x, z = round(c + math.cos(ang) * 17.5), round(c + math.sin(ang) * 17.5)
        t.set(x, ground, z, "minecraft:magma_block")
        t.set(x, ground + 1, z, "minecraft:campfire[lit=true,signal_fire=false,facing=north,waterlogged=false]")
    return t


# ------------------------------------------------------------------ Rift Maw

def bone(nx, ny, nz):
    """Leviathan Bone for a skull surface facing (nx, ny, nz): the grain runs along the skull, so its
    top and sides show the long grain and only the front and back show the ring of the bone's end."""
    return a("leviathan_bone_block") + ("[axis=z]" if abs(nx) > max(abs(ny), abs(nz)) else "[axis=x]")


# ------------------------------------------------------------------ Constellation Maze

def maze_platform(t, cx, y, cz, radius, rnd):
    """A broken platform floating in the void: polished stone on a rough cone of Nightstone."""
    for x in range(int(cx - radius - 1), int(cx + radius + 2)):
        for z in range(int(cz - radius - 1), int(cz + radius + 2)):
            d = math.hypot(x - cx, z - cz)
            if d > radius + 0.4:
                continue
            edge = d > radius - 0.9
            if edge and rnd.random() < 0.25:
                continue                                    # chipped edge
            t.set(x, y, z, a("nightstone_bricks") if edge else a("polished_nightstone"))
            for dy in range(1, 5):
                if d <= radius * (1 - dy / 4.5) + rnd.uniform(-0.4, 0.3):
                    t.set(x, y - dy, z, a("nightstone"))


def bridge(t, a0, a1, bend, constellation):
    """A star-bridge two blocks wide from a0 to a1, bending sideways by `bend` blocks halfway."""
    mx, my, mz = [(a0[i] + a1[i]) / 2 for i in range(3)]
    dx, dz = a1[0] - a0[0], a1[2] - a0[2]
    length = math.hypot(dx, dz) or 1.0
    px, pz = -dz / length, dx / length
    mid = (mx + px * bend, my, mz + pz * bend)
    state = a("star_bridge") + f"[constellation={constellation},lit=false]"
    for p, q in ((a0, mid), (mid, a1)):
        steps = int(max(abs(q[0] - p[0]), abs(q[2] - p[2])) * 2) + 1
        for s in range(steps + 1):
            f = s / steps
            x, y, z = p[0] + (q[0] - p[0]) * f, p[1] + (q[1] - p[1]) * f, p[2] + (q[2] - p[2]) * f
            for ox, oz in ((0, 0), (1, 0), (0, 1), (1, 1)):
                if t.get(round(x) + ox, math.floor(y), round(z) + oz) in (None, "minecraft:air"):
                    t.set(round(x) + ox, math.floor(y), round(z) + oz, state)


def constellation_maze(seed):
    """A ring of broken platforms floating in open void around a central dais, joined by star-bridges
    that are invisible until lit. Each platform's way on answers to one constellation: find it in
    the sky through a spyglass and the bridge to the next platform lights for 20 seconds (a barrel on
    the first platform holds a spyglass). The last bridge leads to the
    dais and its chest. Fall, and the maze catches you (MazeScoping)."""
    S = 49
    t = Template(S, 44, S, seed)
    rnd = t.rnd
    c = S // 2
    stops = [(5, 28, c), (c, 30, 5), (S - 6, 32, c), (c, 34, S - 6)]
    for i, (x, y, z) in enumerate(stops):
        maze_platform(t, x, y, z, 3.2, rnd)
        t.set(x, y, z, a("star_inlay_observatory_quartz"))      # where to stand and look up
        lx, lz = (x - 2, z + 2) if i % 2 == 0 else (x + 2, z - 2)
        t.set(lx, y + 1, lz, a("polished_nightstone_wall"))
        t.set(lx, y + 2, lz, a("null_lantern"))
        if i == 0:
            # A spyglass left at the start for whoever came without one.
            bx, bz = (x - 2, z - 2)
            t.set(bx, y + 1, bz, "minecraft:barrel[facing=up,open=false]", nbt={
                "Items": nbt_list_compound({"Slot": Byte(13), "id": "minecraft:spyglass", "count": Int(1)})})
    # The dais.
    dy = 36
    maze_platform(t, c, dy, c, 5.4, rnd)
    for x in range(c - 5, c + 6):
        for z in range(c - 5, c + 6):
            d = math.hypot(x - c, z - c)
            if d <= 4.6:
                t.set(x, dy, z, a("star_inlay_observatory_quartz") if 2.6 < d < 3.4 and (x + z) % 2 == 0 else a("observatory_quartz"))
    for ox, oz in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        for k in range(1, 4 if rnd.random() < 0.7 else 2):
            t.set(c + ox, dy + k, c + oz, a("observatory_quartz_pillar") + "[axis=y]")
    t.set(c - 3, dy + 4, c - 3, a("null_lantern"))
    t.set(c + 3, dy + 4, c + 3, a("null_lantern"))
    chest(t, c, dy + 1, c, "constellation_maze", facing="west")
    # The bridges, each answering to one constellation (the one sighted from the platform it starts at).
    ends = stops + [(c, dy, c)]
    for i in range(4):
        (x0, y0, z0), (x1, y1, z1) = ends[i], ends[i + 1]
        r0 = 3.6
        r1 = 5.8 if i == 3 else 3.6
        d = math.hypot(x1 - x0, z1 - z0)
        ux, uz = (x1 - x0) / d, (z1 - z0) / d
        bridge(t, (x0 + ux * r0, y0, z0 + uz * r0), (x1 - ux * r1, y1, z1 - uz * r1), 5 if i % 2 == 0 else -5, i)
    return t


def nbt_list_compound(*values):
    import nbt as _nbt
    return _nbt.List(list(values), _nbt.COMPOUND)


def nbt_list_float(*values):
    import nbt as _nbt
    return _nbt.List([_nbt.Float(v) for v in values], _nbt.FLOAT)


# ------------------------------------------------------------------ Phase 10: the Resonant Spire

def resonant_spire(seed):
    """A Resonant Spire of the Glimmering Crags: a hollow tower of Nightstone banded with Glimmer
    Crystal, ninety blocks tall, open to the sky inside. There are no stairs: you climb it on shulker
    bullets (the shulkers on its ledges shoot them) or a Potion of Upwelling (two lie in the chest at
    its foot). Ledges ring the shaft at four heights, each hung with a bell; arched openings pierce
    the walls, through which the Spire Wyrm coils in and out. At the top: the Wyrm's nest, and the
    Resonance Vault, which only the Wyrm's Resonant Key opens."""
    size, c = 37, 18
    height = 100
    t = Template(size, height, size, seed)
    rnd = t.rnd
    stone, bricks = a("nightstone"), a("nightstone_bricks")
    polished, chiseled = a("polished_nightstone"), a("chiseled_nightstone")
    crystal = a("glimmer_crystal")
    R_OUT, R_IN = 13.5, 11.5
    levels = [18, 36, 54, 72]           # ledge floors
    nest = 86                           # nest floor

    def opening(ang, y, base_y, width=0.22, tall=7):
        # Arched openings: four per ledge level, turned 45 degrees from one level to the next.
        for k, ly in enumerate([4] + levels):
            off = (math.pi / 4) * (k % 2)
            for q in range(4):
                a0 = off + q * math.pi / 2
                gap = abs((ang - a0 + math.pi) % math.tau - math.pi)
                top = ly + 2 + tall - int(gap / width * 2)
                if gap < width and ly + 1 <= y <= top:
                    return True
        return False

    # Foundation: a broad footing of Nightstone, ragged at the edge.
    for x in range(size):
        for z in range(size):
            d = math.hypot(x - c, z - c)
            if d < 18 - rnd.random() * 2:
                for y in range(0, 4):
                    t.set(x, y, z, stone)
            if d < R_IN:
                t.set(x, 3, z, polished if (x + z) % 2 else chiseled if d < 3 else polished)
    # The tower: walls tapering slightly, crystal bands every 9 blocks, openings.
    for y in range(4, nest + 1):
        taper = (y / nest) * 1.5
        ro, ri = R_OUT - taper, R_IN - taper
        for x in range(size):
            for z in range(size):
                d = math.hypot(x - c, z - c)
                if ri <= d <= ro:
                    ang = math.atan2(z - c, x - c)
                    if opening(ang, y, 4):
                        t.set(x, y, z, "minecraft:air")
                        continue
                    band = y % 9 == 0
                    t.set(x, y, z, crystal if band else (bricks if rnd.random() < 0.85 else stone))
                elif d < ri:
                    t.set(x, y, z, "minecraft:air")
    # The ledges: three arcs at each level (so you can't walk up), a bell, a shulker or two, a chest.
    for k, ly in enumerate(levels):
        taper = (ly / nest) * 1.5
        ri = R_IN - taper
        start = rnd.uniform(0, math.tau)
        for arc in range(3):
            a0 = start + arc * math.tau / 3
            for x in range(size):
                for z in range(size):
                    d = math.hypot(x - c, z - c)
                    ang = math.atan2(z - c, x - c)
                    gap = abs((ang - a0 + math.pi) % math.tau - math.pi)
                    if ri - 3.2 <= d < ri and gap < 0.55:
                        t.set(x, ly, z, polished)
                        if d < ri - 2.6 and rnd.random() < 0.5:
                            t.set(x, ly + 1, z, a("polished_nightstone_slab") + "[type=bottom]")
            # Bell in the middle of the arc, at its inner edge; a shulker on the far end.
            bx, bz = round(c + math.cos(a0) * (ri - 2.2)), round(c + math.sin(a0) * (ri - 2.2))
            t.set(bx, ly, bz, polished)
            t.set(bx, ly + 1, bz, "minecraft:bell[attachment=floor,facing=north,powered=false]")
            # End Chimes hang under the ledge (they keep the shulkers from teleporting off it).
            hx, hz = round(c + math.cos(a0 - 0.25) * (ri - 2.0)), round(c + math.sin(a0 - 0.25) * (ri - 2.0))
            if t.get(hx, ly, hz) is not None and t.get(hx, ly, hz) != "minecraft:air":
                t.set(hx, ly - 1, hz, a("end_chimes"))
            ex, ez = round(c + math.cos(a0 + 0.45) * (ri - 1.5)), round(c + math.sin(a0 + 0.45) * (ri - 1.5))
            if arc != 1 or k % 2 == 0:
                t.entity(ex + 0.5, ly + 1, ez + 0.5, {"id": "minecraft:shulker", "AttachFace": Byte(0),
                                                      "PersistenceRequired": Byte(1), "Color": Byte(9)})
            if arc == 2 and k in (1, 3):
                cx, cz = round(c + math.cos(a0 - 0.4) * (ri - 1.5)), round(c + math.sin(a0 - 0.4) * (ri - 1.5))
                chest(t, cx, ly + 1, cz, "resonant_spire")
    # At the foot: a chest with what you need to start climbing, and two shulkers to shoot you up.
    chest(t, c + 6, 4, c, "resonant_spire_foot", facing="west")
    for (dx, dz) in ((-5, 4), (4, -5)):
        t.entity(c + dx + 0.5, 4, c + dz + 0.5, {"id": "minecraft:shulker", "AttachFace": Byte(0),
                                                 "PersistenceRequired": Byte(1), "Color": Byte(9)})
    # The top: a floor across the shaft (the arena), the Wyrm's nest in the middle - a low rim of
    # bricks and old bones round a hollow it lies coiled in - four bells on posts round the edge
    # (ring one as the Wyrm passes and it crashes down, stunned), crystal spikes, and the Resonance
    # Vault on a dais.
    taper = 1.5
    top_r = R_OUT - taper
    for x in range(size):
        for z in range(size):
            d = math.hypot(x - c, z - c)
            if d <= top_r:
                t.set(x, nest, z, bricks if d > R_IN - taper - 0.5 else (polished if (x + z) % 3 else chiseled))
            for y in range(nest + 1, height):
                if d <= top_r:
                    t.set(x, y, z, "minecraft:air")
    for x in range(size):
        for z in range(size):
            d = math.hypot(x - c, z - c)
            if 5.0 <= d < 6.0:
                t.set(x, nest + 1, z, a("leviathan_bone_block") + "[axis=y]" if rnd.random() < 0.3 else bricks)
            elif d < 5.0:
                t.set(x, nest, z, stone if rnd.random() < 0.6 else polished)    # the worn hollow
    for q in range(8):
        ang = q * math.tau / 8 + 0.2
        sx, sz = round(c + math.cos(ang) * (top_r - 0.5)), round(c + math.sin(ang) * (top_r - 0.5))
        tall = rnd.randint(5, 11)
        for y in range(nest + 1, nest + 1 + tall):
            t.set(sx, y, sz, crystal if y > nest + 2 else bricks)
    for q in range(4):
        ang = math.pi / 4 + q * math.pi / 2
        bx, bz = round(c + math.cos(ang) * 9.5), round(c + math.sin(ang) * 9.5)
        t.set(bx, nest + 1, bz, chiseled)
        t.set(bx, nest + 2, bz, "minecraft:bell[attachment=floor,facing=north,powered=false]")
    vx, vz = c, c - 9
    t.fill(vx - 1, nest + 1, vz - 1, vx + 1, nest + 1, vz + 1, chiseled)
    t.set(vx, nest + 2, vz, f"minecraft:vault[facing=south,ominous=false,vault_state=inactive]",
          nbt={"config": {"loot_table": a("chests/resonance_vault"), "key_item": {"id": a("resonant_key"), "count": Int(1)}}})
    # The Wyrm itself, asleep in its nest.
    t.entity(c + 0.5, nest + 1, c + 0.5, {"id": a("spire_wyrm"), "PersistenceRequired": Byte(1)})
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
        "step": "surface_structures",
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
         biomes=[a("ashen_barrens")], spread=(64, 24, 61829341), height={"absolute": -8}, heightmap="WORLD_SURFACE_WG",
         spawns=None, adaptation="beard_thin", ground=(16, 4096, 12)),
    # Relays float out over the void, so they want open space (or low ground) under them.
    dict(name="wanderer_relay", variants=[lambda: wanderer_relay(21)],
         biomes=[a("hollow_isles")], spread=(18, 7, 51273391),
         height={"type": "minecraft:uniform", "min_inclusive": {"absolute": 66}, "max_inclusive": {"absolute": 92}},
         heightmap=None, spawns=None, adaptation=None, ground=(-4096, 40, 7)),
    # A landmark: rare, out on the vanilla Highlands or in the Nebula Fields.
    dict(name="shattered_observatory", variants=[lambda: shattered_observatory(41)],
         biomes=["minecraft:end_highlands", a("nebula_fields")], spread=(40, 16, 33094871), height={"absolute": -2},
         heightmap="WORLD_SURFACE_WG", spawns=None, adaptation="beard_thin", ground=(16, 4096, 18)),
    dict(name="solar_array", variants=[lambda: solar_array(51)],
         biomes=[a("corona")], spread=(8, 4, 92814473), height={"absolute": -8},
         heightmap="WORLD_SURFACE_WG", spawns=None, adaptation="beard_thin", ground=(16, 4096, 18)),
    # Mazes float out over open void, among the small islands. (The Rift Maw is built in Java now: see
    # write_data.)
    # Phase 10's tower of the Glimmering Crags. Retired by the distillation (the Curator replaces the
    # Spire Wyrm): its template is still built, so it can be placed by hand, but it isn't generated.
    dict(name="resonant_spire", variants=[lambda: resonant_spire(81)], retired=True,
         biomes=[a("glimmering_crags")], spread=(28, 10, 51772093), height={"absolute": -3},
         heightmap="WORLD_SURFACE_WG", spawns=None, adaptation="beard_thin", ground=(16, 4096, 16)),
    dict(name="constellation_maze", variants=[lambda: constellation_maze(71)],
         biomes=["minecraft:small_end_islands", a("nebula_fields")], spread=(24, 8, 77162391),
         height={"type": "minecraft:uniform", "min_inclusive": {"absolute": 56}, "max_inclusive": {"absolute": 76}},
         heightmap=None, spawns=None, adaptation=None, ground=(-4096, 40, 22)),
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
        if st.get("retired"):
            # Not generated: no worldgen files for it (and none left over from before).
            for stale in (DATA / "worldgen/structure" / f"{name}.json", DATA / "worldgen/structure_set" / f"{name}.json",
                          DATA / "worldgen/template_pool" / name / "start.json",
                          DATA / "tags/worldgen/biome/has_structure" / f"{name}.json"):
                if stale.exists():
                    stale.unlink()
            continue
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
    # The Whalefall is built by Java (WhalefallStructure / WhalefallPiece: too big for a template). It
    # drifts over open void anywhere in the outer End, very rarely.
    write_json(DATA / "worldgen/structure/whalefall.json", {
        "type": a("whalefall"), "biomes": f"#{NS}:has_structure/whalefall", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/whalefall.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 70127811, "separation": 30, "spacing": 64},
        "structures": [{"structure": a("whalefall"), "weight": 1}]})
    # Phase 11: the Rift Maw is built by Java too (RiftMawStructure / RiftMawPiece): a colossal skull
    # on its own island, out over the void among the small islands.
    write_json(DATA / "worldgen/structure/rift_maw.json", {
        "type": a("rift_maw"), "biomes": f"#{NS}:has_structure/rift_maw", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/rift_maw.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 48129573, "separation": 12, "spacing": 30},
        "structures": [{"structure": a("rift_maw"), "weight": 1}]})
    write_json(DATA / "tags/worldgen/biome/has_structure/rift_maw.json", {"values": [
        "minecraft:small_end_islands", a("nebula_fields"), a("hollow_isles")]})
    # The Remnant hangs high over the hell half (the Corona) of every Eclipse (RemnantStructure /
    # RemnantPiece). Every chunk is a candidate; RemnantStructure only starts in the one under the
    # Eclipse's anchor (EclipseLayout.REMNANT).
    write_json(DATA / "worldgen/structure/remnant.json", {
        "type": a("remnant"), "biomes": f"#{NS}:has_structure/remnant", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/remnant.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 31337117, "separation": 0, "spacing": 1},
        "structures": [{"structure": a("remnant"), "weight": 1}]})
    write_json(DATA / "tags/worldgen/biome/has_structure/remnant.json", {"values": [a("corona")]})
    # Distillation 5: the Leviathan (LeviathanStructure / LeviathanPiece), a whole skeleton with a citadel
    # on it, out over the void of the small islands and the barrens. Rare: one region in 90 chunks square.
    write_json(DATA / "worldgen/structure/leviathan.json", {
        "type": a("leviathan"), "biomes": f"#{NS}:has_structure/leviathan", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/leviathan.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 90210447, "separation": 40, "spacing": 90},
        "structures": [{"structure": a("leviathan"), "weight": 1}]})
    write_json(DATA / "tags/worldgen/biome/has_structure/leviathan.json", {"values": [
        "minecraft:small_end_islands", "minecraft:end_barrens", a("ashen_barrens")]})
    write_json(DATA / "tags/worldgen/biome/has_structure/whalefall.json", {"values": [
        "minecraft:small_end_islands", "minecraft:end_barrens", "minecraft:end_midlands", "minecraft:end_highlands",
        a("nebula_fields"), a("chorus_thicket"), a("ashen_barrens"), a("hollow_isles"),
        a("glimmering_crags")]})


def main():
    made = write_templates(preview="--preview" in sys.argv)
    write_data()
    print(f"Wrote {len(made)} structure templates: {', '.join(made)}")


if __name__ == "__main__":
    main()
