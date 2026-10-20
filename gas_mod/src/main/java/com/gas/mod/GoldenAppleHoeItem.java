package com.gas.mod;

import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tier;

/**
 * 金苹果锄（PRD §4.8）。
 *
 * <p>需要这个子类的原因：Minecraft 1.20.1 中 {@code HoeItem} 的构造器是 {@code protected}，
 * 外部包必须继承才能实例化。
 *
 * <p>攻击伤害 -4 + Tier 加成 5.0 = 1，攻速修正 0.0（PRD §6.2）。
 */
public class GoldenAppleHoeItem extends HoeItem {

    public GoldenAppleHoeItem(Tier tier, int attackDamageModifier, float attackSpeedModifier,
                              Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }
}
