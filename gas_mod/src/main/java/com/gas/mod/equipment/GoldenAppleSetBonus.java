package com.gas.mod.equipment;

import java.util.UUID;
import com.gas.mod.GoldenAppleSetMod;
import com.gas.mod.balance.GoldenAppleStats;
import com.gas.mod.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 服务端唯一属性修饰符，完整穿戴才生效，不改玩家基础值，也不覆盖药水。 */
@Mod.EventBusSubscriber(modid = GoldenAppleSetMod.MODID)
public final class GoldenAppleSetBonus {
    public static final UUID HEALTH_ID = UUID.fromString("c0f6a252-439b-4532-af19-81c994f07759");
    private static final AttributeModifier HEALTH = new AttributeModifier(HEALTH_ID,
            "gas.golden_apple_full_set", GoldenAppleStats.FULL_SET_HEALTH_BONUS, AttributeModifier.Operation.ADDITION);

    public static boolean isWearingFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GOLDEN_APPLE_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GOLDEN_APPLE_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.GOLDEN_APPLE_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.GOLDEN_APPLE_BOOTS.get());
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        var player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide) return;
        var maximum = player.getAttribute(Attributes.MAX_HEALTH);
        if (maximum == null) return;
        if (player.isAlive() && isWearingFullSet(player)) {
            if (maximum.getModifier(HEALTH_ID) == null) maximum.addTransientModifier(HEALTH);
            var regen = player.getEffect(MobEffects.REGENERATION);
            // 60→10 后再续 60，周期恰为 50 tick，不会反复刷新而阻断再生 I 的治疗节拍。
            if (regen == null || (regen.getAmplifier() == 0 && !regen.isInfiniteDuration() && regen.getDuration() <= 10)) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
            }
        } else if (maximum.getModifier(HEALTH_ID) != null) {
            maximum.removeModifier(HEALTH_ID);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
        // 不主动 removeEffect：保留其他来源的再生。套装自己的短时再生会在脱装后自然到期。
    }

    private GoldenAppleSetBonus() { }
}
