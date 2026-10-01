"""Draws a structure template the way it will look in the world (isometric, lit like the game),
so a structure can be judged before it goes into the game. Mod blocks use their real textures;
vanilla blocks get a flat colour of roughly their look.

    python3 tools/structures/preview.py comet_crater   -> docs/images/structures/comet_crater.png
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/textures"))
from scene import far, render  # noqa: E402

TEX = ROOT / "src/main/resources/assets/astralend/textures/block"
VANILLA = {   # rough colours of the vanilla blocks the structures use
    "end_stone": "dcdca0", "end_stone_bricks": "d7d69c", "obsidian": "140f20", "crying_obsidian": "2a0f4a",
    "chest": "a0702c", "barrel": "8a6234", "magma_block": "a83c10", "lantern": "e8b048", "chain": "4a4a52",
    "glowstone": "e8c878", "purpur_block": "a678a6", "purpur_pillar": "ac82ac", "end_rod": "f0f0e8",
    "vault": "3a4048", "smooth_quartz": "ece6dc", "tinted_glass": "2c2634", "iron_bars": "70707a",
    "copper_block": "c06c4c", "amethyst_block": "8a62c8", "lodestone": "8a8a8a", "lectern": "a87c48",
    "bookshelf": "8a6a3c", "candle": "e8d8b0", "glass": "c0dce4", "fire": "f0a838",
}
_cache = {}


def _tex(name):
    if name in _cache:
        return _cache[name]
    ns, path = name.split(":")
    result = None
    if ns == "astralend":
        bases = [path]
        for suffix in ("_slab", "_stairs", "_wall"):
            if path.endswith(suffix):
                base = path[:-len(suffix)]
                bases = [base, base + "s", base.replace("_brick", "_bricks")]
        if path.endswith("_block"):
            bases.append(path[:-6])
        for base in bases:
            for top, side in ((base, base), (base + "_top", base + "_side"), (base + "_end", base + "_side"),
                              (base + "_top", base), (base, base + "_side")):
                if (TEX / f"{top}.png").exists() and (TEX / f"{side}.png").exists():
                    t = Image.open(TEX / f"{top}.png").convert("RGBA").crop((0, 0, 16, 16))
                    s = Image.open(TEX / f"{side}.png").convert("RGBA").crop((0, 0, 16, 16))
                    result = (t, s)
                    break
            if result:
                break
    if result is None:
        colour = VANILLA.get(path, "ff00ff")
        img = Image.new("RGBA", (16, 16), tuple(int(colour[i:i + 2], 16) for i in (0, 2, 4)) + (255,))
        result = (img, img)
    _cache[name] = result
    return result


ALIASES = {"maze_telescope": "telescope_tube", "maw_eye": "maw_eye"}


def voxels(template, cut=None, turn=False):
    """cut=(axis, value) leaves out every block past that plane, to show a structure's inside.
    turn=True turns it a quarter so its east (+x) face is drawn where the south face was."""
    out = {}
    for (x, y, z), (state, _nbt) in template.blocks.items():
        if turn:
            x, z = template.size[2] - 1 - z, x
        if cut and (x, y, z)["xyz".index(cut[0])] > cut[1]:
            continue
        name = state.split("[")[0]
        if name.endswith(":air") or name.endswith("structure_void"):
            continue
        name = name if ":" in name else "minecraft:" + name
        ns, path = name.split(":")
        out[(x, y, z)] = _tex(f"{ns}:{ALIASES.get(path, path)}")
    return out


def save(template, name, block=12, cut=None, turn=False):
    img = render(voxels(template, cut, turn), block)
    out = ROOT / "docs/images/structures" / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    return out
