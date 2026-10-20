# 金苹果农夫：金绿双肩披肩

1.3.1 的实际游戏模型，沿用原版农夫的草帽、脸部、抱臂、长袍与群系服装。
左右各一片短披肩，共用对称的设计：金色肩顶、浅金镶边、绿色侧面垂片和背部短尾；肩顶点缀小片绿叶。
胸前中央保持开放，保留农夫抱臂的辨识度。披肩随身体旋转，不跟随头部转动；只应用于 `gas:golden_apple_farmer`。

## 模型与贴图

- 原生 Java 模型：`src/main/java/com/gas/mod/client/GoldenShoulderLayer.java`，每肩 5 个长方体，共 10 个。
- 专属渲染器：`src/main/java/com/gas/mod/client/GoldenAppleFarmerRenderer.java`。继承原版村民各服装层，再绘制披肩。
- 贴图：`src/main/resources/assets/gas/textures/entity/golden_apple_farmer/shoulders.png`，64×32 RGBA，6 种金色/绿色。
- 像素源：项目根目录 `gas_asset_build/farmer_shoulders.py`，使用已有原生像素编译工具，不依赖外部模型库。

UV 每个长方体覆盖完整六面，左右共享纹理。模型由 Forge `RegisterLayerDefinitions` 烘焙；Java 版实体采用代码模型，不使用物品的 `models/item` JSON。

在项目根目录重建或检查：

```powershell
& 'C:/Users/Administrator/.workbuddy/binaries/python/versions/3.13.12/python.exe' -B gas_asset_build/farmer_shoulders.py
& 'C:/Users/Administrator/.workbuddy/binaries/python/versions/3.13.12/python.exe' -B gas_asset_build/farmer_shoulders.py --check
```

## 实际游戏检查

`src/renderSmoke` 的 NPC 检查验证专属渲染器和双侧模型成功烘焙，并保存实际客户端图片：

- `runtime/01-farmer.png`：正面。
- `runtime/03-farmer-side.png`：侧面。
- `runtime/04-farmer-back.png`：背面。
- `runtime/02-trading.png`：交易界面。

继续复用 [NPC 检查命令](../../FARMER.md)，同时回归村庄刷新、扣款、库存、补货和存档功能。
