package com.gas.mod.world;

import java.util.ArrayList;
import com.gas.mod.GoldenAppleSetMod;
import com.gas.mod.config.GoldenAppleFarmerConfig;
import com.gas.mod.entity.GoldenAppleFarmer;
import com.gas.mod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 对已加载的有人居住村庄进行低频检查，兼容已有世界，无需修改结构模板。 */
@Mod.EventBusSubscriber(modid = GoldenAppleSetMod.MODID)
public final class GoldenAppleFarmerSpawns {
    public static final int SEPARATION = 96;

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !GoldenAppleFarmerConfig.ENABLED.get() || !level.isDay()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || level.getGameTime() % GoldenAppleFarmerConfig.SPAWN_INTERVAL.get() != 0) return;
        var checked = new ArrayList<BlockPos>();
        for (var player : level.players()) {
            if (player.isSpectator()) continue;
            var residents = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64),
                    villager -> !(villager instanceof GoldenAppleFarmer) && villager.isAlive()
                            && !villager.isBaby() && level.isVillage(villager.blockPosition()));
            for (var resident : residents) {
                var center = resident.blockPosition();
                if (checked.stream().anyMatch(pos -> pos.closerThan(center, SEPARATION))) continue;
                checked.add(center);
                if (level.random.nextDouble() < GoldenAppleFarmerConfig.SPAWN_CHANCE.get()) {
                    trySpawn(level, center);
                }
            }
        }
    }

    /** 同时检查村庄 POI、重复 NPC 与安全地面；仅访问已加载区块。 */
    public static boolean trySpawn(ServerLevel level, BlockPos center) {
        if (!level.hasChunkAt(center) || !level.isVillage(center)
                || !level.getEntitiesOfClass(GoldenAppleFarmer.class, new AABB(center).inflate(SEPARATION),
                        GoldenAppleFarmer::isAlive).isEmpty()) return false;
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos column = center.offset(level.random.nextInt(25) - 12, 0, level.random.nextInt(25) - 12);
            if (!level.hasChunkAt(column)) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (!level.getWorldBorder().isWithinBounds(pos) || !level.isVillage(pos)
                    || !level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), ModEntities.GOLDEN_APPLE_FARMER.get())
                    || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) continue;
            var farmer = ModEntities.GOLDEN_APPLE_FARMER.get().create(level);
            if (farmer == null) return false;
            farmer.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            if (!level.noCollision(farmer) || !level.isUnobstructed(farmer)) continue;
            farmer.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            farmer.setAge(0);
            return level.addFreshEntity(farmer);
        }
        return false;
    }

    private GoldenAppleFarmerSpawns() { }
}
