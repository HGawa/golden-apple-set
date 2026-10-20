package com.gas.mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import com.gas.mod.registry.ModBlocks;
import com.gas.mod.world.GoldenRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "gas", value = Dist.CLIENT)
public final class BlockSmoke {
    private static final Path OUT=Path.of("../../art/realm_blocks/runtime").toAbsolutePath().normalize();
    private static int stage,ticks; private static long started;
    private static CompletableFuture<?> pending;
    private static BlockPos center;
    private static void require(boolean ok,String msg) { if(!ok)throw new AssertionError(msg); }
    private static void server(Minecraft mc,Runnable r) { pending=mc.getSingleplayerServer().submit(r); }
    private static void capture(Minecraft mc,String name)throws Exception {
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(OUT.resolve(name+".png")); }
    }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if(!Boolean.getBoolean("gas.blockSmoke")||e.phase!=TickEvent.Phase.END||stage==99)return;
        var mc=Minecraft.getInstance();
        try {
            if(started==0){started=System.currentTimeMillis();Files.createDirectories(OUT);Files.writeString(OUT.resolve("result.txt"),"RUNNING\n");}
            require(System.currentTimeMillis()-started<240000,"timeout stage "+stage);
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(stage==0){
                if(mc.screen==null||mc.getOverlay()!=null)return;
                mc.options.renderDistance().set(6);mc.options.simulationDistance().set(5);mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("gas-blocks-"+System.currentTimeMillis(),
                    new LevelSettings("Realm blocks QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                    new WorldOptions(12345L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                stage=1;return;
            }
            if(mc.player==null||mc.level==null||++ticks<60)return;ticks=0;
            if(stage==1){
                mc.setScreen(null);
                server(mc,()->{
                    var s=mc.getSingleplayerServer();var p=s.overworld().players().get(0);
                    s.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(2),"gas gold");
                    var l=s.getLevel(GoldenRealm.KEY);require(p.level()==l,"dimension travel failed");center=p.blockPosition();
                    l.setDayTime(6000);l.setWeatherParameters(6000,0,false,false);
                    int grass=0,dirt=0,stone=0,min=320,max=-64,exposedStone=0;
                    for(int x=-24;x<=24;x+=4)for(int z=-24;z<=24;z+=4){
                        l.getChunk(x >> 4,z >> 4);
                        int h=l.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);min=Math.min(min,h);max=Math.max(max,h);
                        var surface=l.getBlockState(new BlockPos(x,h-1,z));
                        // Vanilla preliminary-surface rules leave deep eroded/cave openings as exposed rock.
                        require(surface.is(ModBlocks.GOLDEN_APPLE_GRASS_BLOCK.get()) || surface.is(ModBlocks.GOLDEN_APPLE_STONE.get()),
                                "unexpected surface at "+x+","+(h-1)+","+z+": "+surface);
                        if(surface.is(ModBlocks.GOLDEN_APPLE_STONE.get()))exposedStone++;
                        for(int y=-64;y<h;y++){
                            var state=l.getBlockState(new BlockPos(x,y,z));
                            if(state.is(ModBlocks.GOLDEN_APPLE_GRASS_BLOCK.get()))grass++;
                            else if(state.is(ModBlocks.GOLDEN_APPLE_DIRT.get()))dirt++;
                            else if(state.is(ModBlocks.GOLDEN_APPLE_STONE.get()))stone++;
                            else require(state.isAir(),"unexpected terrain block "+state);
                        }
                    }
                    require(grass>80&&dirt>0&&stone>dirt&&max>min,"missing layers or natural height variation");
                    require(l.getBiome(center).value().getSkyColor()==0xC6B0FF,"sky changed");
                    var silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(Enchantments.SILK_TOUCH,1);
                    var normal=new ItemStack(Items.DIAMOND_PICKAXE);
                    for(int i=0;i<4;i++){
                        var b=ModBlocks.TERRAIN.get(i).get();var state=b.defaultBlockState();var pos=center.offset(i*2-3,18,5);
                        l.setBlockAndUpdate(pos.below(),Blocks.BLACK_CONCRETE.defaultBlockState());l.setBlockAndUpdate(pos,state);
                        var drops=Block.getDrops(state,l,pos,null,p,normal);
                        var expected=ModBlocks.TERRAIN.get(i==0?1:i==2?3:i).get().asItem();
                        require(drops.size()==1&&drops.get(0).is(expected),"normal drop mismatch "+b);
                        var silkDrops=Block.getDrops(state,l,pos,null,p,silk);
                        require(silkDrops.size()==1&&silkDrops.get(0).is(b.asItem()),"silk drop mismatch "+b);
                        require(state.is(i<2?BlockTags.MINEABLE_WITH_SHOVEL:BlockTags.MINEABLE_WITH_PICKAXE),"wrong mining tag");
                        if(i>=2)require(!new ItemStack(Items.STICK).isCorrectToolForDrops(state)&&normal.isCorrectToolForDrops(state),"stone tool requirement");
                        p.getInventory().setItem(i,new ItemStack(b));
                    }
                    p.getAbilities().flying=true;p.onUpdateAbilities();
                    p.teleportTo(l,center.getX()+0.5,center.getY()+20,center.getZ()-2.5,0,17);
                    System.out.println("GAS_BLOCK_SMOKE PASS terrain grass="+grass+" dirt="+dirt+" stone="+stone+" exposedRockColumns="+exposedStone+" height="+min+".."+max+"; drops/silk/tags/placement/lavender sky");
                });stage=2;
            }else if(stage==2){
                require(mc.level.dimension().equals(GoldenRealm.KEY),"client dimension not synced");
                for(var entry:ModBlocks.TERRAIN){
                    var stack=new ItemStack(entry.get());
                    require(mc.getItemRenderer().getItemModelShaper().getItemModel(stack)!=mc.getModelManager().getMissingModel(),"missing item model");
                    require(mc.getBlockRenderer().getBlockModel(entry.get().defaultBlockState())!=mc.getModelManager().getMissingModel(),"missing block model");
                }
                capture(mc,"01-four-blocks");
                server(mc,()->{var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().get(0);
                    p.teleportTo(s.getLevel(GoldenRealm.KEY),center.getX()+0.5,center.getY()+36,center.getZ()+0.5,-40,35);
                });stage=3;
            }else if(stage==3){capture(mc,"02-new-terrain");RenderSmoke.openCreativeTab(mc);stage=4;
            }else{
                capture(mc,"03-creative-tab");
                Files.writeString(OUT.resolve("result.txt"),"PASS: generated grass/dirt/stone layers, natural terrain and lavender sky; four placed block/item models; normal and silk drops; shovel/pickaxe tags and tool requirement; 16-item creative tab; screenshots.\n");
                System.out.println("GAS_BLOCK_SMOKE ALL PASS");stage=99;mc.stop();
            }
        }catch(Throwable ex){ex.printStackTrace();stage=99;try{Files.writeString(OUT.resolve("result.txt"),"FAIL "+ex);}catch(Exception ignored){}mc.stop();}
    }
}
