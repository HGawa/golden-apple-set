# 工具握持与金苹果盾 · 1.1.4 · 2026-10-14

## 修复内容

- 镐、斧、锹、锄改用 `minecraft:item/handheld`，第一/第三人称左右手继承原版工具握持姿态。
- 为 `gas:golden_apple_shield` 注册客户端 `minecraft:blocking` 属性。
  仅正在使用的那一份盾堆栈返回 1；未使用、其他堆栈、无持有者时返回 0。
- 待机与格挡分别继承 `minecraft:item/shield` 和 `minecraft:item/shield_blocking`；
  沿用原版主手/副手的举盾姿态、使用动作和恢复行为。
- 使用真实 `ShieldModel`：盾面 12×22×1，握柄 2×6×6，具有正面、背面、厚度和立体握柄。
- 将原先放大的物品图标替换为专门的 64×64 UV 贴图：金色包边与苹果徽记、边角绿叶、
  木板背面、独立木纹握柄；全部 12 个模型面纹理完整，保留附魔光效。
- 两条历史资产生成入口同步使用 `gas_asset_build/shield_assets.py`，工具生成模板也同步修正，
  避免重新生成时还原这两个问题。
- Gradle 显式使用 UTF-8 编译，避免 Windows GBK 默认编码损坏中文源码/提示文本。

## UV 坐标

来自本机 Minecraft 1.20.1 `ShieldModel.createLayer()` 及 `ModelPart.Cube` 映射字节码。
坐标格式为左上角 `(x,y)` + `宽×高`，所有被采样区域不透明，其余画布透明。

| 区域 | 正面 | 背面 | 左/右侧 | 上/下 |
| --- | --- | --- | --- | --- |
| 盾面 | (1,1) 12×22 | (14,1) 12×22 | (0,1)/(13,1) 1×22 | (1,0)/(13,0) 12×1 |
| 握柄 | (32,6) 2×6 | (40,6) 2×6 | (26,6)/(34,6) 6×6 | (32,0)/(34,0) 2×6 |

`preview.html` / `preview.png` 是实际 UV 与尺寸的离线资源预览。
`runtime/*.png` 是独立 Forge 开发客户端中的实际截图，包含主手、副手的待机/格挡及第三人称盾面。
`runtime/result.txt` 保存运行时断言结果。

## 重建与检查

在项目根目录执行：

```powershell
& 'C:/Users/Administrator/.workbuddy/binaries/python/versions/3.13.12/python.exe' -B gas_asset_build/shield_assets.py
& 'C:/Users/Administrator/.workbuddy/binaries/python/versions/3.13.12/python.exe' -B gas_mod/scripts/verify_rendering.py
& 'C:/Users/Administrator/.workbuddy/binaries/python/versions/3.13.12/python.exe' -B gas_mod/scripts/verify_mod.py
node gas_mod/scripts/build_leaf_armor.cjs --check
node gas_mod/scripts/build_shield_preview.cjs
```

`verify_rendering.py` 还验证检查器能拒绝普通物品握持模型、盾面透明洞和旧版 4 倍放大图标。
原来的资源存在性检查不足以发现这些问题。

在 `gas_mod` 目录执行可选的客户端自动检查：

```powershell
$env:JAVA_HOME='D:/AI/golden_apple_sword/toolchain/jdk/jdk-17.0.20.1+1'
& '../_tools/gradle-8.1.1/gradle-8.1.1/bin/gradle.bat' -I scripts/build_with_mirror.gradle -I scripts/render_smoke.gradle runClient --offline --console=plain
```

测试源位于独立的 `src/renderSmoke`，仅上述 init 脚本启用，不进入发布 JAR。
它创建 `run/client/saves/gas-render-smoke-*` 测试世界，不读取用户存档，测试完成自动退出客户端。
检查实际烘焙后的四把工具在四种手持上下文的变换、盾牌主/副手的属性值与模型切换、
松开恢复、空持有者与非活动堆栈，并保存游戏截图。

正常打包不带测试 init 脚本：

```powershell
& '../_tools/gradle-8.1.1/gradle-8.1.1/bin/gradle.bat' -I scripts/build_with_mirror.gradle build --offline --console=plain
```
