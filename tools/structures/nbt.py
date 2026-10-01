"""A small NBT writer: just enough to save Minecraft structure templates (.nbt, gzipped)."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND = range(11)


class Int(int):
    pass


class Byte(int):
    pass


class Double(float):
    pass


class Float(float):
    pass


class List(list):
    """A list of one tag type (given, or guessed from the first element)."""
    def __init__(self, items=(), kind=None):
        super().__init__(items)
        self.kind = kind


def _kind(v):
    if isinstance(v, Byte) or isinstance(v, bool):
        return BYTE
    if isinstance(v, (Int, int)):
        return INT
    if isinstance(v, Float):
        return FLOAT
    if isinstance(v, (Double, float)):
        return DOUBLE
    if isinstance(v, str):
        return STRING
    if isinstance(v, list):
        return LIST
    if isinstance(v, dict):
        return COMPOUND
    raise TypeError(type(v))


def _payload(out, kind, v):
    if kind == BYTE:
        out += struct.pack(">b", int(v))
    elif kind == INT:
        out += struct.pack(">i", v)
    elif kind == DOUBLE:
        out += struct.pack(">d", v)
    elif kind == FLOAT:
        out += struct.pack(">f", v)
    elif kind == STRING:
        b = v.encode("utf-8")
        out += struct.pack(">H", len(b)) + b
    elif kind == LIST:
        item_kind = getattr(v, "kind", None) or (_kind(v[0]) if v else END)
        out += struct.pack(">bi", item_kind, len(v))
        for item in v:
            _payload(out, item_kind, item)
    elif kind == COMPOUND:
        for key, value in v.items():
            k = _kind(value)
            name = key.encode("utf-8")
            out += struct.pack(">bH", k, len(name)) + name
            _payload(out, k, value)
        out += b"\x00"


def write(path, root):
    out = bytearray()
    out += struct.pack(">bH", COMPOUND, 0)
    _payload(out, COMPOUND, root)
    # mtime=0 so the same structure always makes the same file (no spurious git changes).
    with open(path, "wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0, filename="") as f:
        f.write(bytes(out))
