package com.gas.mod;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class GoldenAppleArmorItem extends ArmorItem {
    public GoldenAppleArmorItem(Type type, Properties properties) {
        super(GoldenAppleArmorMaterial.INSTANCE, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.gas.armor.full_set").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.gas.armor.bonus").withStyle(ChatFormatting.GREEN));
    }
}
