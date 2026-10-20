package com.gas.mod;

import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;

/**
 * 金苹果锹（PRD §4.7）。
 *
 * <p>原版 {@code ShovelItem} 构造器是 public，这个子类是为 4 件工具保持统一结构，
 * 并给 PRD §4.7 的机制（沃土 / 不费劲儿）留出落点。
 *
 * <p>攻击伤害 1.5 + Tier 加成 5.0 = 6.5，攻速修正 -3.0（PRD §6.2）。
 */
public class GoldenAppleShovelItem extends ShovelItem {

    public GoldenAppleShovelItem(Tier tier, float attackDamageModifier, float attackSpeedModifier,
                                 Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }
}
