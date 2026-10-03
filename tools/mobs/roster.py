"""All the mobs of the mod in one picture to keep: each drawn from its own model and textures (as
preview_mobs.py draws them, glowing parts at full brightness), three-quarters on, with its name.

    python3 tools/mobs/roster.py   -> docs/images/mobs.png
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import make_mobs
from preview_mobs import laid_out, render

ROOT = Path(__file__).resolve().parents[2]
NAMES = {"pollinator": "End Pollinator", "void_wanderer": "Void Wanderer", "umbral_stalker": "Umbral Stalker",
         "cinder_mite": "Cinder Mite", "void_ray": "Void Ray", "hollow": "The Hollow", "drift_whale": "Drift Whale",
         "watcher": "The Watcher", "live_comet": "Live Comet", "spire_wyrm": "Spire Wyrm"}
CELL = 360
BG = (30, 30, 38, 255)


def main():
    def font(size, bold=False):
        try:
            return ImageFont.truetype(f"/usr/share/fonts/truetype/dejavu/DejaVuSans{'-Bold' if bold else ''}.ttf", size)
        except OSError:
            return ImageFont.load_default()
    mobs = [m for m in make_mobs.MOBS if m.name != "spire_wyrm"]
    wyrm = next(m for m in make_mobs.MOBS if m.name == "spire_wyrm")
    cols = 3
    rows = (len(mobs) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * CELL + 40, rows * (CELL + 34) + CELL + 130), BG)
    d = ImageDraw.Draw(sheet)
    d.text((20, 18), "The Astral End - the creatures", fill=(236, 230, 250), font=font(26, True))
    label = font(18, True)

    def draw(mob, x, y, w, h, yaw, pitch):
        folder = make_mobs.TEX / "entity" / mob.name
        tex = Image.open(folder / f"{mob.name}.png").convert("RGBA")
        glow_file = folder / f"{mob.name}_glow.png"
        glow = Image.open(glow_file).convert("RGBA") if glow_file.exists() else None
        img = render(laid_out(mob), tex, yaw, pitch, max(w, h), glow)
        img = img.resize((w, h)) if img.size != (w, h) else img
        sheet.paste(img, (x, y))

    for i, mob in enumerate(mobs):
        x, y = 20 + (i % cols) * CELL, 64 + (i // cols) * (CELL + 34)
        draw(mob, x, y, CELL, CELL, -0.7, 0.35)
        name = NAMES.get(mob.name, mob.name)
        d.text((x + (CELL - d.textlength(name, font=label)) / 2, y + CELL + 4), name, fill=(220, 214, 240), font=label)
    y = 64 + rows * (CELL + 34)
    w = cols * CELL
    folder = make_mobs.TEX / "entity" / wyrm.name
    big = render(laid_out(wyrm), Image.open(folder / "spire_wyrm.png").convert("RGBA"), 1.3, 0.35, w,
                 Image.open(folder / "spire_wyrm_glow.png").convert("RGBA") if (folder / "spire_wyrm_glow.png").exists() else None)
    big = big.crop((0, (big.height - CELL) // 2, w, (big.height + CELL) // 2))
    sheet.paste(big, (20, y))
    name = "Spire Wyrm (mini-boss)"
    d.text((20 + (w - d.textlength(name, font=label)) / 2, y + CELL + 4), name, fill=(220, 214, 240), font=label)
    out = ROOT / "docs/images/mobs.png"
    sheet.convert("RGB").save(out)
    print("wrote", out.relative_to(ROOT))


if __name__ == "__main__":
    main()
