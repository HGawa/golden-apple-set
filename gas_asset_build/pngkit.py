"""最小 PNG 读写工具（纯 Python 标准库，无 Pillow 依赖）。

读：支持 color type 0/2/3/4/6、bit depth 8、filter 0~4、非隔行。
写：固定写 8bit RGBA（color type 6），逐行 filter=0，zlib 压缩。
颜色类型 3（索引）自动解到 tRNS 定义的调色板。

用法::

    rows = pngkit.read(path)                 # -> List[List[Tuple[int,int,int,int]]]
    pngkit.write(path, rows)                # rows 为 (r,g,b,a)
    chars = pngkit.to_chars(rows, table)    # -> 字符画
    rows  = pngkit.from_chars(chars, table) # <- 字符画
"""
from __future__ import annotations

import os
import struct
import zlib
from typing import Dict, List, Sequence, Tuple

Pixel = Tuple[int, int, int, int]
Grid = List[List[Pixel]]

#: bit depth 8 下各 color type 的每像素字节数
_CHANNELS = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}

_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def _paeth(a: int, b: int, c: int) -> int:
    """Paeth 预测器（PNG spec 定义）。"""
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def read(path: str) -> Grid:
    """读取 PNG 为 RGBA 网格（16x16 等小图直接调用，性能足够）。"""
    with open(path, "rb") as fh:
        data = fh.read()
    if data[:8] != _SIGNATURE:
        raise ValueError(f"{path}: 不是合法 PNG（魔数错误）")

    pos = 8
    width = height = bit_depth = color_type = 0
    idat = bytearray()
    palette: List[Pixel] = []
    trns: List[int] = []

    while pos + 8 <= len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if tag == b"IHDR":
            width, height, bit_depth, color_type, _c, _f, _i = struct.unpack(
                ">IIBBBBB", body
            )
        elif tag == b"IDAT":
            idat += body
        elif tag == b"PLTE":
            palette = [
                (body[i * 3], body[i * 3 + 1], body[i * 3 + 2], 255)
                for i in range(len(body) // 3)
            ]
        elif tag == b"tRNS":
            trns = list(body)
        elif tag == b"IEND":
            break

    if bit_depth != 8:
        raise ValueError(f"{path}: 仅支持 bit depth 8，实际 {bit_depth}")
    bpp = _CHANNELS.get(color_type)
    if bpp is None:
        raise ValueError(f"{path}: 不支持的 color type {color_type}")

    raw = zlib.decompress(bytes(idat))
    stride = width * bpp
    if len(raw) < height * (stride + 1):
        raise ValueError(f"{path}: IDAT 数据不足")

    out: Grid = []
    prev = bytearray(stride)
    src = 0
    for _ in range(height):
        ftype = raw[src]
        src += 1
        line = bytearray(raw[src:src + stride])
        src += stride
        if ftype == 0:                                    # None
            pass
        elif ftype == 1:                                  # Sub
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 0xFF
        elif ftype == 2:                                  # Up
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif ftype == 3:                                  # Average
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif ftype == 4:                                  # Paeth
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + _paeth(a, prev[i], prev[i - bpp] if i >= bpp else 0)) & 0xFF
        else:
            raise ValueError(f"{path}: 未知行滤波类型 {ftype}")
        prev = line

        row: List[Pixel] = []
        for x in range(width):
            if color_type == 0:
                row.append((line[x], line[x], line[x], 255))
            elif color_type == 2:
                row.append((line[x * 3], line[x * 3 + 1], line[x * 3 + 2], 255))
            elif color_type == 3:
                idx = line[x]
                r, g, b, a = palette[idx]
                if idx < len(trns):
                    a = trns[idx]
                row.append((r, g, b, a))
            elif color_type == 4:
                row.append((line[x * 2], line[x * 2 + 1], line[x * 2 + 1], line[x * 2]))
            else:                                          # color_type == 6
                row.append((line[x * 4], line[x * 4 + 1], line[x * 4 + 2], line[x * 4 + 3]))
        out.append(row)
    return out


def _chunk(tag: bytes, payload: bytes) -> bytes:
    """拼一个 PNG chunk：长度 + 类型 + 数据 + CRC。"""
    return (struct.pack(">I", len(payload)) + tag + payload
            + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF))


def write(path: str, rows: Grid) -> None:
    """把 RGBA 网格写成 8bit RGBA PNG（filter=0，逐行）。"""
    height = len(rows)
    width = len(rows[0]) if height else 0
    raw = bytearray()
    for row in rows:
        raw.append(0)                                     # filter type 0 (None)
        for r, g, b, a in row:
            raw += bytes((r & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF))

    png = (_SIGNATURE
           + _chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
           + _chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + _chunk(b"IEND", b""))
    parent = os.path.dirname(os.path.abspath(path))
    if parent:
        os.makedirs(parent, exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(png)


def nearest(ch: str) -> Pixel:
    """色键 -> RGBA；'.' 表示全透明。"""
    if ch == ".":
        return (0, 0, 0, 0)
    raise ValueError(f"未知色键 {ch!r}")


def from_chars(chars: Sequence[str], table: Dict[str, Pixel]) -> Grid:
    """字符画 -> 网格。字符画必须是等长行。"""
    return [[table.get(ch, (0, 0, 0, 0)) for ch in line] for line in chars]


def to_chars(rows: Grid, table: Dict[str, Pixel]) -> List[str]:
    """网格 -> 字符画（取逆映射）。"""
    inverse = {v: k for k, v in table.items()}
    inverse[(0, 0, 0, 0)] = "."
    inverse[(0, 0, 0, 255)] = "."
    return ["".join(inverse.get(px, "?") for px in row) for row in rows]


def size(path: str) -> Tuple[int, int]:
    """只读取 IHDR 拿尺寸，不解压像素。"""
    with open(path, "rb") as fh:
        head = fh.read(33)
    if head[:8] != _SIGNATURE:
        raise ValueError(f"{path}: 不是合法 PNG")
    w, h = struct.unpack(">II", head[16:24])
    return w, h


def nearest_4x(rows: Grid) -> Grid:
    """最近邻放大 4 倍（返回放大后的网格，不做插值/平滑）。"""
    h, w = len(rows), len(rows[0])
    out: Grid = []
    for y in range(h * 4):
        src_y = y // 4
        out.append([rows[src_y][x // 4] for x in range(w * 4)])
    return out
