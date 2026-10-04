"""Cuts the README's and Modrinth's moving teaser out of the trailer: fifteen seconds from the card
"Somewhere, the void holds its breath." - the Eclipse revealed, heaven and hell - as a looping animated WebP (no sound).

    python3 tools/trailer/teaser.py <the_astral_end_trailer.mp4> [docs/modrinth/teaser.webp]

Needs ffmpeg (with libwebp); FFMPEG names another binary.
"""
import os
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import compose  # noqa: E402

LENGTH = 15.0


def start():
    t = 0
    for b in compose.parse():
        if b["kind"] == "card" and b["text"].startswith("Somewhere, the void"):
            return t / compose.FPS
        t += b["frames"]
    return 0.0


def main(mp4, out="docs/modrinth/teaser.webp"):
    at = start()
    subprocess.run([os.environ.get("FFMPEG", "ffmpeg"), "-y", "-loglevel", "error", "-ss", f"{at:.2f}", "-t", str(LENGTH),
                    "-i", mp4, "-vf", "fps=12,scale=640:-1:flags=lanczos", "-loop", "0", "-c:v", "libwebp_anim",
                    "-quality", "62", "-compression_level", "6", "-an", out], check=True)
    print(f"wrote {out}: {LENGTH:.0f} s from {at:.1f} s ({Path(out).stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main(*sys.argv[1:])
