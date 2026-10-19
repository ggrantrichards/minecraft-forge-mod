"""Gives a trailer already filmed a new soundtrack, without filming it again: the picture from the
video, the sounds from the sound log written while it was filmed (TrailerSound), and the edit's timings
and music from scenes.txt, as compose.py would mix them.

    python3 tools/trailer/remix.py <old video> <sounds.txt> <out dir>
"""
import os
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import compose  # noqa: E402
import music  # noqa: E402
import soundtrack  # noqa: E402


def main(video, log, out_dir):
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    beats = compose.parse()
    starts, t = [], 0
    for b in beats:
        starts.append(t)
        t += b["frames"]
    seconds = t / compose.FPS
    segments = [(st / compose.FPS, b["music"][0], b["music"][1], b["music"][2]) for b, st in zip(beats, starts) if "music" in b]
    shot_starts = {b["index"]: st for b, st in zip(beats, starts) if b["kind"] == "shot"}
    track = soundtrack.build(seconds, segments, log, shot_starts, out_dir / "mix.txt")
    wav = out_dir / "score.wav"
    music.write(str(wav), track)
    ffmpeg = os.environ.get("FFMPEG", "ffmpeg")
    mp4 = out_dir / "the_astral_end_trailer.mp4"
    subprocess.run([ffmpeg, "-y", "-loglevel", "error", "-i", str(video), "-i", str(wav), "-map", "0:v", "-map", "1:a",
                    "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart", str(mp4)], check=True)
    print(f"wrote {mp4} with a new soundtrack ({seconds:.1f} s)")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
