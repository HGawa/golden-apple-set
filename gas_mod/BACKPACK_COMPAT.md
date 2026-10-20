# 金苹果农夫 × 精妙背包 · 1.6.0

运行同一个游戏实例/服务器时，检测到 `sophisticatedbackpacks` 和 `sophisticatedcore` 才启用。金苹果套装本身不强制依赖这两个模组；只安装精妙核心时也不启用。精妙背包自身仍需安装对应版本的核心。

- 每位农夫从普通、铜、铁、金、钻石、下界合金背包中等概率选择一个；每个农夫独立选择。只使用已注册的背包物品。
- 背包放入 NPC 的胸部装备栏，使用精妙背包自带的生物渲染层、模型、材质和物品能力。保留农夫原有的草帽与金绿披肩。
- 自然生成、刷怪蛋、召唤命令，以及旧存档农夫加载时均适用。安装联动后不必重新寻找新区块。
- 装备和初始化标记随 NPC 保存，读档、区块重新加载不会重新抽取。已经占用胸部装备栏的农夫不会被覆盖。初始化后也不会自动补发被命令移走的背包。
- 联动生成的背包装备掉落概率设为 0。不增加背包交易、背包打开交互或额外战利品规则；右键农夫仍打开金苹果交易。

检查对象为用户提供的 Forge 1.20.1 文件：

- `sophisticatedbackpacks-1.20.1-3.26.12.2206.jar`
- `sophisticatedcore-1.20.1-1.5.12.2412.jar`

实现只引用 Forge 和 Minecraft 类，通过模组 ID 与物品注册表联动。精妙核心提供精妙背包使用的存储/渲染基础设施，由原模组加载。发布 JAR 不包含第三方类、材质、模型或第三方 JAR。代码入口为 `src/main/java/com/gas/mod/compat/FarmerBackpackCompat.java`。

## 开发验证

把上述原始 JAR 以不带中文前缀的文件名放入 `gas_mod/libs/compat/`。此目录仅用于本地验证，不参与普通构建。使用项目 JDK 17，在 `gas_mod` 目录执行：

```powershell
# 含精妙背包与精妙核心；ForgeGradle 在开发环境重映射第三方 JAR。
& './gradlew.bat' -I scripts/build_with_mirror.gradle -I scripts/render_smoke.gradle -I scripts/backpack_smoke_deps.gradle -PgasSmoke=backpack -PgasExpectBackpacks=true runClient --offline --console=plain
# 不安装第三方模组，检查可选依赖隔离。
& './gradlew.bat' -I scripts/build_with_mirror.gradle -I scripts/render_smoke.gradle -PgasSmoke=backpack runClient --offline --console=plain
```

测试在独立世界检查 64 次生成、随机范围、NBT 读写、旧 NPC、已有胸甲保护、装备网络同步、交易和原生渲染层；截图与结果分别写入 `art/farmer/backpack-present/`、`art/farmer/backpack-absent/`。

实际效果：[背面](art/farmer/backpack-present/01-back.png) · [侧面](art/farmer/backpack-present/02-side.png) · [正面](art/farmer/backpack-present/03-front.png)。
