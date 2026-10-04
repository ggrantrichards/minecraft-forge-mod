"""Prepares the trailer's narration from a real voice: recordings of the `vo` lines in scenes.txt.

    python3 tools/trailer/narrate.py <out dir>

Put one recording per line in tools/trailer/voice/, numbered in the order the lines appear
(00.wav, 01.wav, ... - .wav, .mp3, .m4a, .ogg or .flac all work; tools/trailer/voice/README.md lists
the lines and their numbers). Each is cleaned up gently - rumble cut, levels evened, a touch of room,
no change of pitch - and written to <out dir>/vo/NN.wav, which compose.py lays over the score. A line
with no recording is left out; with none at all the trailer runs on its title cards alone. (No
synthetic voice: it sounded synthetic.) Needs ffmpeg.
"""
import os
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import compose  # noqa: E402

VOICE_DIR = Path(__file__).parent / "voice"
EXTENSIONS = (".wav", ".mp3", ".m4a", ".ogg", ".flac")
FILTER = ("highpass=f=70,afftdn=nf=-30,acompressor=threshold=-22dB:ratio=2.5:attack=10:release=180,"
          "aecho=0.9:0.6:30:0.08,loudnorm=I=-16:TP=-1.5:LRA=8,aformat=channel_layouts=stereo")


def recording(i):
    for ext in EXTENSIONS:
        p = VOICE_DIR / f"{i:02d}{ext}"
        if p.exists():
            return p
    return None


def main(out_dir):
    beats = compose.parse()
    starts, t = [], 0
    for b in beats:
        starts.append(t)
        t += b["frames"]
    lines = compose.narration(beats, starts)
    vo = Path(out_dir) / "vo"
    vo.mkdir(parents=True, exist_ok=True)
    done = 0
    for at, text, i in lines:
        src = recording(i)
        if src is None:
            continue
        subprocess.run([os.environ.get("FFMPEG", "ffmpeg"), "-y", "-loglevel", "error", "-i", str(src), "-af", FILTER,
                        "-ar", "44100", "-ac", "2", "-c:a", "pcm_s16le", str(vo / f"{i:02d}.wav")], check=True)
        done += 1
        print(f"line {i} at {at:.1f} s: {text}")
    print(f"narration: {done} of {len(lines)} lines recorded" + ("" if done else " - the trailer runs on its title cards"))


if __name__ == "__main__":
    main(sys.argv[-1])
