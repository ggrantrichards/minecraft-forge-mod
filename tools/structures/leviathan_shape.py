"""A Leviathan's shape, in its own frame: w along it (head toward +w), u across, v up - the Python mirror
of LeviathanShape.java, line by line, for previewing designs (leviathan_preview.py). Change both together."""
import math


M64 = (1 << 64) - 1


def mix(h):
    """SplitMix64's finaliser: a well-spread number in [0, 1) for any 64-bit input."""
    z = h & M64
    z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & M64
    z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & M64
    z ^= z >> 31
    return (z >> 11) * 2.0 ** -53


class Leviathan:
    TAIL, SKULL_W, SNOUT = -120, 20, 80

    def __init__(self, seed):
        self.seed = seed
        r = lambda k: mix(seed + (k + 1) * 0x9E3779B97F4A7C15)
        self.r = r
        self.tail_amp = (r(1) * 2 - 1) * 16          # the tail curls off to one side
        self.tail_droop = 4 + r(2) * 8
        self.tail_break = -96 - int(r(3) * 12)        # where the tail snapped
        self.hall_from = -64 + int(r(4) * 16)
        self.collapse = -40 + int(r(5) * 40)          # a fallen-in stretch of the hall's roof
        slots = [-58, -40, -14, 6]
        self.maze_tower = slots[0] if r(6) < 0.5 else slots[1]
        self.towers = [s for i, s in enumerate(slots) if s != self.maze_tower and (r(10 + i) < 0.6 or s == -14)]
        self.hall_from = min(self.hall_from, self.maze_tower - 6)
        self.lungs = [r(7) > 0.25, r(8) > 0.25]       # one may have rotted away
        self.ribs = []                                 # (w, side, reach in degrees or 0 if missing)
        for i in range(10):
            w = -56 + 7 * i
            for side in (-1, 1):
                q = r(100 + i * 2 + (side > 0))
                reach = 0 if q < 0.15 else 70 + (q - 0.15) / 0.25 * 60 if q < 0.4 else 165
                self.ribs.append((w, side, reach))

    # ---- the spine
    @staticmethod
    def sv(w):
        return 44 + 2.5 * math.sin((w + 120) / 35.0)

    def spine_at(self, w):
        """(u, v) of the spine's middle at w: the tail curls and droops past w = -60."""
        u, v = 0.0, self.sv(w)
        if w < -60:
            t = (-60 - w) / 60.0
            u += self.tail_amp * t * t
            v -= self.tail_droop * t * t
        return u, v

    def hash(self, a, b, c):
        return mix(self.seed ^ (a * 0x9E3779B97F4A7C15 + b * 0xC2B2AE3D27D4EB4F + c * 0x165667B19E3779F9))

    # the cranium's frame
    def b(self):
        return math.floor(self.sv(self.SKULL_W)) - 12

    def block(self, u, v, w):
        m = self.raw(u, v, w)
        if m != "bone":
            return m
        above = self.raw(u, v + 1, w)
        if above in (None, "air") and self.patches(u, v, w, 31) > 0.58:
            return "lichened"
        if self.patches(u, v, w, 17) > (0.48 if w < -60 else 0.6):
            return "weathered"
        return m

    def patches(self, u, v, w, salt):
        x, y, z = u / 5.0, v / 5.0, w / 5.0
        x0, y0, z0 = math.floor(x), math.floor(y), math.floor(z)
        fx, fy, fz = x - x0, y - y0, z - z0
        fx, fy, fz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
        lerp = lambda t, a, b: a + (b - a) * t
        hz = lambda dz: z0 + dz + salt * 1000
        c00 = lerp(fx, self.hash(x0, y0, hz(0)), self.hash(x0 + 1, y0, hz(0)))
        c10 = lerp(fx, self.hash(x0, y0 + 1, hz(0)), self.hash(x0 + 1, y0 + 1, hz(0)))
        c01 = lerp(fx, self.hash(x0, y0, hz(1)), self.hash(x0 + 1, y0, hz(1)))
        c11 = lerp(fx, self.hash(x0, y0 + 1, hz(1)), self.hash(x0 + 1, y0 + 1, hz(1)))
        return lerp(fz, lerp(fy, c00, c10), lerp(fy, c01, c11))

    def raw(self, u, v, w):
        res = None
        if w >= self.SKULL_W - 1:
            res = self.skull(u, v, w - self.SKULL_W)
            if res is not None or w >= self.SKULL_W + 3:
                return res
        return self.body(u, v, w)

    # ---------------------------------------------------------------- the body
    def body(self, u, v, w):
        if w < self.TAIL:
            return None
        su, svv = self.spine_at(w)
        sv = self.sv(w)
        # the citadel on its back
        for t in self.towers + [self.maze_tower]:
            rho = max(abs(u), abs(w - t))                 # square, as the End City's towers are
            height = 24 if t == self.maze_tower else 16 + int(self.r(200 + t) * 6)
            if rho <= 4 and sv + 4 <= v <= sv + height + 1:
                return self.tower(u, v - sv, w - t, rho, height, t == self.maze_tower)
        if self.hall_from <= w <= self.SKULL_W + 1 and abs(u) <= 4 and sv + 4 <= v < sv + 11:
            return self.hall(u, v - sv, w)
        # the spine
        if w > self.tail_break - 3 and w < self.tail_break:
            return None                                   # where the tail snapped off
        du, dv = u - su, v - svv
        k = w - 6 * round(w / 6.0)
        rc = 2 if w > -60 else 1
        if w > -90:
            if abs(du) <= 1 and abs(dv) <= 1:
                return "bone"
        elif round(du) == 0 and round(dv) == 0:
            return "bone"
        if abs(k) <= 1 and abs(du) <= rc and abs(dv) <= rc:
            return "bone"
        under_hall = self.hall_from <= w <= self.SKULL_W
        top = 3 if under_hall else (4 if w > -60 else 2)
        if abs(k) < 0.5 and abs(du) < 0.5 and rc < dv <= rc + top:
            return "bone"                                 # the spinous process
        if abs(k) < 0.5 and abs(dv) < 0.5 and rc < abs(du) <= rc + 3 and w > -90:
            return "bone"                                 # the side processes
        # the ribs, and the reliquary they hold
        if -58 <= w <= 9:
            r = self.rib(u, v, w, sv)
            if r is not None:
                return r
            inside = self.inside_cage(u, v, w, sv)
            if inside:
                o = self.organ(u, v, w, sv)
                if o is not None:
                    return o
                floor = math.floor(self.sv(-24)) - 19
                if v == floor and abs(u) <= 9 and -54 <= w <= 6:
                    return "air" if self.gap(u, w) else ("endbrick" if abs(u) == 9 or w in (-54, 6) else "purpur")
                if v == floor + 1 and abs(u) == 9 and -54 <= w <= 6 and (w - 2) % 7 != 0:
                    return "endbrick_wall"
        return None

    def rib_half(self, w):
        return 9 + 4 * math.cos((w + 24) / 40.0 * math.pi / 2)   # widest round the middle

    def rib(self, u, v, w, sv):
        for (rw, side, reach) in self.ribs:
            if abs(w - rw) > 1 or reach == 0 or u * side < 0:
                continue
            a, bb, cv = self.rib_half(rw), 11.5, sv - 10
            e = math.hypot(u / a, (v - cv) / bb)
            if abs(e - 1) <= 1.3 / a:
                ang = math.degrees(math.atan2(abs(u) / a, (v - cv) / bb))
                if ang <= reach:
                    return "bone"
        return None

    def inside_cage(self, u, v, w, sv):
        a, bb, cv = self.rib_half(w), 11.5, sv - 10
        return -54 <= w <= 6 and math.hypot(u / a, (v - cv) / bb) < 1 - 1.3 / a

    def gap(self, u, w):
        protected = (abs(u) < 5 and abs(w + 26) < 6) or (abs(u) < 5 and w > -8) or (abs(u + 2) < 3 and abs(w + 34) < 3) \
            or (w < -42)
        for pu, pw in ((-6, -33), (6, -33), (-6, -19), (6, -19)):
            if abs(u - pu) < 3 and abs(w - pw) < 3:
                protected = True
        return not protected and self.hash(math.floor(u / 3), math.floor(w / 3), 77) < 0.15

    def organ(self, u, v, w, sv):
        # the heart: two lobes hung from the spine over the pedestal
        hv = sv - 8
        h1 = math.sqrt(u * u + (v - hv) ** 2 + (w + 2) ** 2) / 3.6
        h2 = math.sqrt((u - 1.5) ** 2 + (v - (hv - 2)) ** 2 + ((w + 4) ** 2) * 1.2) / 2.7
        h = min(h1, h2)
        if h <= 1.0:
            return "heart" if h <= 0.62 or self.hash(u, v, w) < 0.25 else "flesh"
        # its vessels: up into the spine and out to the ribs
        for (a, b) in (((0, hv + 2, -2), (0, sv - 1, 0)), ((-2, hv - 1, -2), (-9, sv - 14, 5)), ((2, hv - 1, -2), (9, sv - 14, 5)),
                       ((-1.5, hv - 2, -4), (-10, sv - 6, -12)), ((1.5, hv - 2, -4), (10, sv - 6, -12))):
            if self.cone(u, v, w, a, b, 1.0):
                return "flesh"
        # the lungs
        for i, s in enumerate((-1, 1)):
            if self.lungs[i]:
                q = ((u - s * 6) / 2.6) ** 2 + ((v - (sv - 5)) / 2.6) ** 2 + ((w + 26) / 14.0) ** 2
                if q <= 1:
                    return "breathcomb"
        # the stomach, low at the back: a sac of Voidgel with what it swallowed
        st = math.sqrt((u / 4.5) ** 2 + ((v - (math.floor(self.sv(-24)) - 15)) / 3.8) ** 2 + ((w + 48) / 5.0) ** 2)
        if st <= 1:
            return "voidgel" if st >= 0.75 else "air"
        return None

    @staticmethod
    def cone(u, v, w, a, b, r):
        ex, ey, ez = b[0] - a[0], b[1] - a[1], b[2] - a[2]
        ln = ex * ex + ey * ey + ez * ez
        t = max(0, min(1, ((u - a[0]) * ex + (v - a[1]) * ey + (w - a[2]) * ez) / ln))
        dx, dy, dz = u - (a[0] + ex * t), v - (a[1] + ey * t), w - (a[2] + ez * t)
        return dx * dx + dy * dy + dz * dz <= r * r

    def hall(self, u, h, w):
        """The End City hall: purpur floor, End Stone brick walls with purple glass, a purpur roof."""
        fallen = self.collapse <= w <= self.collapse + 5
        if h < 5:
            return "purpur"
        if fallen and h >= 7:
            return "air" if abs(u) < 4 or self.hash(u, int(h), w) < 0.6 else "endbrick"
        if h >= 10:
            return "purpur_slab" if abs(u) <= 4 else None
        if abs(u) >= 4:
            window = w % 6 in (2, 3) and 7 <= h < 9
            return "glass" if window else "endbrick"
        return "air"

    def tower(self, u, h, dw, rho, height, maze):
        if h >= height:
            if h < height + 1:
                return "purpur" if maze or max(abs(u), abs(dw)) == 4 else "purpur_slab"
            return "air"
        if rho == 4:
            if 5 <= h < 8 and abs(u) <= 1:
                return "air"                                    # its doors, onto the hall
            corner = abs(u) == 4 and abs(dw) == 4
            window = (int(h) % 6 == 3) and (u == 0 or dw == 0)
            return "purpur_pillar" if corner else "glass" if window else "purpur"
        if h < 5 or (int(h) % 6 == 4 and h > 8 and not (u == 2 and dw == 2)):
            return "purpur"
        return "air"

    # ---------------------------------------------------------------- the skull
    def skull(self, u, v, s):
        """s = w - SKULL_W: the back of the cranium at 0, the snout's tip near 60."""
        b = self.b()
        cv = b + 11
        res = None
        # the cranium: a boxy shell (a squarish superellipse), hollow
        q = (u / 13.0) ** 4 + ((v - cv) / 11.0) ** 4 + ((s - 12) / 13.0) ** 4
        qi = (u / 11.0) ** 4 + ((v - cv) / 9.0) ** 4 + ((s - 12) / 11.0) ** 4
        if q <= 1:
            if qi > 1:
                res = "bone" if self.hash(u, v, s) > 0.02 else "air"
            else:
                res = "bone" if v <= b + 3 else "air"
                if v == cv + 2 and abs(u) >= 8 and 7 <= s <= 23:
                    res = "bone"                                # the ledge to the eyes
        # brow ridges and cheekbones
        if 12 <= abs(u) <= 14 and cv + 6 <= v <= cv + 7 and 7 <= s <= 24:
            res = "bone"
        if 12 <= abs(u) <= 13 and cv - 3 <= v <= cv - 2 and 12 <= s <= 25:
            res = "bone"
        # a crest down the crown
        if abs(u) <= 1 and cv + 10 <= v <= cv + 12 and 1 <= s <= 18:
            res = "bone"
        # the snout
        if 22 <= s <= 60:
            f = (s - 22) / 38.0
            hw = 11 - 5 * f
            if s > 56:
                hw *= math.sqrt(max(0, 1 - ((s - 56) / 4.5) ** 2))
            tv = cv + 5 - 6 * f
            lip = tv - 6 + 2 * f
            if abs(u) <= hw:
                if tv - 2 <= v <= tv or (abs(u) >= hw - 2 and lip <= v <= tv):
                    res = "bone"
                elif lip < v < tv - 2:
                    res = "air"
            # the nostrils
            if 49 <= s <= 51 and 2 <= abs(u) <= 3 and tv - 2 <= v <= tv:
                res = "obsidian" if v == tv - 2 else "air"
            # upper teeth
            if abs(u) <= hw and abs(u) >= hw - 1.5 and (s - 25) % 3 == 0 and s < 58:
                length = 3 + int(self.hash(int(u > 0), 1, s) * 3) + (4 if 49 <= s <= 52 else 0)
                if lip - length <= v < lip:
                    res = "bone"
        # the throat: a disc of dark stone closing the back of the mouth, in a wall of bone
        t = u * u + (v - (b + 6)) ** 2
        if 22 <= s <= 23 and b + 1 <= v <= cv + 1 and abs(u) <= 11:
            res = "throat" if t <= 4.5 ** 2 else "bone"
        # the tongue-bone: a ledge in the mouth to stand on
        if abs(u) <= 3 and b - 1 <= v <= b and 24 <= s <= 36:
            res = "bone"
        # the lower jaw, hanging open
        if 1 <= s <= 58:
            jv = b + 1 - 0.32 * (s - 1)
            hwj = 11 - 4 * (s - 1) / 57.0
            if jv - 3 <= v <= jv and (hwj - 3 <= abs(u) <= hwj or (s >= 55 and abs(u) <= hwj)):
                res = "bone"
            if abs(u) >= hwj - 1.5 and abs(u) <= hwj and (s - 8) % 3 == 0 and s < 54:
                length = 2 + int(self.hash(int(u > 0), 2, s) * 3)
                if jv < v <= jv + length:
                    res = "bone"
        # the eye sockets, two each side, rimmed in obsidian
        for (s0, s1, v0, v1) in ((16, 22, cv + 1, cv + 5), (9, 13, cv + 1, cv + 4)):
            if 9 <= abs(u) <= 13:
                if s0 <= s <= s1 and v0 <= v <= v1:
                    res = "air"
                elif s0 - 1 <= s <= s1 + 1 and v0 - 1 <= v <= v1 + 1 and abs(u) >= 11 and q <= 1.0 and res == "bone":
                    res = "obsidian"
        # the way in at the back, where the spine and the hall come in
        if s <= 3 and abs(u) <= 2:
            svv = self.sv(self.SKULL_W)
            if svv - 2 <= v <= svv + 1 or svv + 4 <= v <= svv + 8:
                res = "air"
        return res

    def eyes(self):
        b = self.b()
        cv = b + 11
        return [(s * 10, cv + 3, self.SKULL_W + 19) for s in (-1, 1)] + [(s * 10, cv + 2, self.SKULL_W + 11) for s in (-1, 1)]

    def throat(self):
        return (0, self.b() + 6, self.SKULL_W + 22)
