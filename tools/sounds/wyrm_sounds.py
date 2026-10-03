"""The Spire Wyrm's voice, synthesised from scratch (no samples, nothing borrowed): a glottal
growl through vocal-tract formants, a rattling noise layer, a sub-octave for size, a short room.

    python3 tools/sounds/wyrm_sounds.py        -> src/main/resources/assets/astralend/sounds/mob/spire_wyrm/*.ogg

Needs numpy, scipy and ffmpeg (FFMPEG names another binary). Each sound has a few variants, as the
game's own mob sounds do, so it never repeats exactly.
"""
import os
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np
from scipy import signal

RATE = 44100
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/sounds/mob/spire_wyrm"

# Formants (Hz, bandwidth) of a huge throat: an open "ah", a closed "oo", a hissing "eh".
VOWELS = {
    "ah": [(420, 120), (780, 160), (2100, 300)],
    "oo": [(260, 90), (520, 120), (1900, 280)],
    "eh": [(380, 120), (1500, 220), (2600, 320)],
}


def curve(points, n):
    """A smooth contour through (time 0..1, value) points, n samples long."""
    t = np.linspace(0, 1, n)
    xs, ys = zip(*points)
    return np.interp(t, xs, ys)


def voice(rnd, dur, f0, vowel_a, vowel_b, growl=0.6, rattle=0.5, rattle_hz=28.0, breath=0.25):
    """dur seconds of growl. f0 is a list of (t, Hz); the vowel morphs from vowel_a to vowel_b."""
    n = int(dur * RATE)
    pitch = curve(f0, n)
    pitch *= 1 + 0.025 * np.cumsum(rnd.normal(0, 1, n)) / np.sqrt(np.arange(1, n + 1))   # wander
    pitch *= 1 + 0.012 * np.sin(2 * np.pi * 5.5 * np.arange(n) / RATE)                   # tremor
    phase = 2 * np.pi * np.cumsum(pitch) / RATE
    src = np.zeros(n)
    for k in range(1, 48):
        amp = 1.0 / k ** 1.15
        src += amp * np.sin(k * phase) * (k * pitch < 5000)
    sub = np.sin(phase / 2) * growl + 0.5 * np.sin(phase / 2 + np.sin(phase / 3)) * growl   # sub-octave rumble
    src = src + sub
    # the rattle: amplitude flutter like air through a big throat
    flutter = 1 - rattle * 0.5 * (1 + np.sin(2 * np.pi * rattle_hz * np.arange(n) / RATE + rnd.uniform(0, 6)))
    flutter = np.maximum(flutter, 0.15)
    src *= flutter
    noise = rnd.normal(0, 1, n) * flutter
    b, a = signal.butter(2, [250 / (RATE / 2), 3500 / (RATE / 2)], "band")
    src = src + breath * 1.6 * signal.lfilter(b, a, noise)
    # formants, morphing from one vowel to the other
    def tract(x, vowel):
        y = np.zeros_like(x)
        for f, bw in VOWELS[vowel]:
            bb, aa = signal.iirpeak(f / (RATE / 2), f / bw)
            y += signal.lfilter(bb, aa, x)
        return y + 0.15 * x
    mix = curve([(0, 0), (0.35, 0.6), (1, 1)], n)
    out = tract(src, vowel_a) * (1 - mix) + tract(src, vowel_b) * mix
    return out


def noise_band(rnd, dur, lo, hi):
    n = int(dur * RATE)
    b, a = signal.butter(3, [lo / (RATE / 2), hi / (RATE / 2)], "band")
    return signal.lfilter(b, a, rnd.normal(0, 1, n))


def env(n, attack, release, shape=None):
    e = np.ones(n)
    a, r = int(attack * RATE), int(release * RATE)
    e[:a] = np.linspace(0, 1, a) ** 1.5
    e[n - r:] *= np.linspace(1, 0, r) ** 2
    if shape is not None:
        e *= curve(shape, n)
    return e


def room(x, seconds=0.7, wet=0.22, seed=3):
    rnd = np.random.default_rng(seed)
    n = int(seconds * RATE)
    ir = rnd.normal(0, 1, n) * np.exp(-np.arange(n) / (n / 5))
    b, a = signal.butter(2, 3000 / (RATE / 2))
    ir = signal.lfilter(b, a, ir)
    ir /= np.sqrt(np.sum(ir ** 2))
    tail = signal.fftconvolve(x, ir)[: len(x) + n]
    dry = np.concatenate([x, np.zeros(len(tail) - len(x))])
    out = dry * (1 - wet) + tail * wet
    return out


def finish(x, gain_db=-1.5):
    b, a = signal.butter(2, 35 / (RATE / 2), "high")      # nothing below hearing
    x = signal.lfilter(b, a, x)
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 1.1)     # a little grit
    fade = int(0.01 * RATE)
    x[:fade] *= np.linspace(0, 1, fade)
    x[-fade:] *= np.linspace(1, 0, fade)
    return x / np.max(np.abs(x)) * 10 ** (gain_db / 20)


def roar(seed, low=1.0):
    rnd = np.random.default_rng(seed)
    d = 2.6
    x = voice(rnd, d, [(0, 62 * low), (0.25, 84 * low), (0.6, 74 * low), (1, 52 * low)], "oo", "ah", growl=0.8, rattle=0.65)
    x *= env(len(x), 0.18, 0.9, [(0, 0.6), (0.2, 1), (0.7, 0.9), (1, 0.5)])
    boom = np.sin(2 * np.pi * 42 * np.arange(len(x)) / RATE) * np.exp(-np.arange(len(x)) / (0.5 * RATE)) * 3
    return room(x + boom)


def ambient(seed):
    rnd = np.random.default_rng(seed)
    d = 1.5 + rnd.uniform(0, 0.6)
    base = rnd.uniform(46, 58)
    x = voice(rnd, d, [(0, base), (0.5, base * 1.15), (1, base * 0.9)], "oo", "oo", growl=0.9, rattle=0.8, rattle_hz=18,
              breath=0.4)
    x *= env(len(x), 0.3, 0.6, [(0, 0.4), (0.5, 1), (1, 0.5)])
    return room(x, 0.5, 0.18, seed)


def hurt(seed):
    rnd = np.random.default_rng(seed)
    d = 0.55
    top = rnd.uniform(130, 160)
    x = voice(rnd, d, [(0, top), (0.3, top * 1.1), (1, top * 0.6)], "ah", "eh", growl=0.5, rattle=0.4, rattle_hz=40, breath=0.5)
    x *= env(len(x), 0.01, 0.3)
    return room(x, 0.4, 0.15, seed)


def death(seed):
    rnd = np.random.default_rng(seed)
    d = 4.2
    x = voice(rnd, d, [(0, 90), (0.15, 104), (0.6, 60), (1, 28)], "ah", "oo", growl=1.0, rattle=0.75, rattle_hz=22, breath=0.35)
    x *= env(len(x), 0.08, 2.0, [(0, 1), (0.6, 0.9), (1, 0.3)])
    crumble = noise_band(rnd, d, 120, 900) * env(len(x), 1.5, 1.5, [(0, 0), (0.55, 0), (0.75, 1), (1, 0.4)]) * 0.6
    return room(x + crumble, 1.2, 0.3, seed)


def charge(seed):
    rnd = np.random.default_rng(seed)
    d = 1.4
    x = voice(rnd, d, [(0, 48), (1, 120)], "oo", "ah", growl=0.7, rattle=0.6, rattle_hz=24, breath=0.5)
    whoosh = noise_band(rnd, d, 400, 4000) * curve([(0, 0), (0.7, 0.6), (1, 1.2)], int(d * RATE))
    x = (x + whoosh * 0.8) * env(int(d * RATE), 0.2, 0.08)
    return room(x, 0.4, 0.15, seed)


def breath(seed):
    rnd = np.random.default_rng(seed)
    d = 1.3
    hiss = noise_band(rnd, d, 700, 6000) * env(int(d * RATE), 0.05, 0.6, [(0, 1), (0.4, 0.8), (1, 0.4)])
    rumble = voice(rnd, d, [(0, 70), (1, 55)], "eh", "ah", growl=0.4, rattle=0.3, breath=0.6) * env(int(d * RATE), 0.05, 0.6) * 0.5
    return room(hiss * 2.5 + rumble, 0.5, 0.2, seed)


def slam(seed):
    rnd = np.random.default_rng(seed)
    d = 0.9
    n = int(d * RATE)
    t = np.arange(n) / RATE
    thump = np.sin(2 * np.pi * (60 * np.exp(-t * 3)) * t * 6) * np.exp(-t * 7) * 3
    crunch = noise_band(rnd, d, 200, 5000) * np.exp(-t * 14) * 1.5
    debris = noise_band(rnd, d, 1500, 7000) * np.exp(-((t - 0.25) ** 2) / 0.02) * 0.4
    return room(thump + crunch + debris, 0.6, 0.25, seed)


def snore(seed):
    rnd = np.random.default_rng(seed)
    d = 2.6
    n = int(d * RATE)
    inhale = noise_band(rnd, d, 300, 1800) * curve([(0, 0), (0.35, 0.7), (0.45, 0), (1, 0)], n)
    exhale = voice(rnd, d, [(0, 38), (1, 34)], "oo", "oo", growl=1.0, rattle=0.9, rattle_hz=14, breath=0.6) \
        * curve([(0, 0), (0.45, 0), (0.6, 0.8), (1, 0)], n)
    return room(inhale * 0.8 + exhale * 0.6, 0.4, 0.15, seed)


SOUNDS = {
    "roar": [lambda: roar(11), lambda: roar(12, 0.9)],
    "ambient": [lambda: ambient(21), lambda: ambient(22), lambda: ambient(23)],
    "hurt": [lambda: hurt(31), lambda: hurt(32), lambda: hurt(33)],
    "death": [lambda: death(41)],
    "charge": [lambda: charge(51), lambda: charge(52)],
    "breath": [lambda: breath(61), lambda: breath(62)],
    "slam": [lambda: slam(71), lambda: slam(72)],
    "snore": [lambda: snore(81), lambda: snore(82)],
}


def write_ogg(x, path, gain_db):
    x = finish(x, gain_db)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes((x * 32767).astype("<i2").tobytes())
        subprocess.run([os.environ.get("FFMPEG", "ffmpeg"), "-y", "-loglevel", "error", "-i", tmp.name, "-c:a", "libvorbis",
                        "-q:a", "4", str(path)], check=True)
    os.unlink(tmp.name)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, makers in SOUNDS.items():
        for i, make in enumerate(makers, 1):
            path = OUT / f"{name}{i}.ogg"
            write_ogg(make(), path, -3.0 if name in ("ambient", "snore") else -1.0)
            print("wrote", path.relative_to(OUT.parents[4]))


if __name__ == "__main__":
    main()
