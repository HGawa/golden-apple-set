package com.gas.mod.entity;

import com.gas.mod.config.GoldenAppleFarmerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

/** 独立 NPC 类型，复用原版村民寻路、农夫外观和交易界面。 */
public final class GoldenAppleFarmer extends Villager {
    public static final int STOCK_PER_RESTOCK = 64;
    private long nextRestock;

    public GoldenAppleFarmer(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        setVillagerData(getVillagerData());
        setVillagerXp(1);
        setPersistenceRequired();
        setCustomName(Component.translatable("entity.gas.golden_apple_farmer"));
        setCustomNameVisible(true);
    }

    @Override
    public void setVillagerData(VillagerData data) {
        // 就业、出生与读档都不能把商人改成普通职业或清空独有交易。
        super.setVillagerData(data.setProfession(VillagerProfession.FARMER).setLevel(1));
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.gas.golden_apple_farmer");
    }

    @Override
    protected void updateTrades() {
        getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD, GoldenAppleFarmerConfig.EMERALD_COST.get()),
                new ItemStack(Items.GOLDEN_APPLE), STOCK_PER_RESTOCK, 0, 0.0F));
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // 固定供应商不升级，也不追加原版农夫交易。
    }

    @Override
    public boolean canBreed() { return false; }

    @Override
    public boolean showProgressBar() { return false; }

    @Override
    public boolean canRestock() { return false; }

    @Override
    public boolean shouldRestock() { return false; }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long now = level().getGameTime();
        if (nextRestock == 0) nextRestock = now + GoldenAppleFarmerConfig.RESTOCK_TICKS.get();
        if (now >= nextRestock && !isTrading()) {
            getOffers().forEach(MerchantOffer::resetUses);
            nextRestock = now + GoldenAppleFarmerConfig.RESTOCK_TICKS.get();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("GasNextRestock", nextRestock);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nextRestock = tag.getLong("GasNextRestock");
        // 旧存档可能保存过更大的 maxUses。迁移保留已交易次数、价格及补货时间，不能借读档补满库存。
        var trades = getOffers();
        for (int i = 0; i < trades.size(); i++) {
            var offer = trades.get(i);
            if (offer.getResult().is(Items.GOLDEN_APPLE)
                    && (offer.getMaxUses() != STOCK_PER_RESTOCK || offer.getResult().getCount() != 1)) {
                var saved = offer.createTag();
                saved.putInt("maxUses", STOCK_PER_RESTOCK);
                saved.putInt("uses", Math.min(offer.getUses(), STOCK_PER_RESTOCK));
                saved.put("sell", new ItemStack(Items.GOLDEN_APPLE).save(new CompoundTag()));
                trades.set(i, new MerchantOffer(saved));
            }
        }
    }
}
