package com.gas.mod.registry;

import com.gas.mod.GoldenAppleSetMod;
import com.gas.mod.entity.GoldenAppleFarmer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GoldenAppleSetMod.MODID);
    public static final RegistryObject<EntityType<GoldenAppleFarmer>> GOLDEN_APPLE_FARMER =
            ENTITIES.register("golden_apple_farmer", () -> EntityType.Builder
                    .of(GoldenAppleFarmer::new, MobCategory.MISC).sized(0.6F, 1.95F)
                    .clientTrackingRange(10).build("gas:golden_apple_farmer"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(GOLDEN_APPLE_FARMER.get(), Villager.createAttributes().build());
    }

    private ModEntities() { }
}
