# 金苹果维度方块 · 1.7.0

四种方块采用 Minecraft 原生 16×16 像素纹理、完整立方体模型，已加入「金苹果套装」创造标签页，支持正常放置和挖掘。

| 中文名 / ID（gas 命名空间） | 外观 | 普通挖掘 |
| --- | --- | --- |
| 金苹果草方块 `golden_apple_grass_block` | 金色原版草纹顶部、金色草沿、绿色细纹，底部为金苹果泥土 | 掉金苹果泥土；锹加速 |
| 金苹果泥土 `golden_apple_dirt` | 沿用草方块下部土层纹理与绿纹，六面无草皮 | 掉自身；锹加速 |
| 金苹果石头 `golden_apple_stone` | 原版石头颗粒结构，白色与浅灰层次 | 镐挖掘掉金苹果圆石 |
| 金苹果圆石 `golden_apple_cobblestone` | 原版圆石块面与裂缝，白色石块、灰色接缝 | 镐挖掘掉自身 |

精准采集可获得草方块和石头本体。石头、圆石需要镐才能正常掉落，硬度和音效参照对应原版方块。草方块为稳定装饰地表，不会自动扩散或在遮挡后转成泥土。

## 维度生成

按本次确认，`gas:golden_realm` 的新生成地形以金苹果草方块覆盖表面，下铺数层金苹果泥土，再往下为白色金苹果石头；深切地形或洞口可露出白石，地下洞穴不会铺满草皮。圆石主要通过挖掘石头获取。沿用主世界噪声起伏、地下空洞、无水海盆和淡紫天空。

已有区块保留原来的金块，不扫描或替换存档。更新后前往未生成地区或创建新世界可见新地形；新旧交界会有材质变化。

进入命令：`/gas gold`。直接获取：`/give @s gas:golden_apple_grass_block`，其他方块使用表中对应 ID。

## 资源与验证

- 贴图：`src/main/resources/assets/gas/textures/block/`，共 5 张（草顶、草侧、泥土、石头、圆石）。草底复用泥土，不使用群系绿色 tint。
- 原生 JSON：`assets/gas/blockstates`、`models/block`、`models/item`；掉落表在 `data/gas/loot_tables/blocks`，工具标签在 `data/minecraft/tags/blocks/mineable`。
- 可复现像素源：`node scripts/build_realm_blocks.cjs`，校验用 `--check`。`art/realm_blocks/vanilla/` 保存本机原版 1.20.1 client.jar 纹理参考，颜色重映射保留像素结构，绿纹坐标在脚本中定义。
- [纹理图集](art/realm_blocks/texture-atlas.png)，从左到右：草顶、草侧、泥土、石头、圆石。
- 客户端测试：`-I scripts/build_with_mirror.gradle -I scripts/render_smoke.gradle -PgasSmoke=blocks runClient --offline --console=plain`（Gradle 8.1.1 / JDK 17）。结果与实际截图在 `art/realm_blocks/runtime/`。

[四块并排实机预览](art/realm_blocks/runtime/01-four-blocks.png) · [新区块地形](art/realm_blocks/runtime/02-new-terrain.png) · [创造背包](art/realm_blocks/runtime/03-creative-tab.png)
