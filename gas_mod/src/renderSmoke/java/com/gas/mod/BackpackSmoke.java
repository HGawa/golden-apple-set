package com.gas.mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.gas.mod.compat.FarmerBackpackCompat;
import com.gas.mod.entity.GoldenAppleFarmer;
import com.gas.mod.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "gas", value = Dist.CLIENT)
public final class BackpackSmoke {
    private static final boolean EXPECT = Boolean.getBoolean("gas.expectBackpacks");
    private static final Path OUT = Path.of("../../art/farmer/backpack-" + (EXPECT ? "present" : "absent")).toAbsolutePath().normalize();
    private static long started;
    private static int stage, ticks;
    private static UUID npcId;
    private static ItemStack selected;
    private static CompletableFuture<?> pending;
    private static void require(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
    private static void server(Minecraft mc, Runnable r) { pending = mc.getSingleplayerServer().submit(r); }
    private static void face(GoldenAppleFarmer npc, float angle) {
        npc.setYRot(angle); npc.yRotO = angle; npc.yBodyRot = angle; npc.yBodyRotO = angle;
        npc.setYHeadRot(angle); npc.yHeadRotO = angle;
    }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("gas.backpackSmoke") || event.phase != TickEvent.Phase.END || stage == 99) return;
        var mc = Minecraft.getInstance();
        try {
            if (started == 0) { started = System.currentTimeMillis(); Files.createDirectories(OUT); Files.writeString(OUT.resolve("result.txt"), "RUNNING\n"); }
            require(System.currentTimeMillis() - started < 240000, "timeout stage " + stage);
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (stage == 0) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                require(FarmerBackpackCompat.isAvailable() == EXPECT, "wrong optional dependency detection");
                mc.options.renderDistance().set(3); mc.options.simulationDistance().set(5); mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("gas-backpack-" + EXPECT + "-" + System.currentTimeMillis(),
                        new LevelSettings("Backpack QA", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(1L, false, false),
                        r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                stage = 1; return;
            }
            if (mc.player == null || mc.level == null || ++ticks < 50) return;
            ticks = 0;
            if (stage == 1) {
                mc.setScreen(null);
                server(mc, () -> {
                    var level = mc.getSingleplayerServer().overworld(); var p = level.players().get(0);
                    var choices = new HashSet<String>();
                    for (int i = 0; i < 64; i++) {
                        var sample = ModEntities.GOLDEN_APPLE_FARMER.get().create(level);
                        sample.getRandom().setSeed(i); sample.setNoAi(true); sample.moveTo(p.getX() + 12, p.getY(), p.getZ(), 0, 0);
                        require(level.addFreshEntity(sample), "spawn failed");
                        var chest = sample.getItemBySlot(EquipmentSlot.CHEST);
                        require(chest.isEmpty() != EXPECT, "wrong equipment presence");
                        if (EXPECT) {
                            choices.add(ForgeRegistries.ITEMS.getKey(chest.getItem()).toString());
                            var saved = new CompoundTag(); sample.save(saved);
                            require(saved.getList("ArmorDropChances", 5).getFloat(2) == 0, "accessory drop chance not zero");
                            sample.discard();
                            var loaded = (GoldenAppleFarmer) EntityType.loadEntityRecursive(saved, level, e -> e);
                            loaded.setUUID(UUID.randomUUID()); require(level.addFreshEntity(loaded), "reload failed");
                            require(ItemStack.matches(chest, loaded.getItemBySlot(EquipmentSlot.CHEST)), "backpack rerolled on reload");
                            loaded.discard();
                        } else {
                            require(!sample.getPersistentData().contains(FarmerBackpackCompat.ASSIGNED), "absent mod marks old NPC initialized");
                            sample.discard();
                        }
                    }
                    if (EXPECT) require(choices.size() == 6, "random selection did not cover 6 tiers: " + choices);
                    var occupied = ModEntities.GOLDEN_APPLE_FARMER.get().create(level);
                    occupied.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                    occupied.moveTo(p.getX() + 12, p.getY(), p.getZ(), 0, 0); level.addFreshEntity(occupied);
                    require(occupied.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "existing equipment overwritten"); occupied.discard();
                    // Simulate a saved pre-integration farmer, then join the actual level.
                    var old = ModEntities.GOLDEN_APPLE_FARMER.get().create(level); var tag = new CompoundTag(); old.save(tag);
                    var npc = (GoldenAppleFarmer) EntityType.loadEntityRecursive(tag, level, e -> e);
                    npc.setNoAi(true); npc.moveTo(p.getX(), p.getY(), p.getZ() + 3.5, 0, 0); face(npc, 0);
                    level.addFreshEntity(npc); npcId = npc.getUUID(); selected = npc.getItemBySlot(EquipmentSlot.CHEST).copy();
                    require(selected.isEmpty() != EXPECT, "legacy NPC not equipped");
                    require(npc.getOffers().size() == 1 && npc.getOffers().get(0).getMaxUses() == 64, "trades changed");
                    require(npc.getOffers().get(0).getResult().is(Items.GOLDEN_APPLE), "wrong trade");
                    System.out.println("GAS_BACKPACK_SMOKE PASS optional=" + EXPECT + ", choices=" + choices + ", save/reload, legacy NPC, occupied chest and trades");
                });
                mc.player.setYRot(0); mc.player.setXRot(0); stage = 2;
            } else if (stage == 2) {
                var npc = mc.level.getEntitiesOfClass(GoldenAppleFarmer.class, mc.player.getBoundingBox().inflate(8)).get(0);
                var clientStack = npc.getItemBySlot(EquipmentSlot.CHEST);
                System.out.println("GAS_BACKPACK_SMOKE equipment expected=" + selected.save(new CompoundTag())
                        + " client=" + clientStack.save(new CompoundTag()));
                // SB lazily initializes wrapper/settings NBT when the client renders a backpack.
                require(selected.isEmpty() ? clientStack.isEmpty() : clientStack.is(selected.getItem()) && clientStack.getCount() == 1,
                        "equipment item/count not synced");
                var renderer = mc.getEntityRenderDispatcher().getRenderer(npc);
                boolean found = false;
                for (var field : LivingEntityRenderer.class.getDeclaredFields()) {
                    if (field.getType() != List.class) continue;
                    field.setAccessible(true);
                    for (Object layer : (List<?>) field.get(renderer))
                        if (layer.getClass().getName().equals("net.p3pp3rf1y.sophisticatedbackpacks.client.render.BackpackLayerRenderer")) found = true;
                }
                require(found == EXPECT, "native backpack render layer missing/unexpected");
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(OUT.resolve("01-back.png")); }
                server(mc, () -> face((GoldenAppleFarmer) mc.getSingleplayerServer().overworld().getEntity(npcId), 90)); stage = 3;
            } else if (stage == 3) {
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(OUT.resolve("02-side.png")); }
                server(mc, () -> face((GoldenAppleFarmer) mc.getSingleplayerServer().overworld().getEntity(npcId), 180)); stage = 4;
            } else {
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(OUT.resolve("03-front.png")); }
                Files.writeString(OUT.resolve("result.txt"), "PASS optional=" + EXPECT + "; 64 spawns; six tiers when present; equipment persistence/sync; old NPC; existing chest respected; trades preserved; native render layer; front/side/back screenshots.\n");
                System.out.println("GAS_BACKPACK_SMOKE ALL PASS optional=" + EXPECT); stage = 99; mc.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace(); stage = 99;
            try { Files.writeString(OUT.resolve("result.txt"), "FAIL " + e); } catch (Exception ignored) { }
            mc.stop();
        }
    }
}
