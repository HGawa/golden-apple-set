package com.gas.mod;

import com.gas.mod.balance.GoldenAppleStats;

import java.util.function.Consumer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * 金苹果盾 —— 原版盾牌行为（格挡 / 副手 / 3D 模型贴图槽）+ 套装统一属性。
 *
 * <p>耐久 512 由注册时的 {@code props().durability(512)} 给出；
 * 附魔值 22 与修复材料金苹果在 PRD §3 里对 11 件统一要求，故此处覆盖原版默认值。
 *
 * <p>客户端外观：接过 {@link GoldenAppleShieldRenderer}——因为 1.20.1 的原版 BEWLR 渲染盾时
 * 用的是 {@code ModelBakery} 里硬编码的原版贴图，模型 JSON 的 {@code shield} 槽会被忽略。
 *
 * <p>PRD §4.9 的三个被动（果壳 / 弹梢 / 金壳）属于机制层，本次未实现。
 */
public class GoldenAppleShieldItem extends ShieldItem {

    public GoldenAppleShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEnchantmentValue() {
        return GoldenAppleStats.ENCHANTMENT_VALUE;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.GOLDEN_APPLE) || super.isValidRepairItem(toRepair, repair);
    }

    /**
     * 装上自定义渲染器。本方法<b>只在客户端</b>被 Forge 调用，所以服务端不会去加载
     * {@link GoldenAppleShieldRenderer}（它继承的 {@link BlockEntityWithoutLevelRenderer}
     * 是纯客户端类）；渲染器实例懒创建，避免在 {@code Minecraft} 尚未就绪时构造。
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private GoldenAppleShieldRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GoldenAppleShieldRenderer();
                }
                return this.renderer;
            }
        });
    }
}
