package com.gas.mod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 金苹果盾的自定义渲染器（客户端专用）。
 *
 * <p><b>为什么需要它</b>：1.20.1 的原版 {@link BlockEntityWithoutLevelRenderer} 在渲染
 * {@code ShieldItem} 时，贴图取自 {@code ModelBakery} 里<b>硬编码</b>的
 * {@code ModelBakery.NO_PATTERN_SHIELD / SHIELD_BASE}（即原版 {@code textures/entity/shield_base_nopattern.png}），
 * 也就是说：盾的 {@code builtin/entity} 路径会**忽略**模型 JSON 里的 {@code shield} 贴图槽。
 * 后果是自定义盾能格挡、但外观永远是原版盾——T1~T8 静态验收抓不到，只有真客户端才看得出来。
 *
 * <p><b>本类做的事</b>：用原版 {@link ShieldModel} + 我们自己的实体贴图重画一遍，
 * 姿态与 vanilla 逐条对齐（{@code pushPose()} → {@code scale(1, -1, -1)} → handle → plate → {@code popPose()}），
 * 只是把贴图源从「图集 Material」换成「独立贴图 + {@link RenderType#entitySolid}」。
 *
 * <p>物品的持握/展示姿态仍由模型 JSON（{@code golden_apple_shield.json} 继承的 {@code display} 块）提供，
 * 由 {@code ItemRenderer} 在调用本渲染器<b>之前</b>施加，所以这里不需要再做位移旋转。
 *
 * <p>只在客户端实例化：服务端不会加载本类（见 {@link GoldenAppleShieldItem#initializeClient}）。
 */
public class GoldenAppleShieldRenderer extends BlockEntityWithoutLevelRenderer {

    /** 64×64 盾实体贴图（资产侧产物，路径与模型 JSON 的 {@code shield} 槽一致）。 */
    public static final ResourceLocation SHIELD_TEXTURE =
            new ResourceLocation(GoldenAppleSetMod.MODID, "textures/entity/shield/golden_apple_shield.png");

    private static final RenderType RENDER_TYPE = RenderType.entitySolid(SHIELD_TEXTURE);

    private static final float R = 1.0F;
    private static final float G = 1.0F;
    private static final float B = 1.0F;
    private static final float A = 1.0F;

    private final ShieldModel model;

    public GoldenAppleShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
        this.model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // 与 vanilla 一致：盾模型是"倒挂"烘出来的
        poseStack.scale(1.0F, -1.0F, -1.0F);

        // 支持附魔光效（忠诚/耐久等）；图案层不做（本盾不接旗帜图案）
        VertexConsumer consumer =
                ItemRenderer.getFoilBufferDirect(bufferSource, RENDER_TYPE, true, stack.hasFoil());

        this.model.handle().render(poseStack, consumer, packedLight, packedOverlay, R, G, B, A);
        this.model.plate().render(poseStack, consumer, packedLight, packedOverlay, R, G, B, A);

        poseStack.popPose();
    }
}
