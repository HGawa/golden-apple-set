#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""金苹果套装 Forge 1.20.1 mod · 集成回归校验（纯标准库，不依赖 JDK/Gradle）

覆盖 8 组：
    T1 资源树完整     11 贴图 / 2 盔甲层 / 1 盾实体 / 4 弓帧 / 12 模型 / 2 语言 / 11 配方 / Java 源
    T2 PNG 尺寸       16×16（item/bow）、64×32（盔甲层）、64×64（盾实体）
    T3 调色板合规      盔甲为原版金色 + 叶绿，其余资源保持原有 12 色
    T4 模型引用可达    models/item 每个贴图引用都能在磁盘找到 png
    T5 配方规范        1.20.1：result.item / key 为 Ingredient 对象 / pattern 空槽为空格
    T6 语言与注册一致  12 件物品名称与注册名一致，含 11 件装备与 1 个刷怪蛋
    T7 Java 静态检查   包路径/类名一致、MODID 一致、12 件均有注册
    T8 mods.toml 一致  modId / 版本 / 依赖区间 / pack_format

用法：
    C:/Users/Administrator/.workbuddy/binaries/python/versions/3.13.12/python.exe \
        gas_mod/scripts/verify_mod.py
退出码 0 = 全通过
"""
from __future__ import annotations

import glob
import json
import os
import struct
import sys
import zipfile
import zlib
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
SRC = MOD / "src" / "main"
RES = SRC / "resources"
ASSETS = RES / "assets" / "gas"

NAMES = [
    "sword", "helmet", "chestplate", "leggings", "boots", "pickaxe",
    "axe", "shovel", "hoe", "shield", "bow",
]
JAVA_ITEMS = ["golden_apple_" + n for n in NAMES]
BLOCK_NAMES = ['golden_apple_grass_block', 'golden_apple_dirt', 'golden_apple_stone', 'golden_apple_cobblestone']
REG_NAMES = ["golden_apple_" + n for n in NAMES] + ["golden_apple_farmer_spawn_egg"]

PALETTE = {
    "L": (0xFC, 0xE9, 0x7A), "G": (0xF6, 0xC9, 0x45), "D": (0xA8, 0x7B, 0x1B),
    "*": (0xFF, 0xFF, 0xFF), "u": (0x7C, 0xC8, 0x5B), "V": (0x4E, 0x9B, 0x3C),
    "v": (0x2F, 0x6B, 0x27), "W": (0x7A, 0x4E, 0x28), "w": (0x54, 0x33, 0x1B),
    "l": (0x5F, 0xAF, 0x3E), "R": (0xE2, 0x3B, 0x32), "Y": (0xF8, 0xD3, 0x4A),
}

results: list[tuple[str, str, bool, str]] = []


def rec(tid: str, name: str, ok: bool, info: str = "") -> None:
    results.append((tid, name, ok, info))


# ---------------------------------------------------------------- PNG 解码
def png_info(path: Path) -> tuple[int, int, list[tuple[int, int, int, int]]]:
    """返回 (w, h, 像素列表 RGBA)。纯标准库。"""
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"{path.name} 不是 PNG"
    pos, idat = 8, b""
    w = h = ct = 0
    while pos < len(data):
        ln = int.from_bytes(data[pos:pos + 4], "big")
        tag = data[pos + 4:pos + 8]
        if tag == b"IHDR":
            w = int.from_bytes(data[pos + 8:pos + 12], "big")
            h = int.from_bytes(data[pos + 12:pos + 16], "big")
            ct = data[pos + 17]
        elif tag == b"IDAT":
            idat += data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
    ch = {0: 1, 2: 3, 4: 2, 6: 4}[ct]
    raw = zlib.decompress(idat)
    stride = w * ch
    px, prev, off = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[off]
        off += 1
        line = bytearray(raw[off:off + stride])
        off += stride
        if f == 1:
            for i in range(ch, stride):
                line[i] = (line[i] + line[i - ch]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                b = prev[i]
                c = prev[i - ch] if i >= ch else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        px.append(bytes(line))
        prev = line
    out = []
    for line in px:
        for x in range(w):
            p = line[x * ch:(x + 1) * ch]
            out.append((p[0], p[1], p[2], p[3] if ch == 4 else 255) if ch == 4
                       else (p[0], p[1], p[2], 255))
    return w, h, out


# ---------------------------------------------------------------- T1
def t1() -> None:
    missing: list[str] = []
    for n in NAMES:
        if not (ASSETS / "textures" / "item" / f"golden_apple_{n}.png").exists():
            missing.append(f"textures/item/{n}")
    for n in (1, 2):
        if not (ASSETS / "textures" / "models" / "armor" / f"golden_apple_layer_{n}.png").exists():
            missing.append(f"layer_{n}")
    if not (ASSETS / "textures" / "entity" / "shield" / "golden_apple_shield.png").exists():
        missing.append("shield entity")
    for n in ("bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2"):
        if not (ASSETS / "textures" / "item" / f"{n}.png").exists():
            missing.append(f"item/{n}")
    for n in NAMES:
        if not (ASSETS / "models" / "item" / f"golden_apple_{n}.json").exists():
            missing.append(f"models/item/{n}")
    for ln in ("zh_cn", "en_us"):
        if not (ASSETS / "lang" / f"{ln}.json").exists():
            missing.append(f"lang/{ln}")
    if not (RES / "data" / "gas" / "recipes").is_dir():
        missing.append("data/gas/recipes")
    else:
        for n in NAMES:
            if not (RES / "data" / "gas" / "recipes" / f"golden_apple_{n}.json").exists():
                missing.append(f"recipe/{n}")
    if not (RES / "META-INF" / "mods.toml").exists():
        missing.append("mods.toml")
    for f in ("build.gradle", "settings.gradle", "gradle.properties"):
        if not (MOD / f).exists():
            missing.append(f)
    for f in ("GoldenAppleSetMod.java", "GoldenAppleSwordItem.java"):
        if not (SRC / "java" / "com" / "gas" / "mod" / f).exists():
            missing.append(f"java/{f}")
    ok = not missing
    rec("T1", "资源树完整（贴图/模型/语言/配方/构建/Java）", ok,
        "缺失: " + ", ".join(missing) if missing else f"全部就位（{len(NAMES) + 13 + 11} 个文件点）")


# ---------------------------------------------------------------- T2/T3
def png_files() -> list[tuple[str, Path]]:
    """所有必须存在的贴图：(说明, 路径)。缺失由 T2 统一计入违规。"""
    items: list[tuple[str, Path]] = []
    for n in NAMES:
        p = ASSETS / "textures" / "item" / f"golden_apple_{n}.png"
        if p not in [q for _, q in items]:
            items.append((f"item/{n}", p))
    for n in (1, 2):
        items.append((f"layer_{n}", ASSETS / "textures" / "models" / "armor" / f"golden_apple_layer_{n}.png"))
    items.append(("shield entity", ASSETS / "textures" / "entity" / "shield" / "golden_apple_shield.png"))
    for n in ("bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2"):
        items.append((f"item/{n}", ASSETS / "textures" / "item" / f"{n}.png"))
    items.append(("farmer shoulders", ASSETS / "textures/entity/golden_apple_farmer/shoulders.png"))
    return items


def expect_size(label: str) -> tuple[int, int]:
    if label == "shield entity":
        return 64, 64
    if label == "farmer shoulders":
        return 64, 32
    if label.startswith("layer_"):
        return 64, 32
    return 16, 16


def t2_t3() -> None:
    bad: list[str] = []
    for label, p in png_files():
        if not p.exists():
            bad.append(f"{label} 文件缺失")
            continue
        want = expect_size(label)
        w, h, _ = png_info(p)
        if (w, h) != want:
            bad.append(f"{label} {w}x{h}≠{want[0]}x{want[1]}")
    rec("T2", "PNG 尺寸（16×16 / 64×32 / 64×64）", not bad,
        "异常: " + ", ".join(bad) if bad else "全部符合")

    offenders: list[str] = []
    scanned = 0
    for label, p in png_files():
        if not p.exists():
            continue
        scanned += 1
        _, _, px = png_info(p)
        # 2026-10-13: user requested vanilla gold armor with sparse green leaves.
        # Only the four armor icons and two wearable layers use this palette.
        leaf_green = {(47, 107, 39), (78, 155, 60), (124, 200, 91)}
        gold_icons = {(63, 46, 14), (130, 93, 22), (220, 150, 19),
                      (233, 177, 21), (234, 238, 87), (253, 255, 118), (255, 255, 255)}
        gold_layers = {(211, 150, 50), (224, 178, 48), (245, 184, 28),
                       (253, 255, 118), (254, 255, 189), (255, 216, 61),
                       (255, 240, 90), (255, 255, 255)}
        allowed = set(PALETTE.values())
        if label in {"item/helmet", "item/chestplate", "item/leggings", "item/boots"}:
            allowed = gold_icons | leaf_green
        elif label in {"layer_1", "layer_2"}:
            allowed = gold_layers | leaf_green
        off = {q[:3] for q in px if q[3] > 8} - allowed
        if off:
            offenders.append(f"{label} 异色 {len(off)}")
    if scanned == 0:
        offenders.append("一个 PNG 都没有，无法验证调色板")
    rec("T3", "调色板合规（盔甲原版金色+叶绿 / 其他12色）", not offenders,
        "违规: " + "; ".join(offenders) if offenders else f"{scanned} 张贴图符合各自调色板")


# ---------------------------------------------------------------- T4
def t4() -> None:
    bad: list[str] = []
    d = ASSETS / "models" / "item"
    if not d.is_dir():
        rec("T4", "模型贴图引用可达", False, "models/item 目录不存在")
        return
    files = sorted(d.glob("*.json"))
    if len(files) != len(REG_NAMES + BLOCK_NAMES):
        bad.append(f"模型数量 {len(files)}，应为 {len(REG_NAMES + BLOCK_NAMES)}")
    want = {f"{n}.json" for n in REG_NAMES + BLOCK_NAMES}
    miss = sorted(want - {f.name for f in files})
    bad.extend(f"缺模型 {m}" for m in miss)
    for f in files:
        try:
            m = json.loads(f.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001
            bad.append(f"{f.name}: JSON 解析失败 {exc}")
            continue
        for key, val in m.get("textures", {}).items():
            if not isinstance(val, str):
                bad.append(f"{f.name}:{key} 贴图值非字符串")
                continue
            if val.startswith("gas:"):
                rel = val[4:]
                if not (ASSETS / "textures" / (rel + ".png")).exists():
                    bad.append(f"{f.name}:{key} 指向 {val} 不存在")
            elif not val.startswith("minecraft:"):
                bad.append(f"{f.name}:{key} 命名空间非法 {val}")
    rec("T4", "模型贴图引用可达", not bad,
        "问题模型: " + "; ".join(bad) if bad else "16 个物品模型引用全部落地")


# ---------------------------------------------------------------- T5 的参考数据
# 来源（都在本机，不联网）：原版客户端 jar 的 en_us.json（item id）、
# 基线构建产物 mod/build/combined/data（tag）。均可用环境变量覆盖。
BASELINE = MOD.parent / "mod"
TAG_ROOT = Path(os.environ.get("GAS_TAG_ROOT", str(BASELINE / "build" / "combined" / "data")))
LANG_ENTRY = "assets/minecraft/lang/en_us.json"


def _jar_candidates() -> list[Path]:
    env = os.environ.get("GAS_VANILLA_JAR")
    if env:
        return [Path(env)]
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")))
    out: list[Path] = [gradle_home / "caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar"]
    for root in (Path("D:/PCL/.minecraft/versions"),
                 Path("D:/MCLDownload/Game/.minecraft/versions"),
                 Path("D:/XX/.minecraft/versions")):
        if not root.is_dir():
            continue
        for d in sorted(root.iterdir()):
            if d.is_dir() and d.name.startswith("1.20.1"):
                out.extend(sorted(d.glob("*.jar")))
    return out


def vanilla_jar() -> Path | None:
    """找到 1.20.1 客户端 jar（要求含 en_us.json，排除 fabric 的加工产物）。"""
    for p in _jar_candidates():
        try:
            with zipfile.ZipFile(p) as z:
                if LANG_ENTRY in z.namelist():
                    return p
        except Exception:  # noqa: BLE001
            continue
    return None


def vanilla_item_ids(jar: Path) -> set[str]:
    with zipfile.ZipFile(jar) as z:
        keys = json.loads(z.read(LANG_ENTRY).decode("utf-8"))
    ids: set[str] = set()
    for k in keys:
        for pre in ("item.minecraft.", "block.minecraft."):
            if k.startswith(pre):
                ids.add(k[len(pre):])
    return ids


# ---------------------------------------------------------------- T5
def t5() -> None:
    bad: list[str] = []
    d = RES / "data" / "gas" / "recipes"
    if not d.is_dir():
        rec("T5", "配方 1.20.1 规范（格式 + id 真实存在）",
            False, "data/gas/recipes 目录不存在")
        return
    files = sorted(d.glob("*.json"))
    if len(files) != len(NAMES):
        bad.append(f"配方数量 {len(files)}，应为 {len(NAMES)}")
    for n in NAMES:
        if not (d / f"golden_apple_{n}.json").exists():
            bad.append(f"缺配方 golden_apple_{n}")

    recipes: list[tuple[Path, dict]] = []
    for f in files:
        try:
            r = json.loads(f.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001
            bad.append(f"{f.name}: JSON 解析失败 {exc}")
            continue
        recipes.append((f, r))
        if "result" not in r or "item" not in r["result"]:
            bad.append(f"{f.name}: result.item 缺失")
        if "id" in r.get("result", {}):
            bad.append(f"{f.name}: 用了 1.21 的 result.id")
        for k, v in r.get("key", {}).items():
            if not isinstance(v, dict) or ("item" not in v and "tag" not in v):
                bad.append(f"{f.name}: key {k} 不是 Ingredient 对象")
        if "pattern" not in r:
            bad.append(f"{f.name}: 缺 pattern")
        allowed = set(" ") | set(r.get("key", {}))
        for row in r.get("pattern", []):
            for ch in sorted(set(row) - allowed):
                bad.append(f"{f.name}: pattern 空槽含未定义字符 '{ch}'")

    # ---- id 真实存在性：格式合法但拼错的 id（如 minecraft:vines）会被游戏静默丢弃
    jar = vanilla_jar()
    if jar is None:
        bad.append("找不到 1.20.1 客户端 jar，无法验证 id 真实性"
                   "（用环境变量 GAS_VANILLA_JAR 指定）")
        items: set[str] = set()
    else:
        items = vanilla_item_ids(jar)
    vanilla_tags = set()
    if jar is not None:
        with zipfile.ZipFile(jar) as archive:
            vanilla_tags = set(archive.namelist())
    for f, r in recipes:
        refs: list[tuple[str, str]] = []
        for v in r.get("key", {}).values():
            if isinstance(v, dict):
                refs += [("item", v["item"])] if "item" in v else []
                refs += [("tag", v["tag"])] if "tag" in v else []
        if "item" in r.get("result", {}):
            refs.append(("item", r["result"]["item"]))
        for kind, rid in refs:
            if not isinstance(rid, str) or ":" not in rid:
                bad.append(f"{f.name}: id 缺少命名空间 -> {rid}")
                continue
            ns, path = rid.split(":", 1)
            if kind == "item":
                if ns == "gas":
                    if path not in REG_NAMES:
                        bad.append(f"{f.name}: gas 物品未注册 -> {rid}")
                elif ns == "minecraft":
                    if items and path not in items:
                        bad.append(f"{f.name}: 原版不存在这个 item -> {rid}")
                else:
                    bad.append(f"{f.name}: 未知命名空间 -> {rid}")
            elif not ((TAG_ROOT / ns / "tags" / "items" / f"{path}.json").is_file() or f"data/{ns}/tags/items/{path}.json" in vanilla_tags):
                bad.append(f"{f.name}: 标签不存在 -> {rid}")

    tag_note = f"tag 根 {TAG_ROOT.name}" if TAG_ROOT.is_dir() else "客户端 JAR 标签"
    rec("T5", "配方 1.20.1 规范（格式 + id 真实存在）", not bad,
        "违规: " + "; ".join(bad[:8]) if bad else
        f"{len(files)} 份配方合规；id 对照原版 {len(items)} 个 item（{tag_note}）")


# ---------------------------------------------------------------- T6
def t6() -> None:
    bad: list[str] = []
    for ln in ("zh_cn", "en_us"):
        p = ASSETS / "lang" / f"{ln}.json"
        if not p.exists():
            bad.append(f"{ln}.json 缺失")
            continue
        keys = set(json.loads(p.read_text(encoding="utf-8")))
        want = {f"item.gas.{n}" for n in REG_NAMES} | {f"block.gas.{n}" for n in BLOCK_NAMES} | {"itemGroup.gas.golden_apple_set", "entity.gas.golden_apple_farmer", "biome.gas.golden_realm", "tooltip.gas.armor.full_set", "tooltip.gas.armor.bonus"}
        if keys != want:
            diff = (want - keys, keys - want)
            bad.append(f"{ln}: 缺 {len(diff[0])} 条 / 多 {len(diff[1])} 条")
    rec("T6", "语言键与物品和方块注册名一致", not bad,
        "差异: " + "; ".join(bad) if bad else "zh_cn/en_us 各 12 件物品 + 4 个方块 + 创造页 + NPC + 群系名称对齐")


# ---------------------------------------------------------------- T7
def t7() -> None:
    bad: list[str] = []
    d = SRC / "java" / "com" / "gas" / "mod"
    if not d.is_dir():
        rec("T7", "Java 静态检查（包/类名/MODID/12 件注册）", False,
            "src/main/java/com/gas/mod 目录不存在")
        return
    srcs = sorted(d.rglob("*.java"))
    if not srcs:
        rec("T7", "Java 静态检查（包/类名/MODID/12 件注册）", False, "没有任何 .java 文件")
        return
    if len(srcs) < 2:
        bad.append(f"java 文件数 {len(srcs)}，至少 2（主类 + GoldenAppleSwordItem）")
    for f in srcs:
        src = f.read_text(encoding="utf-8")
        if "package " + ".".join(f.relative_to(SRC / "java").parent.parts) + ";" not in src:
            bad.append(f"{f.name}: package 声明不符")
        if f.stem not in src:
            bad.append(f"{f.name}: 未声明 public class {f.stem}")
        if not src.strip():
            bad.append(f"{f.name}: 空文件")
    all_src = "\n".join(p.read_text(encoding="utf-8") for p in srcs)
    if 'MODID = "gas"' not in all_src and "MODID = 'gas'" not in all_src:
        bad.append("MODID 常量不是 gas")
    for n in REG_NAMES:
        if f'"{n}"' not in all_src:
            bad.append(f"未注册 {n}")
    if "DeferredRegister" not in all_src or "Registries.CREATIVE_MODE_TAB" not in all_src:
        bad.append("缺少物品注册或独立创造标签页注册")
    rec("T7", "Java 静态检查（包/类名/MODID/12 件注册）", not bad,
        "问题: " + "; ".join(bad) if bad else "源码结构一致")


# ---------------------------------------------------------------- T8
def t8() -> None:
    bad: list[str] = []
    p = RES / "META-INF" / "mods.toml"
    if not p.exists():
        bad.append("mods.toml 缺失")
    else:
        t = p.read_text(encoding="utf-8")
        if 'modId="gas"' not in t:
            bad.append("modId 不符")
        if 'loaderVersion="[47,)"' not in t:
            bad.append("loaderVersion 不是 [47,)")
        if 'versionRange="[1.20.1,1.21)"' not in t and "[1.20.1," not in t:
            bad.append("MC 版本区间缺失")
    p2 = RES / "pack.mcmeta"
    if not p2.exists():
        bad.append("pack.mcmeta 缺失")
    else:
        j = json.loads(p2.read_text(encoding="utf-8"))
        if j.get("pack", {}).get("pack_format") != 15:
            bad.append("pack_format 不是 15")
    rec("T8", "mods.toml / pack.mcmeta 一致", not bad,
        "问题: " + "; ".join(bad) if bad else "modId=gas / loader[47,) / MC[1.20.1,1.21) / pack_format 15")


if __name__ == "__main__":
    print("=" * 72)
    print("金苹果套装 · Forge 1.20.1 mod 集成回归校验")
    print("=" * 72)
    if not RES.is_dir():
        print("!! src/main/resources 不存在——整个 mod 工程还没落地，不必逐项跑。\n")
    for fn in (t1, t2_t3, t4, t5, t6, t7, t8):
        try:
            fn()
        except Exception as exc:  # noqa: BLE001
            rec(fn.__name__.upper().replace("_", "/"), f"{fn.__name__} 执行异常", False, repr(exc))
    total = fails = 0
    for tid, name, ok, info in results:
        total += 1
        fails += 0 if ok else 1
        print(f"[{'PASS' if ok else 'FAIL'}] {tid} {name}  {info}")
    print("-" * 72)
    print(f"通过 {total - fails}/{total} 组，失败 {fails}")
    if fails:
        sys.exit(1)
