package com.gas.mod.registry;

import com.gas.mod.GoldenAppleSetMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** 模组的创造模式入口。只使用公共注册接口，可在专用服务器安全加载。 */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GoldenAppleSetMod.MODID);

    public static final RegistryObject<CreativeModeTab> GOLDEN_APPLE_SET =
            TABS.register("golden_apple_set", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gas.golden_apple_set"))
                    .icon(() -> new ItemStack(ModItems.GOLDEN_APPLE_SWORD.get()))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((parameters, output) -> {
                        ModItems.GOLDEN_APPLE_SET.forEach(item -> output.accept(item.get()));
                        ModBlocks.TERRAIN.forEach(block -> output.accept(block.get()));
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() { }
}
