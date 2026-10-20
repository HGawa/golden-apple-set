package com.gas.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/** 原生 Java 实体模型层：左右对称的金肩顶、绿垂片、金边和叶形收尾。 */
public final class GoldenShoulderLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation("gas", "golden_apple_farmer"), "shoulders");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("gas", "textures/entity/golden_apple_farmer/shoulders.png");
    private final ModelPart shoulders;

    public GoldenShoulderLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent, ModelPart root) {
        super(parent);
        shoulders = root.getChild("shoulders");
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var shoulders = mesh.getRoot().addOrReplaceChild("shoulders", CubeListBuilder.create(), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            // UV blocks match gas_asset_build/farmer_shoulders.py; dimensions in model pixels.
            float capX = side > 0 ? 3.5F : -8.5F;
            float edgeX = side > 0 ? 8.0F : -9.0F;
            float tipX = side > 0 ? 4.5F : -7.5F;
            var cubes = CubeListBuilder.create()
                    .texOffs(0, 0).addBox(capX, -0.5F, -4.0F, 5, 2, 8)
                    .texOffs(0, 12).addBox(capX, 1.5F, -4.0F, 5, 4, 1)
                    .texOffs(28, 0).addBox(edgeX, 1.5F, -4.0F, 1, 5, 8)
                    .texOffs(0, 20).addBox(capX, 1.5F, 3.5F, 5, 8, 1)
                    .texOffs(16, 20).addBox(tipX, 9.5F, 3.5F, 3, 2, 1);
            shoulders.addOrReplaceChild(side > 0 ? "left" : "right", cubes, PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, Villager villager,
                       float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch) {
        if (villager.isInvisible()) return;
        // Follow the body, not the head or folded-arm bone. Parent renderer handles size and pose.
        shoulders.copyFrom(getParentModel().root().getChild("body"));
        shoulders.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                LivingEntityRenderer.getOverlayCoords(villager, 0), 1, 1, 1, 1);
    }
}
