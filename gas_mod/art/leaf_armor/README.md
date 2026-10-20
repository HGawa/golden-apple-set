# 绿叶金甲 · 2026-10-13 · 1.1.3

本轮用户要求：叶子只在边缘做点缀，让盔甲更简洁。
落地范围为头盔、胸甲、护腿、靴子四件；保留原版模型轮廓、金色明暗和透明区域。
此要求取代旧设计在这四件装备上的原创外形、苹果装饰和固定 12 色限制。

## 交付与模型绑定

- `preview.html`：可直接双击的离线实际资源预览，可拖动旋转、缩放、切换原版对比。
- `preview.png`：该预览页面截图，不是游戏实测截图。
- `concept.png`：内置 imagegen 生成的初始造型参考，最终以实际 PNG 和模型绑定为准。
- `vanilla/`：从本机 Minecraft 1.20.1 官方客户端 JAR 提取的六张原版金甲参考贴图。
- 游戏文件位于 `src/main/resources/assets/gas/textures/`：两张 64×32 穿戴层和四张 16×16 物品图标。
- 四件均已注册为 `ArmorItem`，沿用原版 `HumanoidModel` 盔甲立方体模型。
  `GoldenAppleArmorMaterial.getName()` 返回 `gas:golden_apple`，Forge 将头盔、胸甲、靴子绑定
  `golden_apple_layer_1.png`，护腿绑定 `golden_apple_layer_2.png`。
  原版外层膨胀 1.0、护腿层 0.5；没有额外凸起叶片几何。
- 四份 `models/item/golden_apple_<部件>.json` 使用原版 `minecraft:item/generated` 并指向对应图标。
- 装备数值、技能和合成配方不属于本次外观改动。

## 设计与可复现来源

叶片使用 `#2F6B27 / #4E9B3C / #7CC85B` 三种绿，浅色叶尖与深色下缘构成对角小叶。
仅在盔沿、肩部边缘、护腿外侧接缝和靴口保留小叶片；头顶、胸腹、背部、腰带、腿部正背面和鞋面均恢复原版金色。64×32 贴图沿用原版左右肢体镜像。
按用户最新要求精简：穿戴层从 197 个减至 41 个叶绿像素（37 + 4）；头盔/胸甲/护腿/靴子图标分别为 6/8/8/6 个叶绿像素，均只在外缘点缀。
所有其他 RGBA 像素与原版一致，不扩大剪影、不填平眼孔、不修改 UV 岛。

最终像素布局在 `scripts/build_leaf_armor.cjs` 的 `assets[].marks` 中维护。
该文件是原生像素资产源，使用 Node 内置库编译为 PNG，无需安装依赖。
普通 Gradle 构建直接使用已经生成的 PNG，不依赖 Node。
若使用旧的全量美术生成脚本，随后必须重新执行此脚本，避免恢复旧盔甲。

在 `gas_mod` 目录执行：

```powershell
node scripts/build_leaf_armor.cjs
node scripts/build_leaf_armor.cjs --check
& 'C:/Users/Administrator/.workbuddy/binaries/python/versions/3.13.12/python.exe' -B scripts/verify_mod.py
$env:JAVA_HOME='D:/AI/golden_apple_sword/toolchain/jdk/jdk-17.0.20.1+1'
& '../_tools/gradle-8.1.1/gradle-8.1.1/bin/gradle.bat' -I scripts/build_with_mirror.gradle build --offline --console=plain
```

`--check` 校验四个模型引用、输出与源布局一致、原版透明区域不变、叶片未越界、
未装饰像素颜色不变、每张贴图的叶绿比例小于 10%。集成检查仅对这六张资源采用
原版金色和三种叶绿的白名单，其余装备仍保持原有 12 色约束。

## 设计参考提示词

使用内置 imagegen（非 CLI）生成概念参考；输入参考为原版 `gold_layer_1.png` 与
`golden_chestplate.png`。提示要求：Minecraft 原版金甲的正背三分之四视角、四件物品图标；
保留原版方块几何与亮黄金色，只在太阳穴、肩领、腿侧与靴口加少量平面像素绿叶，
至少 90% 仍为金色；叶绿使用上述三色；无红苹果、无大片藤蔓、无武器、无额外凸起或幻想曲面；
深色中性背景，不加文字。概念图不承担 UV 数据或游戏实测证明。

已完成资源静态检查、实际资源预览检查和 ForgeGradle 打包。尚未启动 Minecraft 客户端实测。
