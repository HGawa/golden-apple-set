package com.gas.mod;

import com.gas.mod.balance.GoldenAppleStats;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 金苹果盔甲材质 —— 4 件盔甲共用（PRD §6.1）。
 *
 * <p>数值：总防御 20（头 3 / 胸 8 / 腿 6 / 靴 3）、每件韧度 2、击退抗性 0.05、
 * 耐久（头 220 / 胸 320 / 腿 300 / 靴 260）、附魔值 22、修复材料金苹果。
 * 防御与下界合金相同，韧性与钻石相同；四件套效果见 equipment/GoldenAppleSetBonus。
 *
 * <p>层贴图解析：Forge 1.20.1 的 {@code HumanoidArmorLayer#getArmorResource} 用
 * {@code "%s:textures/models/armor/%s_layer_%d%s.png"} 拼路径，命名空间取自
 * {@link #getName()} 里 ':' 之前的部分。因此 {@link #NAME} 必须是
 * {@code "gas:golden_apple"}，最终贴图 = {@code assets/gas/textures/models/armor/golden_apple_layer_1.png}
 * 与 {@code ..._layer_2.png}（资产侧由 ZCode 提供）。
 *
 * <p>注：Forge 1.20.1 并不存在 {@code net.minecraftforge.common.IForgeArmorMaterial}
 * （已在 forge-1.20.1-47.3.0-client.jar 中确认），直接实现原版 {@link ArmorMaterial} 接口即可。
 */
public final class GoldenAppleArmorMaterial implements ArmorMaterial {

    public static final GoldenAppleArmorMaterial INSTANCE = new GoldenAppleArmorMaterial();

    /** 带命名空间，供 HumanoidArmorLayer 解析到 assets/gas/... 下的层贴图。 */
    public static final String NAME = "gas:golden_apple";

    private GoldenAppleArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> GoldenAppleStats.HELMET_DURABILITY;
            case CHESTPLATE -> GoldenAppleStats.CHESTPLATE_DURABILITY;
            case LEGGINGS -> GoldenAppleStats.LEGGINGS_DURABILITY;
            case BOOTS -> GoldenAppleStats.BOOTS_DURABILITY;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> GoldenAppleStats.HELMET_DEFENSE;
            case CHESTPLATE -> GoldenAppleStats.CHESTPLATE_DEFENSE;
            case LEGGINGS -> GoldenAppleStats.LEGGINGS_DEFENSE;
            case BOOTS -> GoldenAppleStats.BOOTS_DEFENSE;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return GoldenAppleStats.ENCHANTMENT_VALUE;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_GOLD;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.GOLDEN_APPLE);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public float getToughness() {
        return GoldenAppleStats.ARMOR_TOUGHNESS;
    }

    @Override
    public float getKnockbackResistance() {
        return GoldenAppleStats.KNOCKBACK_RESISTANCE;
    }
}
