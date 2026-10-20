package com.gas.mod.compat;

import java.util.List;
import com.gas.mod.GoldenAppleSetMod;
import com.gas.mod.entity.GoldenAppleFarmer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional integration using vanilla equipment sync/save and SB's own living-entity render layer. */
@Mod.EventBusSubscriber(modid = GoldenAppleSetMod.MODID)
public final class FarmerBackpackCompat {
    public static final String ASSIGNED = "gas:farmer_backpack_assigned";
    private static final List<ResourceLocation> BACKPACKS = List.of("backpack", "copper_backpack",
            "iron_backpack", "gold_backpack", "diamond_backpack", "netherite_backpack")
            .stream().map(name -> new ResourceLocation("sophisticatedbackpacks", name)).toList();

    public static boolean isAvailable() {
        return ModList.get().isLoaded("sophisticatedbackpacks") && ModList.get().isLoaded("sophisticatedcore");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof GoldenAppleFarmer farmer)
                || !isAvailable() || farmer.getPersistentData().getBoolean(ASSIGNED)) return;
        // Existing equipment, including backpacks assigned by another mod, takes precedence.
        if (!farmer.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            farmer.getPersistentData().putBoolean(ASSIGNED, true);
            return;
        }
        var candidates = BACKPACKS.stream().filter(ForgeRegistries.ITEMS::containsKey).toList();
        if (candidates.isEmpty()) return;
        var item = ForgeRegistries.ITEMS.getValue(candidates.get(farmer.getRandom().nextInt(candidates.size())));
        farmer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(item));
        // A worn visual accessory, not a new source of high-tier backpack loot.
        farmer.setDropChance(EquipmentSlot.CHEST, 0.0F);
        farmer.getPersistentData().putBoolean(ASSIGNED, true);
    }

    private FarmerBackpackCompat() { }
}
