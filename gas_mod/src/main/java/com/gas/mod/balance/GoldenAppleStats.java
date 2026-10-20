package com.gas.mod.balance;

/** 金苹果系列的基础数值。行为逻辑与注册代码只引用此处，新增系列可独立定义参数。 */
public final class GoldenAppleStats {
    public static final int TOOL_DURABILITY = 512;
    public static final int SHIELD_DURABILITY = 512;
    public static final int BOW_DURABILITY = 512;
    // 下界合金为 9.0；只影响适用方块的基础挖掘速度，采集等级独立设置。
    public static final float MINING_SPEED = 12.0F;
    public static final int HARVEST_LEVEL = 2;
    public static final float ATTACK_DAMAGE_BONUS = 5.0F;
    public static final int ENCHANTMENT_VALUE = 22;

    public static final int HELMET_DURABILITY = 220;
    public static final int CHESTPLATE_DURABILITY = 320;
    public static final int LEGGINGS_DURABILITY = 300;
    public static final int BOOTS_DURABILITY = 260;
    public static final int HELMET_DEFENSE = 3;
    public static final int CHESTPLATE_DEFENSE = 8;
    public static final int LEGGINGS_DEFENSE = 6;
    public static final int BOOTS_DEFENSE = 3;
    public static final float ARMOR_TOUGHNESS = 2.0F;
    public static final float KNOCKBACK_RESISTANCE = 0.05F;
    public static final double FULL_SET_HEALTH_BONUS = 6.0;

    private GoldenAppleStats() { }
}
