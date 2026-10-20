package com.gas.mod;

import com.gas.mod.registry.ModCreativeTabs;
import com.gas.mod.registry.ModItems;
import com.gas.mod.registry.ModEntities;
import com.gas.mod.registry.ModBlocks;
import com.gas.mod.config.GoldenAppleFarmerConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge 入口只负责连接注册模块，装备行为与数值各自维护。 */
@Mod(GoldenAppleSetMod.MODID)
public final class GoldenAppleSetMod {
    public static final String MODID = "gas";

    public GoldenAppleSetMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(bus);
        ModBlocks.register(bus);
        ModCreativeTabs.register(bus);
        ModEntities.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GoldenAppleFarmerConfig.SPEC);
    }
}
