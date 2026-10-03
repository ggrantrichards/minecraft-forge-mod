"""Speaks the trailer's narration - the `vo` lines in scenes.txt - in a deep movie-trailer voice.

    python3 tools/trailer/narrate.py <kokoro model .onnx> <voices .bin> <out dir>

Writes <out dir>/vo/00.wav, 01.wav, ... (44.1 kHz stereo), which compose.py lays over the score.
The voice is Kokoro (kokoro-onnx; the Kokoro-82M model and its voices are Apache-2.0), spoken a
little slowly, then pitched down, warmed, compressed and given a touch of room with ffmpeg - the
"In a world..." sound. Needs kokoro-onnx, soundfile and ffmpeg.
"""
import os
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import compose  # noqa: E402

VOICE = os.environ.get("NARRATOR_VOICE", "am_onyx")
SPEED = 0.88
# Pitch down about two semitones (which also slows it a touch), then the trailer treatment: rumble
# cut, chest boosted, a gentle compressor, a short slap of room, and loudness evened out.
FILTER = ("asetrate={rate}*0.885,aresample=44100,highpass=f=55,bass=g=5:f=120:w=0.8,"
          "equalizer=f=3200:t=q:w=1.2:g=2,acompressor=threshold=-20dB:ratio=3:attack=8:release=160,"
          "aecho=0.85:0.5:38|76:0.16|0.08,loudnorm=I=-15:TP=-1.5:LRA=7,aformat=channel_layouts=stereo")


def main(model, voices, out_dir):
    from kokoro_onnx import Kokoro
    import soundfile as sf

    beats = compose.parse()
    starts, t = [], 0
    for b in beats:
        starts.append(t)
        t += b["frames"]
    lines = compose.narration(beats, starts)
    vo = Path(out_dir) / "vo"
    vo.mkdir(parents=True, exist_ok=True)
    kokoro = Kokoro(model, voices)
    for at, text, i in lines:
        samples, rate = kokoro.create(text, voice=VOICE, speed=SPEED, lang="en-us")
        raw = vo / f"{i:02d}_raw.wav"
        sf.write(str(raw), samples, rate)
        out = vo / f"{i:02d}.wav"
        subprocess.run([os.environ.get("FFMPEG", "ffmpeg"), "-y", "-loglevel", "error", "-i", str(raw), "-af", FILTER.format(rate=rate),
                        "-ar", "44100", "-ac", "2", "-c:a", "pcm_s16le", str(out)], check=True)
        raw.unlink()
        seconds = len(samples) / rate / 0.885
        print(f"line {i} at {at:.1f} s, {seconds:.1f} s long: {text}")
    print(f"spoke {len(lines)} lines in the voice {VOICE}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
