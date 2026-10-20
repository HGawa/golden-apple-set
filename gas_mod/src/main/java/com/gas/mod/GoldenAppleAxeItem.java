package com.gas.mod;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tier;

/**
 * 金苹果斧（PRD §4.6）。
 *
 * <p>需要这个子类的原因：Minecraft 1.20.1 中 {@code AxeItem} 的构造器是 {@code protected}，
 * 外部包必须继承才能实例化。
 *
 * <p>攻击伤害 5 + Tier 加成 5.0 = 10，攻速修正 -3.0（PRD §6.2）。挖掘等级锁 2（铁级）。
 */
public class GoldenAppleAxeItem extends AxeItem {

    public GoldenAppleAxeItem(Tier tier, float attackDamageModifier, float attackSpeedModifier,
                              Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }
}
