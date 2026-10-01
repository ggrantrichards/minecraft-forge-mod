"""A structure template being built block by block, saved in Minecraft's structure format.

    t = Template(25, 12, 25)
    t.set(x, y, z, "astralend:charred_end_stone")
    t.set(x, y, z, "minecraft:chest[facing=north]", nbt={"LootTable": "astralend:chests/comet_crater"})
    t.save(path)

Blocks never set are left out of the file, so the world's own blocks stay there. Set "minecraft:air"
to carve a space out of the terrain.
"""
import math
import random

import nbt

DATA_VERSION = 5023   # Minecraft 26.3


def parse_state(state):
    if "[" not in state:
        name, props = state, {}
    else:
        name, rest = state.split("[", 1)
        props = dict(p.split("=") for p in rest.rstrip("]").split(",") if p)
    if ":" not in name:
        name = "minecraft:" + name
    return name, props


class Template:
    def __init__(self, sx, sy, sz, seed=0):
        self.size = (sx, sy, sz)
        self.blocks = {}       # (x, y, z) -> (state, nbt)
        self.entities = []
        self.rnd = random.Random(seed)

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, state, nbt=None):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if self.inside(x, y, z):
            self.blocks[(x, y, z)] = (state, nbt)

    def get(self, x, y, z):
        b = self.blocks.get((int(x), int(y), int(z)))
        return b[0] if b else None

    def fill(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, state)

    def line(self, a, b, state, thickness=0.0):
        """Blocks along a straight line from a to b (each a 3-tuple), optionally thickened."""
        steps = int(max(abs(b[i] - a[i]) for i in range(3)) * 2) + 1
        for s in range(steps + 1):
            t = s / steps
            p = [a[i] + (b[i] - a[i]) * t for i in range(3)]
            if thickness <= 0:
                self.set(*p, state)
            else:
                r = int(math.ceil(thickness))
                for dx in range(-r, r + 1):
                    for dy in range(-r, r + 1):
                        for dz in range(-r, r + 1):
                            if dx * dx + dy * dy + dz * dz <= thickness * thickness:
                                self.set(p[0] + dx, p[1] + dy, p[2] + dz, state)

    def entity(self, x, y, z, nbt_tag):
        self.entities.append((x, y, z, nbt_tag))

    def save(self, path):
        palette, index = [], {}
        blocks = []
        for (x, y, z), (state, tag) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            if state not in index:
                name, props = parse_state(state)
                entry = {"Name": name}
                if props:
                    entry["Properties"] = {k: v for k, v in sorted(props.items())}
                index[state] = len(palette)
                palette.append(entry)
            b = {"pos": nbt.List([nbt.Int(x), nbt.Int(y), nbt.Int(z)], nbt.INT), "state": nbt.Int(index[state])}
            if tag:
                b["nbt"] = tag
            blocks.append(b)
        entities = []
        for (x, y, z, tag) in self.entities:
            entities.append({
                "pos": nbt.List([nbt.Double(x), nbt.Double(y), nbt.Double(z)], nbt.DOUBLE),
                "blockPos": nbt.List([nbt.Int(int(x)), nbt.Int(int(y)), nbt.Int(int(z))], nbt.INT),
                "nbt": tag,
            })
        root = {
            "DataVersion": nbt.Int(DATA_VERSION),
            "size": nbt.List([nbt.Int(s) for s in self.size], nbt.INT),
            "palette": nbt.List(palette, nbt.COMPOUND),
            "blocks": nbt.List(blocks, nbt.COMPOUND),
            "entities": nbt.List(entities, nbt.COMPOUND),
        }
        nbt.write(path, root)
