package com.gas.mod;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 金苹果剑:攻击伤害 8(4 颗心)、攻速 1.6、耐久 512。
 * 被动「金辉」:命中 20% 概率给自己 再生 I(3 秒)。
 * 主动「开饭!」:长按右键食用 —— 饥饿 +6、伤害吸收 II(30 秒)、再生 II(10 秒),
 * 吃一口消耗 8 点耐久并进入 10 秒冷却。剑本身不会被吃掉。
 */
public class GoldenAppleSwordItem extends SwordItem {

    private static final float GOLDEN_GLOW_CHANCE = 0.2F;
    private static final int GOLDEN_GLOW_TICKS = 60;
    private static final int EAT_DURABILITY_COST = 8;
    private static final int EAT_COOLDOWN_TICKS = 200; // 10 秒

    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(6)
            .saturationMod(0.6F)
            .alwaysEat()
            .build();

    public GoldenAppleSwordItem() {
        // SwordItem(tier, 基础伤害加成 3, 攻速修正 -2.4 => 1.6 次/秒)
        // 总攻击伤害 = 3 + 5(tier) = 8
        super(GoldenAppleToolTier.INSTANCE, 3, -2.4F,
                new Item.Properties().rarity(Rarity.EPIC).food(FOOD));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hurt = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide && attacker.getRandom().nextFloat() < GOLDEN_GLOW_CHANCE) {
            attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, GOLDEN_GLOW_TICKS, 0), attacker);
        }
        return hurt;
    }

    /**
     * 食用完成:效果全部手动应用。绝不能调 super.finishUsingItem ——
     * 它会把 ItemStack shrink(1),而剑最大堆叠为 1,吃完就直接消失了(1.0.1 修复的 bug)。
     * 剑的代价是耐久(8 点),创造模式下 hurtAndBreak 内部会自动跳过耐久扣除。
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            if (entity instanceof Player player) {
                player.getFoodData().eat(6, 0.6F);
                player.awardStat(Stats.ITEM_USED.get(this));
            } else {
                entity.heal(6.0F);
            }
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1), entity);
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1), entity);
            entity.playSound(SoundEvents.PLAYER_BURP, 0.5F, entity.getRandom().nextFloat() * 0.1F + 0.9F);

            if (!stack.isEmpty()) {
                stack.hurtAndBreak(EAT_DURABILITY_COST, entity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
            if (entity instanceof Player p) {
                p.getCooldowns().addCooldown(this, EAT_COOLDOWN_TICKS);
            }
        }
        return stack;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("咬一口,再战一场。").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("金辉:命中目标有 20% 概率获得再生 I (3秒)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("开饭!:长按右键食用,消耗 8 点耐久,冷却 10 秒").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

}
