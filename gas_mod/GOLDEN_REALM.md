# 黄金维度 · 1.7.0

维度 ID：`gas:golden_realm`。独立于主世界，不替换主世界生成器。

## 进入与返回

启用作弊或拥有管理员权限（权限等级 2）的玩家执行：

```mcfunction
/gas gold
/gas overworld
```

第一条进入黄金维度原点附近，第二条返回主世界出生点附近。命令会加载目标地形、寻找有坚实支撑且无碰撞、无流体的落脚点，再传送玩家；找不到时保留原位置。
当前入口是管理员命令，没有新增传送门或生存传送道具。安装新版本后需重新启动游戏或服务器以加载新维度。

刷怪蛋获取：

```mcfunction
/give @s gas:golden_apple_farmer_spawn_egg
```

## 地形与外观

- 使用 Minecraft 1.20.1 原版主世界噪声路由与高度范围（-64 至 319），形成自然起伏、山坡、谷地与地下空洞。不是超平坦地面。
- 1.7.0 新生成区域使用金苹果草地表层、金苹果泥土层和白色金苹果石头地下层；旧区块保持原来的金块。海平面仍设在最低生成高度，流体为空气，关闭含水层、矿脉与装饰生成，不生成水、熔岩或树木。详见 [方块说明](REALM_BLOCKS.md)。
- 唯一群系为「黄金原野」，天空底色为淡紫 `#C6B0FF`，远雾为 `#D2BFFF`。保留主世界的日夜、太阳/月亮和天空渲染，因此夜晚会变暗；群系不下雨雪。
- 未添加自然生物刷新和结构。刷怪蛋生成的 NPC 仍可在这里使用。
- 四种新方块可正常放置、挖掘；草方块掉泥土，石头掉圆石，精准采集获得本体。旧区块的金块仍为原版金块。

## 开发入口

| 文件 | 作用 |
| --- | --- |
| `src/main/resources/data/gas/dimension/golden_realm.json` | 维度定义，连接噪声设置与固定群系 |
| `src/main/resources/data/gas/worldgen/noise_settings/golden_realm.json` | 主世界噪声路由、白石主体与草地/泥土表面规则、关闭其他流体/矿脉 |
| `src/main/resources/data/gas/worldgen/biome/golden_realm.json` | 淡紫天空、雾色与空装饰列表 |
| `src/main/java/com/gas/mod/world/GoldenRealm.java` | 权限检查、安全传送、返回主世界 |
| `src/main/java/com/gas/mod/registry/ModItems.java` | 原生 Forge 刷怪蛋注册与金绿双层染色 |

## 验证

在 `gas_mod` 目录执行：

```powershell
$env:JAVA_HOME='D:/AI/golden_apple_sword/toolchain/jdk/jdk-17.0.20.1+1'
& './gradlew.bat' -I scripts/build_with_mirror.gradle -I scripts/render_smoke.gradle -PgasSmoke=realm runClient --offline --console=plain
```

独立测试世界验证刷怪蛋实际使用和消耗、物品模型/染色、创造页、旧库存 1024→64 迁移、保留已兑换次数、第 64 次交易成功/第 65 次拒绝、定时补货，以及维度往返。
对实际生成区块取样，检查高度起伏和各高度的维度方块（新草地/泥土/石头，旧区块允许金块），确认群系天空色与客户端截图。
截图和结果：`art/golden_realm/runtime/`；测试源码不进入发布包。

1.4.0 时已在新建测试世界及早期版本测试存档的副本中完成全金块版本检查；两次各取样 55,296 个实际生成方块。旧存档首次加载可能显示 Minecraft 的实验性设置确认，确认后可进入。旧存档检查截图在 `art/golden_realm/runtime-existing/`。

1.7.0 的新区块分层、四方块模型和掉落专项检查使用 `-PgasSmoke=blocks`，结果在 `art/realm_blocks/runtime/`。
