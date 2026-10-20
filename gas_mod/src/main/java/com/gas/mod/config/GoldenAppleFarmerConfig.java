package com.gas.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** 服务端使用的 NPC 参数；单轮库存固定为 64。 */
public final class GoldenAppleFarmerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue SPAWN_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;
    public static final ForgeConfigSpec.IntValue EMERALD_COST;
    public static final ForgeConfigSpec.IntValue RESTOCK_TICKS;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("goldenAppleFarmer");
        ENABLED = builder.comment("Allow random spawns near players in inhabited villages.").define("enabled", true);
        SPAWN_INTERVAL = builder.comment("Ticks between village spawn checks (20 ticks = 1 second).")
                .defineInRange("spawnIntervalTicks", 1200, 20, 72000);
        SPAWN_CHANCE = builder.comment("Chance per nearby village per check; radius 96 permits at most one farmer.")
                .defineInRange("spawnChance", 0.25, 0.0, 1.0);
        EMERALD_COST = builder.comment("Base emerald cost for one regular golden apple; new farmers only.")
                .defineInRange("emeraldCost", 4, 1, 64);
        RESTOCK_TICKS = builder.comment("Restock interval in game ticks; no workstation needed.")
                .defineInRange("restockTicks", 6000, 200, 72000);
        builder.pop();
        SPEC = builder.build();
    }

    private GoldenAppleFarmerConfig() { }
}
