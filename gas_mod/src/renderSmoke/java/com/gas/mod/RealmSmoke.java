package com.gas.mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.gas.mod.entity.GoldenAppleFarmer;
import com.gas.mod.registry.ModEntities;
import com.gas.mod.registry.ModItems;
import com.gas.mod.registry.ModBlocks;
import com.gas.mod.world.GoldenRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "gas", value = Dist.CLIENT)
public final class RealmSmoke {
    private static final String EXISTING = System.getProperty("gas.existingWorld", "");
    private static final Path OUT = Path.of("../../art/golden_realm/" +
            (EXISTING.isBlank() ? "runtime" : "runtime-existing")).toAbsolutePath().normalize();
    private static long started;
    private static int stage, ticks;
    private static CompletableFuture<?> pending;
    private static UUID merchantId;

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void server(Minecraft mc, Runnable action) {
        pending = mc.getSingleplayerServer().submit(action);
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(OUT.resolve(name + ".png"));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("gas.realmSmoke") || event.phase != TickEvent.Phase.END || stage == 99) return;
        var mc = Minecraft.getInstance();
        try {
            if (started == 0) {
                started = System.currentTimeMillis(); Files.createDirectories(OUT);
                Files.writeString(OUT.resolve("result.txt"), "RUNNING\n");
            }
            require(System.currentTimeMillis() - started < 240000, "realm test timeout stage " + stage);
            if (pending != null) {
                if (!pending.isDone()) return;
                pending.join(); pending = null;
            }
            if (stage == 0) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                stage = 1;
                mc.options.renderDistance().set(8); mc.options.simulationDistance().set(5);
                mc.options.pauseOnLostFocus = false;
                if (!EXISTING.isBlank()) {
                    mc.createWorldOpenFlows().loadLevel(mc.screen, EXISTING);
                    return;
                }
                mc.createWorldOpenFlows().createFreshLevel("gas-realm-smoke-" + System.currentTimeMillis(),
                        new LevelSettings("GAS golden realm QA", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(12345L, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (mc.player == null || mc.level == null || ++ticks < 60) return;
            ticks = 0;
            if (stage == 1) {
                mc.setScreen(null);
                server(mc, () -> {
                    var server = mc.getSingleplayerServer(); var level = server.overworld();
                    var player = level.players().get(0);
                    require(server.getLevel(GoldenRealm.KEY) != null, "dimension failed to register from data pack");
                    var egg = ModItems.GOLDEN_APPLE_FARMER_SPAWN_EGG.get();
                    require(ForgeSpawnEggItem.fromEntityType(ModEntities.GOLDEN_APPLE_FARMER.get()) == egg, "egg entity lookup missing");
                    player.setGameMode(GameType.SURVIVAL);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(egg, 2));
                    var ground = player.blockPosition().offset(3, -1, 0);
                    var hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false);
                    require(egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "egg use failed");
                    require(player.getMainHandItem().getCount() == 1, "survival egg was not consumed");
                    var npcs = level.getEntitiesOfClass(GoldenAppleFarmer.class, player.getBoundingBox().inflate(16));
                    require(npcs.size() == 1, "egg spawned wrong entity count");
                    var npc = npcs.get(0); merchantId = npc.getUUID();
                    require(npc.getOffers().get(0).getMaxUses() == 64, "fresh NPC stock not 64");
                    // Simulate an older saved trade with 1024 uses allowed and 63 purchases already made.
                    var tag = new CompoundTag(); npc.save(tag);
                    var offer = tag.getCompound("Offers").getList("Recipes", 10).getCompound(0);
                    offer.putInt("maxUses", 1024); offer.putInt("uses", 63);
                    tag.putLong("GasNextRestock", level.getGameTime() + 6000);
                    npc.readAdditionalSaveData(tag);
                    require(npc.getOffers().get(0).getMaxUses() == 64 && npc.getOffers().get(0).getUses() == 63,
                            "migration reset purchases or failed to cap stock");
                    npc.setTradingPlayer(player);
                    var menu = new MerchantMenu(1, player.getInventory(), npc);
                    menu.getSlot(0).set(new ItemStack(Items.EMERALD, 8));
                    menu.quickMoveStack(player, 2);
                    require(player.getInventory().countItem(Items.GOLDEN_APPLE) == 1
                            && npc.getOffers().get(0).getUses() == 64 && menu.getSlot(0).getItem().getCount() == 4,
                            "64th transaction failed");
                    menu.getSlot(0).set(new ItemStack(Items.EMERALD, 4));
                    require(menu.getSlot(2).getItem().isEmpty(), "65th transaction allowed");
                    menu.removed(player); npc.setTradingPlayer(null);
                    offer.putInt("uses", 100); npc.readAdditionalSaveData(tag);
                    require(npc.getOffers().get(0).isOutOfStock(), "over-cap legacy NPC regained stock");
                    tag.putLong("GasNextRestock", level.getGameTime() - 1); npc.readAdditionalSaveData(tag);
                    player.setGameMode(GameType.CREATIVE);
                    System.out.println("GAS_REALM_SMOKE PASS real spawn egg use/consumption; saved stock migration; 64th trade succeeds and 65th blocked");
                });
                stage = 2;
            } else if (stage == 2) {
                server(mc, () -> {
                    var npc = (GoldenAppleFarmer) mc.getSingleplayerServer().overworld().getEntity(merchantId);
                    require(npc.getOffers().get(0).getUses() == 0 && npc.getOffers().get(0).getMaxUses() == 64,
                            "restock did not reset to 64");
                });
                var stack = new ItemStack(ModItems.GOLDEN_APPLE_FARMER_SPAWN_EGG.get());
                require(mc.getItemColors().getColor(stack, 0) == 0xF6C945 && mc.getItemColors().getColor(stack, 1) == 0x4E9B3C,
                        "spawn egg tint layers incorrect");
                require(mc.getItemRenderer().getItemModelShaper().getItemModel(stack)
                        != mc.getModelManager().getMissingModel(), "missing spawn egg model");
                RenderSmoke.openCreativeTab(mc);
                stage = 3;
            } else if (stage == 3) {
                capture(mc, "01-spawn-egg-tab"); mc.setScreen(null);
                server(mc, () -> {
                    var server = mc.getSingleplayerServer(); var player = server.overworld().players().get(0);
                    server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2), "gas gold");
                    require(player.level().dimension().equals(GoldenRealm.KEY), "safe dimension command failed");
                    var level = server.getLevel(GoldenRealm.KEY);
                    var ground = level.getBlockState(player.blockPosition().below());
                    require(ground.is(ModBlocks.GOLDEN_APPLE_GRASS_BLOCK.get()) || (!EXISTING.isBlank() && ground.is(Blocks.GOLD_BLOCK)), "teleport not on realm ground");
                    int min = 320, max = -64, checked = 0;
                    for (int cx = -1; cx <= 1; cx++) for (int cz = -1; cz <= 1; cz++) {
                        var chunk = level.getChunk(cx, cz);
                        for (int x = 0; x < 16; x += 4) for (int z = 0; z < 16; z += 4) {
                            int wx = cx * 16 + x, wz = cz * 16 + z;
                            int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
                            min = Math.min(min, h); max = Math.max(max, h);
                            for (int y = -64; y < 320; y++) {
                                var state = chunk.getBlockState(new BlockPos(wx, y, wz));
                                require(state.isAir() || state.is(ModBlocks.GOLDEN_APPLE_GRASS_BLOCK.get())
                                        || state.is(ModBlocks.GOLDEN_APPLE_DIRT.get()) || state.is(ModBlocks.GOLDEN_APPLE_STONE.get())
                                        || (!EXISTING.isBlank() && state.is(Blocks.GOLD_BLOCK)), "unexpected realm terrain: " + state + " at " + wx + "," + y + "," + wz);
                                checked++;
                            }
                        }
                    }
                    require(max > min + 1, "terrain has no natural height variation");
                    require(level.getBiome(player.blockPosition()).value().getSkyColor() == 0xC6B0FF, "wrong sky color");
                    level.setDayTime(6000);
                    player.getAbilities().flying = true; player.onUpdateAbilities();
                    player.teleportTo(level, player.getX(), Math.max(max + 45, 100), player.getZ(), -45, 15);
                    System.out.println("GAS_REALM_SMOKE PASS dimension data, safe arrival, " + checked
                            + " sampled realm blocks, height range " + min + ".." + max + ", lavender sky");
                });
                stage = 4;
            } else if (stage == 4) {
                require(mc.level.dimension().equals(GoldenRealm.KEY), "client dimension sync failed");
                // Let distant terrain meshes finish before the actual client screenshot.
                mc.getToasts().clear();
                mc.options.hideGui = true;
                stage = 5;
            } else if (stage == 5) {
                capture(mc, "02-golden-realm");
                mc.options.hideGui = false;
                server(mc, () -> {
                    var server = mc.getSingleplayerServer(); var player = server.getLevel(GoldenRealm.KEY).players().get(0);
                    server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2), "gas overworld");
                    require(player.level().dimension().equals(Level.OVERWORLD), "return command failed");
                });
                stage = 6;
            } else if (stage == 6) {
                require(mc.level.dimension().equals(Level.OVERWORLD), "client failed to return");
                Files.writeString(OUT.resolve("result.txt"), "PASS: spawn egg entity/consumption/tints/model and creative tab; legacy stock migration preserves used count; 64th trade succeeds, 65th blocked; timed restock to 64; golden dimension registration and travel both ways; 55296 sampled generated blocks gold or air only; non-flat terrain and lavender sky; actual client screenshots.\n");
                System.out.println("GAS_REALM_SMOKE ALL PASS"); stage = 99; mc.stop();
            }
        } catch (Throwable error) {
            error.printStackTrace(); stage = 99; mc.options.hideGui = false;
            try { Files.writeString(OUT.resolve("result.txt"), "FAIL: " + error + "\n"); } catch (Exception ignored) { }
            mc.stop();
        }
    }
}
