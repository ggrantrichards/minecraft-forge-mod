"""Each biome's ambience, synthesised from scratch: a seamless background loop, and a few short sounds
the game drops in now and then ("additions"), as vanilla's Nether biomes have.

    python3 tools/sounds/ambience.py [biome...]   -> src/main/resources/assets/astralend/sounds/ambient/<biome>/*.ogg

Needs numpy, scipy and ffmpeg (FFMPEG names another binary). Quiet by design: ambience sits under
everything else.
"""
import os
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np
from scipy import signal

RATE = 44100
LOOP_SECONDS = 24
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/astralend/sounds/ambient"


def band(x, lo, hi, order=2):
    b, a = signal.butter(order, [lo / (RATE / 2), hi / (RATE / 2)], "band")
    return signal.lfilter(b, a, x)


def low(x, hz, order=2):
    b, a = signal.butter(order, hz / (RATE / 2))
    return signal.lfilter(b, a, x)


def slow(rnd, n, rate_hz, depth=1.0):
    """A slow random swell in [1-depth, 1], periodic over the loop."""
    k = max(2, int(LOOP_SECONDS * rate_hz))
    pts = rnd.uniform(0, 1, k)
    pts = np.append(pts, pts[0])
    t = np.linspace(0, k, n, endpoint=False)
    i = t.astype(int)
    f = t - i
    f = f * f * (3 - 2 * f)
    v = pts[i] * (1 - f) + pts[i + 1] * f
    return 1 - depth + depth * v


def seamless(x, fade=2.0):
    """Make a loop: the last `fade` seconds cross-fade into the first ones."""
    f = int(fade * RATE)
    n = len(x) - f
    out = x[:n].copy()
    ramp = np.linspace(0, 1, f)
    out[:f] = x[:f] * ramp + x[n:] * (1 - ramp)
    return out


def tone(freq, dur, decay, rnd=None, partials=((1, 1.0),)):
    n = int(dur * RATE)
    t = np.arange(n) / RATE
    y = np.zeros(n)
    for mult, amp in partials:
        y += amp * np.sin(2 * np.pi * freq * mult * t + (rnd.uniform(0, 6) if rnd is not None else 0))
    return y * np.exp(-t / decay) * np.minimum(1, t / 0.005)


def place(dst, src, at):
    end = min(len(dst), at + len(src))
    dst[at:end] += src[:end - at]


# ---- loops (each LOOP_SECONDS + 2 s, then made seamless)

def loop_nebula(rnd):
    n = (LOOP_SECONDS + 2) * RATE
    air = band(rnd.normal(0, 1, n), 300, 2400) * slow(rnd, n, 0.15, 0.7) * 0.35
    t = np.arange(n) / RATE
    pad = sum(np.sin(2 * np.pi * f * t + rnd.uniform(0, 6)) * slow(rnd, n, 0.08, 0.9) for f in (220.0, 277.2, 329.6, 440.0)) * 0.12
    return air + pad


def loop_thicket(rnd):
    n = (LOOP_SECONDS + 2) * RATE
    rustle = band(rnd.normal(0, 1, n), 900, 5000) * slow(rnd, n, 0.6, 0.85) * 0.4
    hum = low(rnd.normal(0, 1, n), 180) * 1.5 * slow(rnd, n, 0.1, 0.5)
    return rustle + hum


def loop_ashen(rnd):
    n = (LOOP_SECONDS + 2) * RATE
    gusts = slow(rnd, n, 0.25, 0.9) ** 2
    wind = band(rnd.normal(0, 1, n), 150, 1400) * gusts * 0.6
    hiss = band(rnd.normal(0, 1, n), 3000, 8000) * gusts * 0.08
    return wind + hiss


def loop_comet(rnd):
    n = (LOOP_SECONDS + 2) * RATE
    noise = rnd.normal(0, 1, n)
    # A cold whistle: noise through a resonance whose pitch drifts slowly.
    centre = 900 + 500 * slow(rnd, n, 0.12, 1.0)
    out = np.zeros(n)
    block = 2048
    zi = None
    for s in range(0, n, block):
        f0 = float(np.mean(centre[s:s + block]))
        b, a = signal.iirpeak(f0 / (RATE / 2), 18)
        if zi is None:
            zi = signal.lfilter_zi(b, a) * 0
        out[s:s + block], zi = signal.lfilter(b, a, noise[s:s + block], zi=zi)
    return out * 0.5 * slow(rnd, n, 0.2, 0.7) + band(noise, 200, 1200) * 0.15


def loop_hollow(rnd):
    n = (LOOP_SECONDS + 2) * RATE
    t = np.arange(n) / RATE
    hum = (np.sin(2 * np.pi * 41.0 * t) + 0.8 * np.sin(2 * np.pi * 41.6 * t) + 0.4 * np.sin(2 * np.pi * 82.3 * t)) * 0.4
    void = low(rnd.normal(0, 1, n), 300) * 2.0 * slow(rnd, n, 0.08, 0.8)
    return hum * slow(rnd, n, 0.05, 0.4) + void


def loop_corona(rnd):
    n = (LOOP_SECONDS + 2) * RATE
    roar = low(np.cumsum(rnd.normal(0, 1, n)) * 0.02, 400)
    roar -= low(roar, 20)
    crackle = np.zeros(n)
    for _ in range(int(LOOP_SECONDS * 40)):
        at = rnd.integers(0, n - 400)
        crackle[at:at + 300] += rnd.normal(0, 1, 300) * np.exp(-np.arange(300) / 40) * rnd.uniform(0.2, 1.0)
    return roar * 3.0 * slow(rnd, n, 0.15, 0.4) + band(crackle, 1500, 9000) * 0.25


def loop_crags(rnd):
    n = (LOOP_SECONDS + 2) * RATE
    out = band(rnd.normal(0, 1, n), 200, 1500) * slow(rnd, n, 0.2, 0.7) * 0.2
    notes = [523.3, 659.3, 784.0, 987.8, 1174.7, 1568.0]
    for _ in range(int(LOOP_SECONDS * 0.9)):
        f = notes[rnd.integers(len(notes))]
        place(out, tone(f, 3.5, 1.1, rnd, ((1, 1.0), (2.76, 0.4), (5.4, 0.15))) * rnd.uniform(0.05, 0.14), rnd.integers(0, n - RATE))
    return out


def loop_deep_hollows(rnd):
    """The Hollow Isles' hum, lower and closer, with the forest's slow creak under it."""
    n = (LOOP_SECONDS + 2) * RATE
    t = np.arange(n) / RATE
    hum = (np.sin(2 * np.pi * 33.0 * t) + 0.8 * np.sin(2 * np.pi * 33.5 * t) + 0.3 * np.sin(2 * np.pi * 66.2 * t)) * 0.45
    void = low(rnd.normal(0, 1, n), 220) * 2.4 * slow(rnd, n, 0.06, 0.85)
    return hum * slow(rnd, n, 0.04, 0.5) + void


def loop_shattered(rnd):
    """Wind through the gaps between the fragments, and now and then a far crystal note."""
    n = (LOOP_SECONDS + 2) * RATE
    gusts = slow(rnd, n, 0.3, 0.95) ** 2
    out = band(rnd.normal(0, 1, n), 250, 2200) * gusts * 0.5
    for _ in range(int(LOOP_SECONDS * 0.3)):
        f = rnd.choice([392.0, 523.3, 587.3, 784.0])
        place(out, tone(f, 4.0, 1.4, rnd, ((1, 1.0), (2.76, 0.3))) * rnd.uniform(0.03, 0.08), rnd.integers(0, n - 2 * RATE))
    return out


def loop_eclipse(rnd):
    """The heaven half: running water, and a warm open chord that swells and fades like its light."""
    n = (LOOP_SECONDS + 2) * RATE
    t = np.arange(n) / RATE
    water = band(rnd.normal(0, 1, n), 400, 5000) * (0.5 + 0.5 * slow(rnd, n, 0.8, 0.6)) * 0.18
    chord = sum(np.sin(2 * np.pi * f * t + rnd.uniform(0, 6)) * slow(rnd, n, 0.06, 0.8)
                for f in (196.0, 246.9, 293.7, 392.0)) * 0.1
    return water + chord


# ---- additions: short sounds dropped in now and then

def chime(rnd):
    f = rnd.choice([659.3, 784.0, 880.0, 1046.5, 1318.5])
    return tone(f, 3.0, 0.9, rnd, ((1, 1.0), (2.0, 0.3), (3.01, 0.15)))


def pop(rnd):
    n = int(0.4 * RATE)
    x = band(rnd.normal(0, 1, n), 400, 3000) * np.exp(-np.arange(n) / (0.03 * RATE))
    return x + tone(rnd.uniform(180, 260), 0.4, 0.06, rnd)


def ash(rnd):
    n = int(1.6 * RATE)
    return band(rnd.normal(0, 1, n), 2000, 7000) * np.hanning(n) ** 2


def creak(rnd):
    n = int(1.2 * RATE)
    t = np.arange(n) / RATE
    grains = (np.sin(2 * np.pi * (300 + 200 * t) * t) * (rnd.uniform(0, 1, n) > 0.97)).astype(float)
    return band(grains, 800, 6000, 1) * np.exp(-t / 0.5) * 3 + tone(rnd.uniform(1800, 2600), 1.2, 0.3, rnd) * 0.3


def moan(rnd):
    n = int(4.0 * RATE)
    t = np.arange(n) / RATE
    f = rnd.uniform(70, 110) * (1 + 0.3 * np.sin(np.pi * t / 4.0))
    x = np.sin(2 * np.pi * np.cumsum(f) / RATE) + 0.4 * np.sin(4 * np.pi * np.cumsum(f) / RATE)
    return low(x, 600) * np.hanning(n)


def fizz(rnd):
    n = int(0.9 * RATE)
    return band(rnd.normal(0, 1, n), 1200, 8000) * np.exp(-np.arange(n) / (0.2 * RATE)) * (1 + rnd.normal(0, 1, n) * 0.3)


BIOMES = {
    "nebula_fields": (loop_nebula, chime),
    "chorus_thicket": (loop_thicket, pop),
    "ashen_barrens": (loop_ashen, ash),
    "hollow_isles": (loop_hollow, moan),
    "corona": (loop_corona, fizz),
    "glimmering_crags": (loop_crags, chime),
    "deep_hollows": (loop_deep_hollows, creak),
    "shattered_crags": (loop_shattered, chime),
    "eclipse": (loop_eclipse, chime),
}


def void_hum():
    """Near an island's edge: a low drone with a slow beat, and a faint high whine over it."""
    rnd = np.random.default_rng(7)
    n = (LOOP_SECONDS + 2) * RATE
    t = np.arange(n) / RATE
    drone = np.sin(2 * np.pi * 36.0 * t) + 0.7 * np.sin(2 * np.pi * 36.7 * t) + 0.35 * np.sin(2 * np.pi * 72.4 * t)
    air = low(rnd.normal(0, 1, n), 160) * 1.6 * slow(rnd, n, 0.1, 0.6)
    whine = np.sin(2 * np.pi * 1410.0 * t) * 0.012 * slow(rnd, n, 0.07, 0.9)
    return seamless(drone * 0.5 + air + whine)


def write_ogg(x, path, peak_db):
    b, a = signal.butter(2, 30 / (RATE / 2), "high")
    x = signal.lfilter(b, a, x)
    x = x / (np.max(np.abs(x)) + 1e-9) * 10 ** (peak_db / 20)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes((x * 32767).astype("<i2").tobytes())
        subprocess.run([os.environ.get("FFMPEG", "ffmpeg"), "-y", "-loglevel", "error", "-i", tmp.name, "-c:a", "libvorbis",
                        "-q:a", "2", str(path)], check=True)
    os.unlink(tmp.name)


def main(only=()):
    if not only or "void_hum" in only:
        write_ogg(void_hum(), OUT / "void_hum.ogg", -8.0)
        print("wrote void_hum")
    for i, (biome, (loop, addition)) in enumerate(BIOMES.items()):
        if only and biome not in only:
            continue
        rnd = np.random.default_rng(100 + i)
        folder = OUT / biome
        folder.mkdir(parents=True, exist_ok=True)
        write_ogg(seamless(loop(rnd)), folder / "loop.ogg", -9.0)
        for k in range(1, 4):
            write_ogg(addition(rnd), folder / f"additions{k}.ogg", -6.0)
        print("wrote", folder.relative_to(OUT.parents[4]))


if __name__ == "__main__":
    import sys
    main(sys.argv[1:])     # name biomes to write only those
