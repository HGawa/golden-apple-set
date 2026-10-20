package com.gas.mod;

import com.gas.mod.balance.GoldenAppleStats;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** 剑与四件工具共用的材质；速度与采集等级分别由 GoldenAppleStats 定义。 */
public final class GoldenAppleToolTier implements Tier {

    public static final GoldenAppleToolTier INSTANCE = new GoldenAppleToolTier();

    private GoldenAppleToolTier() {
    }

    @Override
    public int getUses() {
        return GoldenAppleStats.TOOL_DURABILITY;
    }

    @Override
    public float getSpeed() {
        return GoldenAppleStats.MINING_SPEED;
    }

    @Override
    public float getAttackDamageBonus() {
        return GoldenAppleStats.ATTACK_DAMAGE_BONUS;
    }

    @Override
    public int getLevel() {
        return GoldenAppleStats.HARVEST_LEVEL;
    }

    @Override
    public int getEnchantmentValue() {
        return GoldenAppleStats.ENCHANTMENT_VALUE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.GOLDEN_APPLE);
    }
}
