# 金苹果农夫 · 1.6.0

独立 NPC 实体 ID：`gas:golden_apple_farmer`。沿用原版农夫的草帽、村民模型、群系服装、行走与避险行为，1.3.1 增加金绿色立体双肩披肩，头顶与交易标题显示「金苹果农夫」，从 1.3.2 起英文客户端也显示这个中文名称。右键打开原版村民交易界面。

## 交易

1.6.0 新增[精妙背包可选联动](BACKPACK_COMPAT.md)：同时安装精妙背包与精妙核心后，农夫随机佩戴一个原模组背包，并保存款式；交易规则保持如下。

- 基础价格：4 颗绿宝石 → 1 个普通金苹果。
- 一轮库存固定为 64 次，每次 1 个，共 64 个金苹果；不出售附魔金苹果。1.4.0 会迁移旧 NPC 的库存上限，并保留本轮已兑换次数，不会因更新重新补满。
- 每 6000 游戏 tick（正常速度约 5 分钟）补满库存，不需要堆肥桶。正在交易时会延后至关闭界面；区块卸载期间不会运行 AI，重新加载后会检查已到期的补货。
- 保留原版适用的玩家折扣，不随需求涨价；不升级追加普通农夫交易，也不通过繁殖扩散。
- 保存与读档保留 NPC 身份、交易使用次数和下次补货时间。

## 村庄刷新

白天每 1200 游戏 tick（约 1 分钟）检查一次。玩家周围 64 格内存在成年村民，且该处被原版已占用的村庄 POI 判定为村庄时，每片检查区域有 25% 概率尝试生成。
多人同处一片区域不会重复抽取；村庄检查点周围 96 格已有金苹果农夫时跳过。生成会寻找附近有支撑、无水且没有碰撞的已加载地面，不替换现有村民，不主动加载新地形。

已有存档的村庄也适用，无需探索新区块；这不是每座村庄固定自带一个 NPC。废弃村庄、无居民区域、夜晚、旁观玩家以及 `doMobSpawning=false` 时不会自然刷新。
农夫会像普通村民一样受伤、死亡和遭遇怪物；远离玩家不会自然消失。死亡或被带离后，原村庄可能再次满足刷新条件。

创造模式「金苹果套装」页末尾新增金黄色、绿色斑点的「金苹果农夫刷怪蛋」。右键地面可生成同款带披肩 NPC；生存模式正常消耗刷怪蛋。
物品 ID：`gas:golden_apple_farmer_spawn_egg`，支持命令获取，不添加生存合成配方。

管理员可直接测试：

```mcfunction
/summon gas:golden_apple_farmer ~ ~ ~
```

## 配置与开发

首次启动后生成 `config/gas-common.toml`，联机时以服务器配置为准：

```toml
[goldenAppleFarmer]
enabled = true
spawnIntervalTicks = 1200
spawnChance = 0.25
emeraldCost = 4
restockTicks = 6000
```

价格参数用于新生成 NPC，已保存的价格保留原值。库存固定为 64，旧 `stock` 配置不再使用。修改补货间隔后，已有计时结束时采用新间隔。

- `config/GoldenAppleFarmerConfig.java`：数值与刷新开关。
- `registry/ModEntities.java`：实体与属性注册。
- `entity/GoldenAppleFarmer.java`：专属交易、补货、存档与职业保持。
- `world/GoldenAppleFarmerSpawns.java`：服务器村庄检测和安全生成。
- `client/GoldenAppleFarmerRenderer.java`：继承原版农夫渲染，添加专属披肩。
- `client/GoldenShoulderLayer.java`：双肩原生 Java 模型、UV 与身体姿势同步。
- `GoldenAppleClientSetup.java`：注册专属渲染器与模型层。

以上 Java 路径相对于 `src/main/java/com/gas/mod/`。不需要额外模型库或其他模组。

## 回归检查

在 `gas_mod` 执行：

```powershell
$env:JAVA_HOME='D:/AI/golden_apple_sword/toolchain/jdk/jdk-17.0.20.1+1'
& './gradlew.bat' -I scripts/build_with_mirror.gradle -I scripts/render_smoke.gradle -PgasSmoke=npc runClient --offline --console=plain
```

测试在独立平坦世界建立有占用床位 POI 与成年村民的村庄环境，临时将刷新概率设为 100%、间隔设为 20 tick，结束后恢复配置。
覆盖实际刷新事件、野外拒绝、重复生成限制、配置及游戏规则、交易扣款与交付、售罄、NBT 读回、定时补货、职业保持、客户端实体显示与交易界面。
结果和截图位于 `art/farmer/runtime/`。测试类不进入正式 JAR；这不等同于用户整合包兼容性测试。
