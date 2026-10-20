package com.gas.mod.registry;

import java.util.List;
import com.gas.mod.GoldenAppleSetMod;
import com.gas.mod.GoldenAppleArmorItem;
import com.gas.mod.GoldenAppleToolTier;
import com.gas.mod.GoldenAppleSwordItem;
import com.gas.mod.GoldenAppleBowItem;
import com.gas.mod.GoldenAppleShieldItem;
import com.gas.mod.GoldenApplePickaxeItem;
import com.gas.mod.GoldenAppleAxeItem;
import com.gas.mod.GoldenAppleShovelItem;
import com.gas.mod.GoldenAppleHoeItem;
import com.gas.mod.balance.GoldenAppleStats;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 物品注册与系列展示顺序；行为由各 Item 类实现。 */
public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GoldenAppleSetMod.MODID);

    /** 统一属性：史诗稀有度 + 不可堆叠（PRD §3「统一属性」表）。 */
    private static Item.Properties props() {
        return new Item.Properties().rarity(Rarity.EPIC).stacksTo(1);
    }

    // 武器与防具。注册 ID 属于存档契约，不随类名或目录调整而改变。

    public static final RegistryObject<Item> GOLDEN_APPLE_SWORD =
            ITEMS.register("golden_apple_sword", GoldenAppleSwordItem::new);

    public static final RegistryObject<Item> GOLDEN_APPLE_HELMET =
            ITEMS.register("golden_apple_helmet",
                    () -> new GoldenAppleArmorItem(ArmorItem.Type.HELMET, props()));

    public static final RegistryObject<Item> GOLDEN_APPLE_CHESTPLATE =
            ITEMS.register("golden_apple_chestplate",
                    () -> new GoldenAppleArmorItem(ArmorItem.Type.CHESTPLATE, props()));

    public static final RegistryObject<Item> GOLDEN_APPLE_LEGGINGS =
            ITEMS.register("golden_apple_leggings",
                    () -> new GoldenAppleArmorItem(ArmorItem.Type.LEGGINGS, props()));

    public static final RegistryObject<Item> GOLDEN_APPLE_BOOTS =
            ITEMS.register("golden_apple_boots",
                    () -> new GoldenAppleArmorItem(ArmorItem.Type.BOOTS, props()));

    public static final RegistryObject<Item> GOLDEN_APPLE_SHIELD =
            ITEMS.register("golden_apple_shield",
                    () -> new GoldenAppleShieldItem(props().durability(GoldenAppleStats.SHIELD_DURABILITY)));

    public static final RegistryObject<Item> GOLDEN_APPLE_BOW =
            ITEMS.register("golden_apple_bow",
                    () -> new GoldenAppleBowItem(props().durability(GoldenAppleStats.BOW_DURABILITY)));

    // 工具。
    // 攻击伤害口径（PRD §6.2，物品栏显示值 = 基础伤害 + Tier 加成 5.0）：
    //   镐 1 + 5 = 6 / 斧 5 + 5 = 10 / 锹 1.5 + 5 = 6.5 / 锄 -4 + 5 = 1
    // 攻击速度沿用原版修正值：镐 -2.8 / 斧 -3.0 / 锹 -3.0 / 锄 0.0。

    // 注：原版 PickaxeItem / AxeItem / HoeItem 的构造器是 protected（锹是 public），
    // 外部包不能直接 new，故 4 件工具统一走各自的金苹果子类（见 GoldenApple*Item.java）。
    public static final RegistryObject<Item> GOLDEN_APPLE_PICKAXE =
            ITEMS.register("golden_apple_pickaxe",
                    () -> new GoldenApplePickaxeItem(GoldenAppleToolTier.INSTANCE, 1, -2.8F, props()));

    public static final RegistryObject<Item> GOLDEN_APPLE_AXE =
            ITEMS.register("golden_apple_axe",
                    () -> new GoldenAppleAxeItem(GoldenAppleToolTier.INSTANCE, 5.0F, -3.0F, props()));

    public static final RegistryObject<Item> GOLDEN_APPLE_SHOVEL =
            ITEMS.register("golden_apple_shovel",
                    () -> new GoldenAppleShovelItem(GoldenAppleToolTier.INSTANCE, 1.5F, -3.0F, props()));

    public static final RegistryObject<Item> GOLDEN_APPLE_HOE =
            ITEMS.register("golden_apple_hoe",
                    () -> new GoldenAppleHoeItem(GoldenAppleToolTier.INSTANCE, -4, 0.0F, props()));

    public static final RegistryObject<Item> GOLDEN_APPLE_FARMER_SPAWN_EGG =
            ITEMS.register("golden_apple_farmer_spawn_egg", () -> new ForgeSpawnEggItem(
                    ModEntities.GOLDEN_APPLE_FARMER, 0xF6C945, 0x4E9B3C, new Item.Properties()));

    /** 创造页展示顺序：装备，然后是 NPC 刷怪蛋。 */
    public static final List<RegistryObject<Item>> GOLDEN_APPLE_SET = List.of(
            GOLDEN_APPLE_SWORD, GOLDEN_APPLE_PICKAXE, GOLDEN_APPLE_AXE,
            GOLDEN_APPLE_SHOVEL, GOLDEN_APPLE_HOE, GOLDEN_APPLE_BOW,
            GOLDEN_APPLE_SHIELD, GOLDEN_APPLE_HELMET, GOLDEN_APPLE_CHESTPLATE,
            GOLDEN_APPLE_LEGGINGS, GOLDEN_APPLE_BOOTS, GOLDEN_APPLE_FARMER_SPAWN_EGG);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() { }
}
