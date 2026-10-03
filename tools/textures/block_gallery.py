"""Every full block of the mod as an inventory-style cube, with its name: one picture to keep.

    python3 tools/textures/block_gallery.py   -> docs/images/blocks.png

Reads the block models (cube_all, cube_column and leaves) and the English names, so it always shows
what the game shows.
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from block_sheet import cube

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/astralend"
CELL_W, CELL_H, COLS = 150, 178, 8
BG = (30, 28, 38)
SKIP = ("_wood", "stripped_", "glass", "rift_tile", "pedestal_", "maw_eye_lit", "star_bridge", "wormhole")


def texture(ref):
    ns, _, path = ref.partition(":")
    folder = ROOT / "src/main/resources/assets" / (ns if path else "minecraft") / "textures"
    img = Image.open(folder / f"{path or ns}.png").convert("RGBA")
    return img.crop((0, 0, 16, 16))


def blocks():
    lang = json.loads((ASSETS / "lang/en_us.json").read_text())
    out = []
    for f in sorted((ASSETS / "models/block").glob("*.json")):
        name = f.stem
        if any(s in name for s in SKIP):
            continue
        m = json.loads(f.read_text())
        parent, tex = m.get("parent", ""), m.get("textures", {})
        if parent.endswith("cube_all") or parent.endswith("leaves"):
            top = side = tex.get("all")
        elif parent.endswith("cube_column"):
            top, side = tex.get("end"), tex.get("side")
        else:
            continue
        title = lang.get(f"block.astralend.{name}")
        if not title or not top:
            continue
        out.append((title, texture(top), texture(side)))
    return out


def main():
    items = blocks()
    rows = (len(items) + COLS - 1) // COLS
    sheet = Image.new("RGB", (COLS * CELL_W + 40, rows * CELL_H + 90), BG)
    d = ImageDraw.Draw(sheet)

    def font(size, bold=False):
        try:
            return ImageFont.truetype(f"/usr/share/fonts/truetype/dejavu/DejaVuSans{'-Bold' if bold else ''}.ttf", size)
        except OSError:
            return ImageFont.load_default()
    d.text((20, 18), f"The Astral End - the blocks ({len(items)})", fill=(236, 230, 250), font=font(26, True))
    label = font(13)
    for i, (title, top, side) in enumerate(items):
        x, y = 20 + (i % COLS) * CELL_W, 70 + (i // COLS) * CELL_H
        c = cube(top, side).resize((128, 128), Image.LANCZOS)
        sheet.paste(c, (x + (CELL_W - 128) // 2, y), c)
        words, lines = title.split(), [""]
        for w in words:
            if d.textlength((lines[-1] + " " + w).strip(), font=label) > CELL_W - 10:
                lines.append(w)
            else:
                lines[-1] = (lines[-1] + " " + w).strip()
        for k, line in enumerate(lines[:2]):
            d.text((x + (CELL_W - d.textlength(line, font=label)) / 2, y + 134 + k * 16), line, fill=(200, 194, 220), font=label)
    out = ROOT / "docs/images/blocks.png"
    sheet.save(out)
    print(f"wrote {out.relative_to(ROOT)} ({len(items)} blocks)")


if __name__ == "__main__":
    main()
