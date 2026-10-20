package com.gas.mod.world;

import com.gas.mod.GoldenAppleSetMod;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 地形由原生数据包定义。此类只提供带地面安全检查的管理员传送入口。 */
@Mod.EventBusSubscriber(modid = GoldenAppleSetMod.MODID)
public final class GoldenRealm {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("gas", "golden_realm"));

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gas").requires(source -> source.hasPermission(2))
                .then(Commands.literal("gold").executes(context -> travel(context.getSource(), KEY)))
                .then(Commands.literal("overworld").executes(context -> travel(context.getSource(), Level.OVERWORLD))));
    }

    private static int travel(CommandSourceStack source, ResourceKey<Level> key) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        ServerLevel destination = source.getServer().getLevel(key);
        if (destination == null) {
            source.sendFailure(Component.literal("目标维度未加载，请检查数据包并重启游戏。"));
            return 0;
        }
        BlockPos origin = key.equals(KEY) ? BlockPos.ZERO : destination.getSharedSpawnPos();
        for (int radius = 0; radius <= 64; radius += 16) {
            for (int dx = -radius; dx <= radius; dx += 16) {
                for (int dz = -radius; dz <= radius; dz += 16) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int x = origin.getX() + dx, z = origin.getZ() + dz;
                    if (!destination.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) continue;
                    destination.getChunk(x >> 4, z >> 4);
                    int y = destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    var feet = new BlockPos(x, y, z);
                    if (y <= destination.getMinBuildHeight() || y + 2 >= destination.getMaxBuildHeight()
                            || !destination.getBlockState(feet.below()).isSolidRender(destination, feet.below())
                            || !destination.getFluidState(feet.below()).isEmpty()
                            || !destination.getFluidState(feet).isEmpty()
                            || !destination.noCollision(new AABB(x + 0.2, y, z + 0.2, x + 0.8, y + 1.9, z + 0.8))) continue;
                    player.stopRiding();
                    player.teleportTo(destination, x + 0.5, y + 0.05, z + 0.5, player.getYRot(), player.getXRot());
                    player.fallDistance = 0;
                    source.sendSuccess(() -> Component.literal(key.equals(KEY) ? "已进入黄金维度。" : "已返回主世界出生点附近。"), false);
                    return 1;
                }
            }
        }
        source.sendFailure(Component.literal("附近未找到安全落脚点，本次未传送。"));
        return 0;
    }

    private GoldenRealm() { }
}
