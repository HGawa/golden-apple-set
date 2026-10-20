package com.gas.mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.gas.mod.equipment.GoldenAppleSetBonus;
import com.gas.mod.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in integration checks; excluded from release JARs. */
@Mod.EventBusSubscriber(modid = "gas", value = Dist.CLIENT)
public final class ArmorSmoke {
    private static final Path OUT = Path.of("../../art/armor_set/runtime").toAbsolutePath().normalize();
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final UUID OTHER = UUID.fromString("787c8d21-5a3d-4939-aaf5-5ea478d439cc");
    private static int stage, ticks;
    private static long started;
    private static CompletableFuture<?> pending;

    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void server(Minecraft mc, Runnable action) { pending = mc.getSingleplayerServer().submit(action); }
    private static Player player(Minecraft mc) { return mc.getSingleplayerServer().overworld().players().get(0); }
    private static void equip(Player p) {
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GOLDEN_APPLE_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.GOLDEN_APPLE_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.GOLDEN_APPLE_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.GOLDEN_APPLE_BOOTS.get()));
    }
    private static void full(Player p, float maximum) {
        require(p.getArmorValue() == 20, "armor total is not 20");
        require(p.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 8, "toughness total is not 8");
        require(p.getMaxHealth() == maximum, "wrong maximum health: " + p.getMaxHealth());
        require(p.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream()
                .filter(m -> m.getId().equals(GoldenAppleSetBonus.HEALTH_ID)).count() == 1, "bonus missing/stacked");
    }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("gas.armorSmoke") || event.phase != TickEvent.Phase.END || stage == 99) return;
        var mc = Minecraft.getInstance();
        try {
            if (started == 0) {
                started = System.currentTimeMillis(); Files.createDirectories(OUT);
                Files.writeString(OUT.resolve("result.txt"), "RUNNING\n");
            }
            require(System.currentTimeMillis() - started < 240000, "armor test timeout stage " + stage);
            if (pending != null) {
                if (!pending.isDone()) return;
                pending.join(); pending = null;
            }
            if (stage == 0) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                stage = 1;
                mc.options.renderDistance().set(3); mc.options.simulationDistance().set(5);
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("gas-armor-smoke-" + System.currentTimeMillis(),
                        new LevelSettings("GAS armor QA", GameType.SURVIVAL, false, Difficulty.NORMAL,
                                true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(0L, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (mc.player == null || mc.level == null || ++ticks < (stage == 2 || stage == 11 ? 140 : 40)) return;
            ticks = 0;
            if (stage == 1) {
                mc.setScreen(null);
                server(mc, () -> {
                    var s = mc.getSingleplayerServer(); var p = player(mc);
                    s.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, s);
                    s.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, s);
                    s.overworld().setDayTime(6000);
                    p.setInvulnerable(true);
                    equip(p); p.setHealth(10);
                    for (var slot : SLOTS) {
                        var item = (ArmorItem) p.getItemBySlot(slot).getItem();
                        require(item.getDefense() == ArmorMaterials.NETHERITE.getDefenseForType(item.getType()), "defense mismatch " + slot);
                        require(item.getToughness() == ArmorMaterials.DIAMOND.getToughness(), "toughness mismatch " + slot);
                    }
                });
                stage = 2;
            } else if (stage == 2) {
                full(mc.player, 26);
                require(mc.player.hasEffect(MobEffects.REGENERATION), "regen not synced to client");
                server(mc, () -> {
                    var p = player(mc); full(p, 26);
                    require(p.getHealth() >= 12, "regeneration did not actually heal: " + p.getHealth());
                    require(p.getEffect(MobEffects.REGENERATION).getAmplifier() == 0, "not regeneration I");
                    require(p.getAttribute(Attributes.MAX_HEALTH).save().getList("Modifiers", 10).isEmpty(), "transient bonus saved permanently");
                    System.out.println("GAS_ARMOR_SMOKE PASS defense 20, toughness 8, max health 26, client sync, real healing " + p.getHealth());
                    p.setHealth(26);
                });
                stage = 3;
            } else if (stage == 3) {
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(OUT.resolve("01-full-set-hearts.png")); }
                mc.setScreen(new InventoryScreen(mc.player)); stage = 4;
            } else if (stage == 4) {
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(OUT.resolve("02-full-set-inventory.png")); }
                mc.setScreen(null);
                server(mc, () -> player(mc).setItemSlot(SLOTS[0], ItemStack.EMPTY)); stage = 5;
            } else if (stage >= 5 && stage <= 8) {
                final int missing = stage - 5;
                server(mc, () -> {
                    var p = player(mc);
                    require(p.getMaxHealth() == 20 && p.getHealth() <= 20, "missing slot still grants bonus " + SLOTS[missing]);
                    equip(p);
                    if (missing < 3) p.setItemSlot(SLOTS[missing + 1], ItemStack.EMPTY);
                });
                stage++;
            } else if (stage == 9) {
                server(mc, () -> {
                    var p = player(mc); full(p, 26);
                    p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                    p.getInventory().add(new ItemStack(ModItems.GOLDEN_APPLE_HELMET.get()));
                }); stage = 10;
            } else if (stage == 10) {
                server(mc, () -> require(player(mc).getMaxHealth() == 20, "inventory or mixed set grants bonus")); stage = 11;
            } else if (stage == 11) {
                server(mc, () -> {
                    var p = player(mc);
                    require(!p.hasEffect(MobEffects.REGENERATION), "set regeneration remained after unequip");
                    p.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(OTHER, "other health source", 4, AttributeModifier.Operation.ADDITION));
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1000, 1)); equip(p);
                }); stage = 12;
            } else if (stage == 12) {
                server(mc, () -> {
                    var p = player(mc); full(p, 30);
                    require(p.getEffect(MobEffects.REGENERATION).getAmplifier() == 1, "strong regeneration downgraded");
                    p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                }); stage = 13;
            } else if (stage == 13) {
                server(mc, () -> {
                    var p = player(mc);
                    require(p.getMaxHealth() == 24, "other health bonus removed");
                    require(p.getEffect(MobEffects.REGENERATION).getAmplifier() == 1, "potion removed on unequip");
                    p.removeEffect(MobEffects.REGENERATION);
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 0)); equip(p);
                }); stage = 14;
            } else if (stage == 14) {
                server(mc, () -> {
                    var p = player(mc); full(p, 30);
                    require(p.getEffect(MobEffects.REGENERATION).isInfiniteDuration(), "infinite regeneration replaced");
                    System.out.println("GAS_ARMOR_SMOKE PASS all four missing slots, re-equip, mixed set/inventory, health clamp, fade, other health/potions, transient persistence");
                }); stage = 15;
            } else {
                Files.writeString(OUT.resolve("result.txt"), "PASS\nDefense 20; toughness 8; full set +6 max health; actual regeneration I healing; client sync; no stacking; all missing slots; mixed armor; inventory only; clamp; expiry; other modifiers and stronger/infinite potions preserved; transient modifier not saved.\n");
                System.out.println("GAS_ARMOR_SMOKE ALL PASS"); stage = 99; mc.stop();
            }
        } catch (Throwable error) {
            error.printStackTrace(); stage = 99;
            try { Files.writeString(OUT.resolve("result.txt"), "FAIL\n" + error); } catch (Exception ignored) { }
            mc.stop();
        }
    }
}
