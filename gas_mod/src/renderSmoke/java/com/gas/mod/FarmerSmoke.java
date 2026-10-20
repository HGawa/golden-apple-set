package com.gas.mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import com.gas.mod.config.GoldenAppleFarmerConfig;
import com.gas.mod.entity.GoldenAppleFarmer;
import com.gas.mod.registry.ModEntities;
import com.gas.mod.world.GoldenAppleFarmerSpawns;
import com.gas.mod.client.GoldenAppleFarmerRenderer;
import com.gas.mod.client.GoldenShoulderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Actual server/client integration in a disposable occupied-POI village fixture. */
@Mod.EventBusSubscriber(modid = "gas", value = Dist.CLIENT)
public final class FarmerSmoke {
    private static final Path OUT = Path.of("../../art/farmer/runtime").toAbsolutePath().normalize();
    private static int stage, ticks;
    private static long started;
    private static BlockPos center;
    private static UUID farmerId;
    private static CompletableFuture<?> pending;
    private static boolean configCaptured, originalEnabled;
    private static int originalInterval;
    private static double originalChance;

    private static void restoreConfig() {
        if (!configCaptured) return;
        GoldenAppleFarmerConfig.ENABLED.set(originalEnabled);
        GoldenAppleFarmerConfig.SPAWN_INTERVAL.set(originalInterval);
        GoldenAppleFarmerConfig.SPAWN_CHANCE.set(originalChance);
        configCaptured = false;
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void server(Minecraft mc, Runnable action) {
        pending = mc.getSingleplayerServer().submit(action);
    }

    private static GoldenAppleFarmer farmer(Minecraft mc) {
        return (GoldenAppleFarmer) mc.getSingleplayerServer().overworld().getEntity(farmerId);
    }

    private static void face(GoldenAppleFarmer npc, float angle) {
        npc.setYRot(angle); npc.yRotO = angle;
        npc.yBodyRot = angle; npc.yBodyRotO = angle;
        npc.setYHeadRot(angle); npc.yHeadRotO = angle;
    }

    private static void capture(Minecraft mc, String file) throws Exception {
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(OUT.resolve(file + ".png"));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("gas.npcSmoke") || event.phase != TickEvent.Phase.END || stage == 99) return;
        var mc = Minecraft.getInstance();
        try {
            if (started == 0) {
                started = System.currentTimeMillis(); Files.createDirectories(OUT);
                Files.writeString(OUT.resolve("result.txt"), "RUNNING\n");
            }
            require(System.currentTimeMillis() - started < 180000, "NPC test timed out at stage " + stage);
            if (pending != null) {
                if (!pending.isDone()) return;
                pending.join(); pending = null;
            }
            if (stage == 0) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                stage = 1;
                mc.options.renderDistance().set(3); mc.options.simulationDistance().set(5);
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("gas-farmer-smoke-" + System.currentTimeMillis(),
                        new LevelSettings("GAS farmer QA", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(0L, false, false),
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (mc.player == null || mc.level == null || ++ticks < 60) return;
            ticks = 0;
            if (stage == 1) {
                mc.setScreen(null);
                server(mc, () -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var player = level.players().get(0);
                    center = player.blockPosition();
                    require(!GoldenAppleFarmerSpawns.trySpawn(level, center), "spawned outside a village");
                    originalEnabled = GoldenAppleFarmerConfig.ENABLED.get();
                    originalInterval = GoldenAppleFarmerConfig.SPAWN_INTERVAL.get();
                    originalChance = GoldenAppleFarmerConfig.SPAWN_CHANCE.get();
                    configCaptured = true;
                    GoldenAppleFarmerConfig.ENABLED.set(false);
                    GoldenAppleFarmerConfig.SPAWN_INTERVAL.set(20);
                    GoldenAppleFarmerConfig.SPAWN_CHANCE.set(1.0);
                    level.setDayTime(1000);
                    var poi = center.offset(5, 0, 0);
                    level.setBlockAndUpdate(poi, Blocks.WHITE_BED.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.BedBlock.PART,
                                    net.minecraft.world.level.block.state.properties.BedPart.HEAD));
                    level.setBlockAndUpdate(poi.south(), Blocks.WHITE_BED.defaultBlockState());
                    level.getPoiManager().add(poi, level.registryAccess().registryOrThrow(Registries.POINT_OF_INTEREST_TYPE)
                            .getHolderOrThrow(PoiTypes.HOME));
                    require(level.getPoiManager().take(type -> type.is(PoiTypes.HOME),
                            (type, pos) -> pos.equals(poi), poi, 1).isPresent(), "could not occupy home POI");
                    var resident = EntityType.VILLAGER.create(level);
                    resident.moveTo(center.getX() + 2.5, center.getY(), center.getZ() + 2.5, 0, 0);
                    resident.setNoAi(true); level.addFreshEntity(resident);
                });
                stage = 2;
            } else if (stage == 2) {
                server(mc, () -> {
                    var level = mc.getSingleplayerServer().overworld();
                    require(level.isVillage(center), "occupied home did not establish village");
                    require(level.getEntitiesOfClass(GoldenAppleFarmer.class, new AABB(center).inflate(96)).isEmpty(),
                            "disabled config allowed spawn");
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, mc.getSingleplayerServer());
                    GoldenAppleFarmerConfig.ENABLED.set(true);
                });
                stage = 3;
            } else if (stage == 3) {
                server(mc, () -> {
                    var level = mc.getSingleplayerServer().overworld();
                    require(level.getEntitiesOfClass(GoldenAppleFarmer.class, new AABB(center).inflate(96)).isEmpty(),
                            "doMobSpawning=false allowed spawn");
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, mc.getSingleplayerServer());
                });
                stage = 4;
            } else if (stage == 4) {
                server(mc, () -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var found = level.getEntitiesOfClass(GoldenAppleFarmer.class, new AABB(center).inflate(96));
                    require(found.size() == 1, "expected one randomly spawned farmer, got " + found.size());
                    var npc = found.get(0); farmerId = npc.getUUID();
                    require(!GoldenAppleFarmerSpawns.trySpawn(level, center), "duplicate village spawn allowed");
                    GoldenAppleFarmerConfig.ENABLED.set(false);
                    require(npc.getOffers().size() == 1, "unexpected offers");
                    var offer = npc.getOffers().get(0);
                    require(offer.getBaseCostA().is(Items.EMERALD) && offer.getBaseCostA().getCount() == 4,
                            "wrong emerald price");
                    require(offer.getResult().is(Items.GOLDEN_APPLE) && offer.getMaxUses() == 64, "wrong product/stock");
                    var player = level.players().get(0);
                    npc.setTradingPlayer(player);
                    var menu = new MerchantMenu(1, player.getInventory(), npc);
                    menu.getSlot(0).set(new ItemStack(Items.EMERALD, 3));
                    require(menu.getSlot(2).getItem().isEmpty(), "insufficient emeralds allowed trade");
                    menu.getSlot(0).set(new ItemStack(Items.EMERALD, 8));
                    require(menu.getSlot(2).getItem().is(Items.GOLDEN_APPLE), "trade result missing");
                    menu.quickMoveStack(player, 2);
                    require(menu.getSlot(0).getItem().getCount() == 4, "trade did not charge 4 emeralds");
                    require(player.getInventory().countItem(Items.GOLDEN_APPLE) == 1 && offer.getUses() == 1,
                            "trade failed to deliver apple / record stock use");
                    offer.setToOutOfStock();
                    menu.getSlot(0).set(new ItemStack(Items.EMERALD, 4));
                    require(menu.getSlot(2).getItem().isEmpty(), "sold-out offer remained usable");
                    menu.removed(player); npc.setTradingPlayer(null);
                    var saved = new CompoundTag(); npc.save(saved);
                    var restored = EntityType.loadEntityRecursive(saved, level, entity -> entity);
                    require(restored instanceof GoldenAppleFarmer, "entity identity lost after NBT reload");
                    var loaded = (GoldenAppleFarmer) restored;
                    require(loaded.getOffers().get(0).isOutOfStock() && loaded.getOffers().size() == 1,
                            "reload reset stock or offers");
                    require(saved.getLong("GasNextRestock") > 0, "restock timer was not saved");
                    saved.putLong("GasNextRestock", level.getGameTime() - 1);
                    npc.readAdditionalSaveData(saved);
                    System.out.println("GAS_FARMER_SMOKE PASS random village spawn, no wilderness/duplicate spawn, config/gamerule gates, real trade, stock and NBT round trip");
                });
                stage = 5;
            } else if (stage == 5) {
                server(mc, () -> {
                    var npc = farmer(mc);
                    require(npc.getOffers().get(0).getUses() == 0, "scheduled restock failed");
                    npc.setVillagerData(npc.getVillagerData().setProfession(VillagerProfession.NONE).setLevel(5));
                    require(npc.getVillagerData().getProfession() == VillagerProfession.FARMER
                            && npc.getVillagerData().getLevel() == 1 && npc.getOffers().size() == 1,
                            "career change broke merchant");
                    npc.setNoAi(true);
                    npc.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 3.5, 180, 0);
                    face(npc, 180);
                    for (var resident : npc.level().getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,
                            new AABB(center).inflate(10), entity -> !(entity instanceof GoldenAppleFarmer))) {
                        resident.moveTo(center.getX() + 10.5, center.getY(), center.getZ() + 4.5, 0, 0);
                    }
                    System.out.println("GAS_FARMER_SMOKE PASS workstation-free restock and locked career");
                });
                mc.player.setYRot(0); mc.player.setXRot(0);
                stage = 6;
            } else if (stage == 6) {
                require(mc.level.getEntitiesOfClass(GoldenAppleFarmer.class, mc.player.getBoundingBox().inflate(32)).size() == 1,
                        "client did not receive custom entity");
                var npc = mc.level.getEntitiesOfClass(GoldenAppleFarmer.class, mc.player.getBoundingBox().inflate(32)).get(0);
                require(mc.getEntityRenderDispatcher().getRenderer(npc) instanceof GoldenAppleFarmerRenderer,
                        "NPC does not use its dedicated renderer");
                var shoulders = mc.getEntityModels().bakeLayer(GoldenShoulderLayer.LAYER).getChild("shoulders");
                require(!shoulders.getChild("left").isEmpty() && !shoulders.getChild("right").isEmpty(),
                        "paired shoulder geometry missing");
                capture(mc, "01-farmer");
                server(mc, () -> face(farmer(mc), 90));
                stage = 8;
            } else if (stage == 8) {
                capture(mc, "03-farmer-side");
                server(mc, () -> face(farmer(mc), 0));
                stage = 9;
            } else if (stage == 9) {
                capture(mc, "04-farmer-back");
                server(mc, () -> {
                    var npc = farmer(mc);
                    face(npc, 180);
                    var player = mc.getSingleplayerServer().overworld().players().get(0);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    npc.mobInteract(player, InteractionHand.MAIN_HAND);
                });
                stage = 7;
            } else if (stage == 7) {
                require(mc.screen instanceof MerchantScreen, "merchant UI failed to open/sync");
                capture(mc, "02-trading");
                Files.writeString(OUT.resolve("result.txt"), "PASS: random spawn in occupied-POI village fixture; wilderness/duplicate/config/gamerule prevention; actual emerald payment and apple delivery; out-of-stock blocking; NBT identity/stock/timer persistence; scheduled restock; locked career; dedicated client renderer, paired shoulder geometry and merchant UI; front/side/back screenshots. Test spawn chance forced to 1.0; production default 0.25.\n");
                restoreConfig();
                System.out.println("GAS_FARMER_SMOKE ALL PASS"); stage = 99; mc.stop();
            }
        } catch (Throwable error) {
            error.printStackTrace(); stage = 99; restoreConfig();
            try { Files.writeString(OUT.resolve("result.txt"), "FAIL: " + error + "\n"); } catch (Exception ignored) { }
            mc.stop();
        }
    }
}
