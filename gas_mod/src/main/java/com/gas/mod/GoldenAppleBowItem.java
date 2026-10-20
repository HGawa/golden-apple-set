package com.gas.mod;

import com.gas.mod.balance.GoldenAppleStats;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 金苹果弓 —— 原版弓行为（20 tick 蓄力 / 满蓄力箭伤害 9，见 PRD §4.10）+ 套装统一属性。
 *
 * <p>耐久 512 由注册时的 {@code props().durability(512)} 给出；
 * 附魔值 22 与修复材料金苹果在 PRD §3 里对 11 件统一要求，故此处覆盖原版默认值。
 *
 * <p>PRD §4.10 的两个被动（金枝 / 果核箭）属于机制层，本次未实现。
 * 拉弓 3 帧贴图 + pull/pulling 谓词模型由资产侧（ZCode）提供。
 */
public class GoldenAppleBowItem extends BowItem {

    public GoldenAppleBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEnchantmentValue() {
        return GoldenAppleStats.ENCHANTMENT_VALUE;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.GOLDEN_APPLE) || super.isValidRepairItem(toRepair, repair);
    }
}
