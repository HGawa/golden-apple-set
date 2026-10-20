package com.gas.mod.registry;

import java.util.List;
import com.gas.mod.GoldenAppleSetMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Terrain building blocks and their inventory forms. Grass is a stable decorative surface. */
public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GoldenAppleSetMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GoldenAppleSetMod.MODID);
    public static final RegistryObject<Block> GOLDEN_APPLE_GRASS_BLOCK = BLOCKS.register("golden_apple_grass_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).strength(0.6F).sound(SoundType.GRASS).mapColor(MapColor.GOLD)));
    public static final RegistryObject<Block> GOLDEN_APPLE_DIRT = BLOCKS.register("golden_apple_dirt",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).mapColor(MapColor.GOLD)));
    public static final RegistryObject<Block> GOLDEN_APPLE_STONE = BLOCKS.register("golden_apple_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.QUARTZ)));
    public static final RegistryObject<Block> GOLDEN_APPLE_COBBLESTONE = BLOCKS.register("golden_apple_cobblestone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE).mapColor(MapColor.QUARTZ)));
    public static final List<RegistryObject<Block>> TERRAIN = List.of(GOLDEN_APPLE_GRASS_BLOCK, GOLDEN_APPLE_DIRT,
            GOLDEN_APPLE_STONE, GOLDEN_APPLE_COBBLESTONE);
    static {
        TERRAIN.forEach(block -> ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties())));
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); }
    private ModBlocks() { }
}
