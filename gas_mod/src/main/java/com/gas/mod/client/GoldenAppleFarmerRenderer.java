package com.gas.mod.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;

/** 仅金苹果农夫使用；原版群系服装、草帽和动作由父类保持。 */
public final class GoldenAppleFarmerRenderer extends VillagerRenderer {
    public GoldenAppleFarmerRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new GoldenShoulderLayer(this, context.bakeLayer(GoldenShoulderLayer.LAYER)));
    }
}
