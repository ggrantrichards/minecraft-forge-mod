"""Refreshes the README's and Modrinth's in-game pictures from the latest CI screenshots.

    git fetch origin ci-screenshots
    python3 tools/refresh_doc_images.py            # reads FETCH_HEAD's astralend-*.png

Each picture under docs/images/ is replaced by the screenshot of the same view, scaled to 854x480.
A view missing from the screenshots is left as it was (and named).
"""
import io
import subprocess
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SIZE = (854, 480)

# docs picture -> the screenshot it comes from (astralend-<name>.png on the ci-screenshots branch)
PICTURES = {
    **{f"screens/{b}": b for b in ("nebula_fields", "ashen_barrens", "gilded_reach", "deep_hollows")},
    "screens/shattered_observatory": "structure_shattered_observatory",
    **{f"mobs/{m}": f"mob_{m}" for m in ("pollinator", "umbral_stalker_lit",
                                          "void_wanderer", "watcher")},
    "mobs/drift_whale": "mob_drift_whale_side",
    "mobs/umbral_stalker": "mob_umbral_stalker_angled",
    "phase10/glimmering_crags": "glimmering_crags",
    "phase10/resonant_spire": "structure_resonant_spire",
    "phase10/whalefall": "structure_whalefall",
    "phase10/wyrm_nest": "wyrm_nest",
}


def main(ref="FETCH_HEAD"):
    missing = []
    for picture, shot in PICTURES.items():
        try:
            data = subprocess.run(["git", "show", f"{ref}:astralend-{shot}.png"], cwd=ROOT, check=True,
                                  capture_output=True).stdout
        except subprocess.CalledProcessError:
            missing.append(picture)
            continue
        img = Image.open(io.BytesIO(data)).convert("RGBA").resize(SIZE, Image.LANCZOS)
        img.save(ROOT / "docs/images" / f"{picture}.png")
        print("updated", picture)
    if missing:
        print("no screenshot for:", ", ".join(missing))


if __name__ == "__main__":
    main()
