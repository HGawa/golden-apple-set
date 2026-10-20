package com.gas.mod;

import com.gas.mod.registry.ModItems;
import com.gas.mod.registry.ModEntities;
import com.gas.mod.client.GoldenAppleFarmerRenderer;
import com.gas.mod.client.GoldenShoulderLayer;
import net.minecraftforge.client.event.EntityRenderersEvent;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端物品属性注册：拉弓和盾牌格挡。
 *
 * <p><b>根因</b>：原版 {@code ItemProperties} 的静态块里，{@code pull} / {@code pulling} 这两个
 * 物品属性是**只为 {@code Items.BOW} 这一个物品**注册的（已用 javap 核对原版字节码确认）。
 * 我们这把弓是 {@code gas:golden_apple_bow}，所以模型 JSON 里的 {@code overrides}
 * （{@code pulling:1} / {@code pull:0.65} / {@code pull:0.90}）**永远不会命中**——
 * 表现就是「拉弓时弓不换帧、没有拉弦动画」，而贴图、模型、谓词全都躺在磁盘上没用上。
 *
 * <p>本类把这两个谓词按原版口径补注册到我们自己的弓上，公式与原版逐条一致：
 * <ul>
 *   <li>{@code pull} = {@code (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F}</li>
 *   <li>{@code pulling} = 正在使用且用的就是这把弓 ? 1 : 0</li>
 * </ul>
 *
 * <p>只在客户端加载（{@code value = Dist.CLIENT}）：Forge 通过扫描 class 文件读取注解，
 * 服务端不会加载本类，因此其中引用的纯客户端 {@link ItemProperties} 不会在服务端炸。
 */
@Mod.EventBusSubscriber(modid = GoldenAppleSetMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GoldenAppleClientSetup {

    /** 原版使用这些名字（{@code new ResourceLocation("pull")} → {@code minecraft:pull}）。 */
    private static final ResourceLocation PULL = new ResourceLocation("pull");
    private static final ResourceLocation PULLING = new ResourceLocation("pulling");
    private static final ResourceLocation BLOCKING = new ResourceLocation("blocking");

    private GoldenAppleClientSetup() {
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOLDEN_APPLE_FARMER.get(), GoldenAppleFarmerRenderer::new);
    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GoldenShoulderLayer.LAYER, GoldenShoulderLayer::createLayer);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 物品属性必须在模型烘焙之前注册好，所以放在 client setup（而不是懒加载的 initializeClient）
        event.enqueueWork(() -> {
            registerBowPullProperties();
            registerShieldBlockingProperty();
        });
    }

    /** Custom shields need their own property; vanilla only registers Items.SHIELD. */
    private static void registerShieldBlockingProperty() {
        ItemProperties.register(ModItems.GOLDEN_APPLE_SHIELD.get(), BLOCKING,
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    private static void registerBowPullProperties() {
        ItemProperties.register(ModItems.GOLDEN_APPLE_BOW.get(), PULL,
                (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack) {
                        return 0.0F;
                    }
                    return (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
                });

        ItemProperties.register(ModItems.GOLDEN_APPLE_BOW.get(), PULLING,
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}
