"""Cuts the trailer together: the frames the game filmed (dev/ClientTrailer.java), the title cards,
the captions, the score (music.py) and the narration (narrate.py, spoken beforehand into
<out dir>/vo/), into an H.264 video.

    python3 tools/trailer/compose.py <frames dir> <font ascii.png> <out dir>

Writes <out dir>/the_astral_end_trailer.mp4, poster.png and stills/ (one picture per beat, to check
the edit without watching it). Needs numpy, Pillow and ffmpeg.
"""
import os
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).parent))
import music  # noqa: E402

W, H, FPS = 1280, 720, 24
SCENES = Path(__file__).with_name("scenes.txt")
FADE = 10                 # frames to fade to and from black around a card


# ---- the game's own font: assets/minecraft/textures/font/ascii.png, a 16x16 grid of 8x8 glyphs

class Font:
    def __init__(self, path):
        self.glyphs = {}
        self.widths = {}
        img = Image.open(path).convert("RGBA") if path and os.path.exists(path) else None
        cell = img.width // 16 if img else 8
        for c in range(32, 127):
            if img is None:
                break
            g = img.crop(((c % 16) * cell, (c // 16) * cell, (c % 16 + 1) * cell, (c // 16 + 1) * cell))
            a = np.array(g)[:, :, 3]
            cols = np.where(a.max(axis=0) > 0)[0]
            self.glyphs[chr(c)] = (a > 0)
            self.widths[chr(c)] = (cols.max() + 1) if len(cols) else 0
        self.widths[" "] = 3
        self.ok = img is not None
        self.cell = cell

    def width(self, text, scale):
        if not self.ok:
            return int(len(text) * 6 * scale)
        return sum((self.widths.get(ch, 5) + 1) * scale for ch in text) - scale

    def mask(self, text, scale, bold=False):
        """The text as a boolean mask, scale pixels per font pixel. Bold draws each glyph twice, one
        font pixel apart, as the game does."""
        if bold:
            m = self.mask(text, scale)
            out = np.zeros((m.shape[0], m.shape[1] + scale + text.count(" ") * 0 + len(text) * scale), bool)
            x = 0
            parts = []
            for ch in text:
                g = self.mask(ch, scale) if ch != " " else np.zeros((m.shape[0], 3 * scale), bool)
                gb = np.zeros((g.shape[0], g.shape[1] + scale), bool)
                gb[:, :g.shape[1]] |= g
                gb[:, scale:] |= g
                out[:, x:x + gb.shape[1]] |= gb
                x += gb.shape[1] + scale
            return out[:, :x - scale]
        if not self.ok:
            img = Image.new("L", (max(1, len(text) * 6), 8))
            ImageDraw.Draw(img).text((0, -1), text, fill=255)
            return np.array(img.resize((img.width * scale, img.height * scale), Image.NEAREST)) > 0
        m = np.zeros((self.cell * scale, max(1, self.width(text, scale))), bool)
        x = 0
        for ch in text:
            g = self.glyphs.get(ch)
            if g is not None and ch != " ":
                big = np.kron(g, np.ones((scale, scale), bool))
                w = min(big.shape[1], m.shape[1] - x)
                m[:, x:x + w] |= big[:, :w]
            x += (self.widths.get(ch, 5) + 1) * scale
        return m


def draw_text(frame, font, text, scale, x, y, color=(255, 255, 255), alpha=1.0, shadow=True):
    """Minecraft-style: the text with its shadow one font pixel down and right, at a quarter brightness."""
    if alpha <= 0:
        return
    m = font.mask(text, scale)
    for dx, dy, col in ([(scale, scale, tuple(c // 4 for c in color))] if shadow else []) + [(0, 0, color)]:
        x0, y0 = int(x + dx), int(y + dy)
        h, w = m.shape
        xs, ys = max(0, x0), max(0, y0)
        xe, ye = min(W, x0 + w), min(H, y0 + h)
        if xe <= xs or ye <= ys:
            continue
        sub = m[ys - y0:ye - y0, xs - x0:xe - x0]
        region = frame[ys:ye, xs:xe]
        region[sub] = region[sub] * (1 - alpha) + np.array(col) * alpha


def centred(frame, font, text, scale, y, color=(255, 255, 255), alpha=1.0):
    draw_text(frame, font, text, scale, (W - font.width(text, scale)) // 2, y, color, alpha)


# ---- the edit

def parse():
    beats = []
    vo = None
    for line in SCENES.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        head, _, caption = line.partition("|")
        words = head.split()
        if words[0] == "vo":
            vo = (float(words[1]) if len(words) > 1 else 0.0, caption.strip())
            continue
        if words[0] == "card":
            beats.append({"kind": "card", "frames": round(float(words[1]) * FPS), "text": caption.strip()})
        elif words[0] == "logo":
            beats.append({"kind": "logo", "frames": round(float(words[1]) * FPS)})
        else:
            beats.append({"kind": "shot", "shot": words[0], "frames": int(words[1]), "caption": caption.strip(),
                          "index": len([b for b in beats if b["kind"] == "shot"]),
                          "events": {w.split("@")[0]: int(w.split("@")[1]) for w in words[2:] if "@" in w}})
        if vo is not None:
            beats[-1]["vo"] = vo
            vo = None
    return beats


def narration(beats, starts):
    """(time in seconds, line, index) for every line the narrator says, in order."""
    out = []
    for b, s in zip(beats, starts):
        if "vo" in b:
            out.append((s / FPS + b["vo"][0], b["vo"][1], len(out)))
    return out


def mix_narration(track, beats, starts, vo_dir):
    """Lays the narrator's lines over the score, each where its beat says (later, if the line before
    is still being spoken), and ducks the score under the voice."""
    lines = narration(beats, starts)
    voice = np.zeros_like(track)
    busy_until = 0.0
    placed = 0
    for at, text, i in lines:
        path = Path(vo_dir) / f"{i:02d}.wav"
        if not path.exists():
            print(f"narration: no recording for line {i} ({text!r}); skipping it")
            continue
        with wave.open(str(path)) as w:
            data = np.frombuffer(w.readframes(w.getnframes()), "<i2").astype(np.float32) / 32768
            data = data.reshape(-1, w.getnchannels()).T
            if data.shape[0] == 1:
                data = np.vstack([data, data])
        if at < busy_until + 0.25:
            print(f"narration: line {i} ({text!r}) moved from {at:.1f} s to {busy_until + 0.25:.1f} s")
            at = busy_until + 0.25
        n0 = int(at * music.RATE)
        n1 = min(track.shape[1], n0 + data.shape[1])
        if n1 <= n0:
            continue
        voice[:, n0:n1] += data[:, :n1 - n0]
        busy_until = at + data.shape[1] / music.RATE
        placed += 1
    # duck: the score dips to 40% under the voice, easing down and back over a third of a second
    talking = (np.abs(voice).max(axis=0) > 1e-3).astype(np.float32)
    k = int(0.35 * music.RATE)
    kernel = np.ones(k, np.float32) / k
    envelope = np.convolve(np.convolve(talking, kernel, "same"), kernel, "same")
    envelope = np.clip(envelope * 1.6, 0, 1)
    out = track * (1 - 0.6 * envelope) + voice * 0.95
    peak = np.max(np.abs(out))
    print(f"narration: {placed} of {len(lines)} lines placed")
    return out / peak * 0.92 if peak > 0.92 else out


def frame_path(frames_dir, beat, i):
    return Path(frames_dir) / f"{beat['index']:02d}_{beat['shot']}_{i:04d}.png"


def load(path):
    img = Image.open(path).convert("RGB")
    if img.size != (W, H):
        img = img.resize((W, H), Image.LANCZOS)
    return np.asarray(img, dtype=np.float32)


def logo_layer(font):
    """THE ASTRAL END in the game's letters, built up like the Minecraft logo: a pale face over a
    stepped, darker extrusion, with a soft violet glow behind."""
    big, small = 13, 5
    face = font.mask("ASTRAL END", big, bold=True)
    the = font.mask("THE", small, bold=True)
    pad = 60
    h = face.shape[0] + the.shape[0] + 30 + pad * 2
    w = max(face.shape[1], the.shape[1]) + pad * 2
    rgba = np.zeros((h, w, 4), np.float32)

    def stamp(mask, x, y, rgb, a=1.0):
        mh, mw = mask.shape
        region = rgba[y:y + mh, x:x + mw]
        region[mask, :3] = rgb
        region[mask, 3] = a

    fx, fy = pad + (w - 2 * pad - face.shape[1]) // 2, pad + the.shape[0] + 30
    tx, ty = pad + (w - 2 * pad - the.shape[1]) // 2, pad
    # extrusion: 3 font-pixels deep, darkening as it goes back
    for k in range(3 * big, 0, -1):
        shade = 0.22 + 0.28 * (1 - k / (3 * big))
        stamp(face, fx + k // 3, fy + k, np.array([92, 64, 140]) * shade * 1.6)
    for k in range(3 * small, 0, -1):
        stamp(the, tx + k // 3, ty + k, np.array([92, 64, 140]) * 0.5)
    # the face: a vertical ramp from starlight white to End purple, with a few font-pixel speckles
    ramp_top, ramp_bottom = np.array([248, 240, 255]), np.array([176, 136, 232])
    ys = np.linspace(0, 1, face.shape[0])[:, None, None]
    colours = ramp_top * (1 - ys) + ramp_bottom * ys
    speck = np.kron((np.random.RandomState(3).rand(face.shape[0] // big + 1, face.shape[1] // big + 1) < 0.12),
                    np.ones((big, big)))[:face.shape[0], :face.shape[1], None]
    colours = colours * (1 - 0.08 * speck)
    region = rgba[fy:fy + face.shape[0], fx:fx + face.shape[1]]
    region[face, :3] = np.broadcast_to(colours, face.shape + (3,))[face]
    region[face, 3] = 1.0
    # a bright top edge on each letter, like light catching it
    top = face & ~np.vstack([np.zeros((big, face.shape[1]), bool), face[:-big]])
    region[top, :3] = np.minimum(255, region[top, :3] + 18)
    stamp(the, tx, ty, np.array([236, 226, 255]))
    # glow
    glow = Image.fromarray((rgba[:, :, 3] * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(28))
    g = np.asarray(glow, np.float32)[:, :, None] / 255 * 0.55
    out = np.zeros_like(rgba)
    out[:, :, :3] = np.array([120, 70, 220]) * g
    out[:, :, 3:] = g
    a = rgba[:, :, 3:]
    out[:, :, :3] = rgba[:, :, :3] * a + out[:, :, :3] * (1 - a)
    out[:, :, 3:] = a + out[:, :, 3:] * (1 - a)
    return out


def paste(frame, layer, cx, cy, alpha, scale=1.0):
    if alpha <= 0:
        return
    if scale != 1.0:
        img = Image.fromarray(np.clip(layer, 0, 255).astype(np.uint8) if layer.max() > 1.0 else layer)
    lh, lw = layer.shape[:2]
    if scale != 1.0:
        im = Image.fromarray(np.dstack([layer[:, :, :3], layer[:, :, 3:] * 255]).clip(0, 255).astype(np.uint8), "RGBA")
        im = im.resize((int(lw * scale), int(lh * scale)), Image.BICUBIC)
        arr = np.asarray(im, np.float32)
        layer = np.dstack([arr[:, :, :3], arr[:, :, 3:] / 255])
        lh, lw = layer.shape[:2]
    x0, y0 = int(cx - lw / 2), int(cy - lh / 2)
    xs, ys, xe, ye = max(0, x0), max(0, y0), min(W, x0 + lw), min(H, y0 + lh)
    sub = layer[ys - y0:ye - y0, xs - x0:xe - x0]
    a = sub[:, :, 3:] * alpha
    frame[ys:ye, xs:xe] = frame[ys:ye, xs:xe] * (1 - a) + sub[:, :, :3] * a


def ramp(i, n, length):
    """0..1 over the first `length` frames, 1..0 over the last `length`."""
    return max(0.0, min(1.0, (i + 1) / length, (n - i) / length))


def render(frames_dir, font_path, out_dir):
    out_dir = Path(out_dir)
    (out_dir / "stills").mkdir(parents=True, exist_ok=True)
    font = Font(font_path)
    beats = parse()
    total = sum(b["frames"] for b in beats)
    starts, t = [], 0
    for b in beats:
        starts.append(t)
        t += b["frames"]
    seconds = total / FPS

    # the score, timed to the edit
    def start_of(pred, default):
        for b, s in zip(beats, starts):
            if pred(b):
                return s / FPS
        return default
    cards = [s / FPS for b, s in zip(beats, starts) if b["kind"] == "card"]
    title = start_of(lambda b: b["kind"] == "logo", seconds - 8) + 0.25
    sections = {"bloom": start_of(lambda b: b["kind"] == "shot" and b["shot"] == "nebula_fields", 14.0),
                "pulse": start_of(lambda b: b["kind"] == "card" and b["text"].startswith("New life"),
                                  cards[2] if len(cards) > 2 else 40.0),
                "lift": start_of(lambda b: b["kind"] == "card" and b["text"].startswith("Don't wake"),
                                 cards[4] if len(cards) > 4 else title - 30),
                "title": title}
    print("score sections:", {k: round(v, 1) for k, v in sections.items()}, "length", round(seconds, 1))
    track = music.score(seconds, sections)
    # the Spire Wyrm's roar as it wakes: a boom on the frame it wakes, a riser into it
    for b, st in zip(beats, starts):
        if b["kind"] == "shot" and "wyrm_wake" in b.get("events", {}):
            at = (st + b["events"]["wyrm_wake"]) / FPS + 0.2
            music.place(track, music.riser(2.0) * 0.5, max(0.0, at - 2.0))
            music.place(track, music.boom() * 1.2, at)
            print(f"the Wyrm wakes at {at:.1f} s")
    track = mix_narration(track, beats, starts, out_dir / "vo")
    wav = out_dir / "score.wav"
    music.write(str(wav), track)

    mp4 = out_dir / "the_astral_end_trailer.mp4"
    ffmpeg = os.environ.get("FFMPEG", "ffmpeg")
    enc = subprocess.Popen([ffmpeg, "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}",
                            "-r", str(FPS), "-i", "-", "-i", str(wav), "-c:v", "libx264", "-preset", "slow", "-crf", "20",
                            "-maxrate", "3200k", "-bufsize", "6400k", "-g", "48",
                            "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart",
                            str(mp4)], stdin=subprocess.PIPE)
    logo = logo_layer(font)
    last = np.zeros((H, W, 3), np.float32)
    missing = 0
    for bi, (b, s) in enumerate(zip(beats, starts)):
        n = b["frames"]
        prev_kind = beats[bi - 1]["kind"] if bi else "card"
        next_kind = beats[bi + 1]["kind"] if bi + 1 < len(beats) else "card"
        backdrop = None
        if b["kind"] == "logo":
            backdrop = np.asarray(Image.fromarray(last.astype(np.uint8)).filter(ImageFilter.GaussianBlur(6)), np.float32) * 0.35
        for i in range(n):
            if b["kind"] == "shot":
                p = frame_path(frames_dir, b, i)
                if p.exists():
                    frame = load(p)
                    last = frame.copy()
                else:
                    missing += 1
                    frame = last.copy()
                fade = 1.0
                if prev_kind != "shot":
                    fade = min(fade, (i + 1) / FADE)
                if next_kind != "shot":
                    fade = min(fade, (n - i) / FADE)
                frame *= max(0.0, min(1.0, fade))
                if b["caption"]:
                    a = ramp(i - 10, n - 16, 8)
                    draw_text(frame, font, b["caption"], 3, 46, H - 70, alpha=a)
            elif b["kind"] == "card":
                frame = np.zeros((H, W, 3), np.float32)
                a = ramp(i, n, 14)
                lines = b["text"].split("\\")
                for k, line in enumerate(lines):
                    centred(frame, font, line.strip(), 4, H // 2 - 18 - (len(lines) - 1) * 22 + k * 44, alpha=a)
            else:
                into = i / FPS
                frame = backdrop * min(1.0, into / 0.8) if backdrop is not None else np.zeros((H, W, 3), np.float32)
                a = min(1.0, max(0.0, (into - 0.25) / 0.6)) * min(1.0, (n - i) / 20)
                paste(frame, logo, W / 2, H / 2 - 40, a, scale=1.0 + 0.04 * max(0.0, 1 - into / 3))
                sub = min(1.0, max(0.0, (into - 1.6) / 0.8)) * min(1.0, (n - i) / 20)
                centred(frame, font, "A free update to the End, for Minecraft Forge 26.3", 2, H // 2 + 120,
                        color=(200, 200, 210), alpha=sub)
            frame = np.clip(frame, 0, 255).astype(np.uint8)
            enc.stdin.write(frame.tobytes())
            if i == n // 2:
                Image.fromarray(frame).save(out_dir / "stills" / f"{bi:02d}_{b.get('shot', b['kind'])}.png")
            elif b["kind"] == "shot" and i % 24 == 12 and any(k in b["shot"] for k in ("wyrm", "whale", "comet_wake")):
                # moving things, a picture a second, to judge them in motion
                Image.fromarray(frame).save(out_dir / "stills" / f"{bi:02d}_{b['shot']}_f{i:03d}.png")
            if b["kind"] == "logo" and i == int(FPS * 3.5):
                Image.fromarray(frame).save(out_dir / "poster.png")
    enc.stdin.close()
    enc.wait()
    print(f"wrote {mp4} ({total} frames, {seconds:.1f} s); {missing} filmed frames were missing")
    return enc.returncode


if __name__ == "__main__":
    sys.exit(render(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None, sys.argv[3] if len(sys.argv) > 3 else "trailer"))
