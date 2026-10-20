package com.gas.mod;

import com.gas.mod.registry.ModItems;
import com.gas.mod.registry.ModCreativeTabs;

import java.nio.file.Files;
import java.nio.file.Path;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
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

/** Opt-in runtime regression harness; independent disposable world, no user saves. */
@Mod.EventBusSubscriber(modid = "gas", value = Dist.CLIENT)
public final class RenderSmoke {
    private static int stage, ticks;
    private static long started;
    private static final Path OUT = Path.of("../../art/shield/runtime").toAbsolutePath().normalize();
    private static final ResourceLocation BLOCKING = new ResourceLocation("minecraft", "blocking");

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(OUT.resolve(name + ".png"));
        }
        System.out.println("GAS_RENDER_SMOKE screenshot " + name);
    }

    private static void equip(Minecraft mc, boolean shieldInMainHand) {
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            player.getInventory().selected = 0;
            player.getInventory().setItem(0, new ItemStack(shieldInMainHand
                    ? ModItems.GOLDEN_APPLE_SHIELD.get() : ModItems.GOLDEN_APPLE_PICKAXE.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, shieldInMainHand
                    ? ItemStack.EMPTY : new ItemStack(ModItems.GOLDEN_APPLE_SHIELD.get()));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void checkTools(Minecraft mc) {
        Item[] ours = {ModItems.GOLDEN_APPLE_PICKAXE.get(), ModItems.GOLDEN_APPLE_AXE.get(),
                      ModItems.GOLDEN_APPLE_SHOVEL.get(), ModItems.GOLDEN_APPLE_HOE.get()};
        Item[] vanilla = {Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE};
        for (int i = 0; i < ours.length; i++) {
            var actual = mc.getItemRenderer().getItemModelShaper().getItemModel(ours[i]);
            var expected = mc.getItemRenderer().getItemModelShaper().getItemModel(vanilla[i]);
            for (var context : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND}) {
                require(actual.getTransforms().getTransform(context).equals(expected.getTransforms().getTransform(context)),
                        ours[i] + " wrong transform " + context);
            }
        }
        System.out.println("GAS_RENDER_SMOKE PASS four tools x four hand contexts match vanilla");
    }

    private static void checkShield(Minecraft mc, InteractionHand hand, boolean active) {
        var stack = mc.player.getItemInHand(hand);
        require(stack.is(ModItems.GOLDEN_APPLE_SHIELD.get()), "shield missing from " + hand);
        var property = ItemProperties.getProperty(stack.getItem(), BLOCKING);
        require(property != null, "blocking property unregistered");
        require(property.call(stack, mc.level, mc.player, 0) == (active ? 1 : 0), "wrong blocking value " + hand);
        require(property.call(stack, mc.level, null, 0) == 0, "GUI/null-entity must be idle");
        require(property.call(stack.copy(), mc.level, mc.player, 0) == 0, "inactive stack must stay idle");
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        BakedModel base = mc.getItemRenderer().getItemModelShaper().getItemModel(stack);
        require(model.isCustomRenderer(), "shield lost its 3D renderer");
        require((model != base) == active, "blocking override did not select the correct model");
        System.out.println("GAS_RENDER_SMOKE PASS " + hand + (active ? " blocking" : " idle"));
    }

    private static void checkMining() {
        Item[] ours = {ModItems.GOLDEN_APPLE_PICKAXE.get(), ModItems.GOLDEN_APPLE_AXE.get(),
                ModItems.GOLDEN_APPLE_SHOVEL.get(), ModItems.GOLDEN_APPLE_HOE.get()};
        Item[] vanilla = {Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE};
        Block[] blocks = {Blocks.STONE, Blocks.OAK_LOG, Blocks.DIRT, Blocks.OAK_LEAVES};
        for (int i = 0; i < ours.length; i++) {
            ItemStack stack = new ItemStack(ours[i]);
            float speed = stack.getDestroySpeed(blocks[i].defaultBlockState());
            float baseline = new ItemStack(vanilla[i]).getDestroySpeed(blocks[i].defaultBlockState());
            require(speed == 12.0F && baseline == 9.0F && speed > baseline, "mining speed regression: " + ours[i]);
            require(stack.getDestroySpeed(Blocks.GLASS.defaultBlockState()) == 1.0F, "wrong-tool speed must stay vanilla");
            require(stack.getMaxDamage() == 512, "durability changed: " + ours[i]);
            require(((TieredItem) ours[i]).getTier() == GoldenAppleToolTier.INSTANCE, "shared tier missing");
            System.out.println("GAS_RENDER_SMOKE PASS mining " + ours[i] + " on " + blocks[i] + ": " + speed + " > " + baseline);
        }
        var pickaxe = new ItemStack(ours[0]);
        require(pickaxe.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "iron-level ore harvesting regressed");
        require(!pickaxe.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "harvest level unexpectedly changed");
        require(((TieredItem) ModItems.GOLDEN_APPLE_SWORD.get()).getTier() == GoldenAppleToolTier.INSTANCE,
                "sword still uses a separate tier");
        require(GoldenAppleToolTier.INSTANCE.getSpeed() > Tiers.NETHERITE.getSpeed(), "tier speed too low");
    }

    public static void openCreativeTab(Minecraft mc) throws Exception {
        checkMining();
        var screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(screen);
        var tab = ModCreativeTabs.GOLDEN_APPLE_SET.get();
        var expected = java.util.stream.Stream.concat(ModItems.GOLDEN_APPLE_SET.stream().map(item -> item.get()),
                com.gas.mod.registry.ModBlocks.TERRAIN.stream().map(block -> block.get().asItem())).toList();
        var actual = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
        require(actual.size() == 16 && actual.equals(expected), "creative tab missing/duplicate/out-of-order items");
        require(tab.getIconItem().is(ModItems.GOLDEN_APPLE_SWORD.get()), "wrong creative icon");
        require(!tab.getDisplayName().getString().equals("itemGroup.gas.golden_apple_set"), "untranslated tab title");
        for (var item : expected) {
            require(CreativeModeTabs.searchTab().contains(new ItemStack(item)), "global search lost " + item);
        }
        // Reflection only in this optional development harness; production uses public Forge APIs.
        var pagesField = CreativeModeInventoryScreen.class.getDeclaredField("pages");
        pagesField.setAccessible(true);
        for (Object value : (java.util.List<?>) pagesField.get(screen)) {
            var page = (CreativeTabsScreenPage) value;
            if (page.getVisibleTabs().contains(tab)) screen.setCurrentPage(page);
        }
        require(screen.getCurrentPage().getVisibleTabs().contains(tab), "custom tab not reachable in creative UI");
        var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
        select.setAccessible(true);
        select.invoke(screen, tab);
        System.out.println("GAS_RENDER_SMOKE PASS creative tab: 16 ordered items, localized title, icon, global search, reachable page");
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("gas.renderSmoke") || event.phase != TickEvent.Phase.END || stage == 99) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (started == 0) { started = System.currentTimeMillis(); Files.createDirectories(OUT); }
            require(System.currentTimeMillis() - started < 180_000, "client test timed out");
            if (stage == 0) {
                // Fresh installations show accessibility onboarding before the title.
                if (mc.screen == null || mc.getOverlay() != null) return;
                stage = 1;
                mc.options.renderDistance().set(3);
                mc.options.simulationDistance().set(3);
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("gas-render-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("GAS render QA", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                        true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(0L, false, false),
                    registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (mc.player == null || mc.level == null) return;
            if (stage == 1) {
                if (++ticks < 80) return;
                mc.setScreen(null); mc.player.setXRot(0); mc.player.setYRot(0);
                checkTools(mc); equip(mc, false); stage = 2; ticks = 0; return;
            }
            if (++ticks < 40) return;
            ticks = 0;
            if (stage == 2) {
                checkShield(mc, InteractionHand.OFF_HAND, false);
                capture(mc, "01-tool-and-offhand-idle");
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
                stage = 3;
            } else if (stage == 3) {
                checkShield(mc, InteractionHand.OFF_HAND, true);
                capture(mc, "02-offhand-blocking");
                mc.options.keyUse.setDown(false); mc.gameMode.releaseUsingItem(mc.player);
                equip(mc, true); stage = 4;
            } else if (stage == 4) {
                checkShield(mc, InteractionHand.MAIN_HAND, false);
                capture(mc, "03-mainhand-idle");
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                stage = 5;
            } else if (stage == 5) {
                checkShield(mc, InteractionHand.MAIN_HAND, true);
                capture(mc, "04-mainhand-blocking");
                mc.options.keyUse.setDown(false); mc.gameMode.releaseUsingItem(mc.player);
                stage = 6;
            } else if (stage == 6) {
                checkShield(mc, InteractionHand.MAIN_HAND, false);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                stage = 7;
            } else if (stage == 7) {
                checkShield(mc, InteractionHand.MAIN_HAND, true);
                capture(mc, "05-thirdperson-shield-front");
                mc.options.keyUse.setDown(false); mc.gameMode.releaseUsingItem(mc.player);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                openCreativeTab(mc);
                stage = 8;
            } else if (stage == 8) {
                capture(mc, "06-creative-tab");
                Files.writeString(OUT.resolve("result.txt"), "PASS: 4 tools x 4 hand transforms; shield main/offhand idle/blocking/release; null entity and inactive stack; 4 tools mining speed 12 > netherite 9 on suitable blocks; wrong-tool speed 1; durability and harvest level preserved; shared sword tier; creative tab 16 ordered items, localized title, icon, global search and reachable page; 6 actual client screenshots.\n");
                System.out.println("GAS_RENDER_SMOKE ALL PASS"); stage = 99; mc.stop();
            }
        } catch (Throwable error) {
            stage = 99; mc.options.keyUse.setDown(false);
            error.printStackTrace();
            try { Files.writeString(OUT.resolve("result.txt"), "FAIL: " + error + "\n"); } catch (Exception ignored) { }
            mc.stop();
        }
    }
}
