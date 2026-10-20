package com.gas.mod;

import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;

/**
 * 金苹果镐（PRD §4.5）。
 *
 * <p>需要这个子类的原因：Minecraft 1.20.1 中 {@code PickaxeItem} 的构造器是 {@code protected}
 * （只有同包的 {@code Items} 能直接 new），外部包必须继承才能实例化。斧、锄同理；锹的构造器是 public。
 *
 * <p>攻击伤害 1 + Tier 加成 5.0 = 6，攻速修正 -2.8（PRD §6.2）。挖掘等级锁 2（铁级）。
 */
public class GoldenApplePickaxeItem extends PickaxeItem {

    public GoldenApplePickaxeItem(Tier tier, int attackDamageModifier, float attackSpeedModifier,
                                  Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }
}
