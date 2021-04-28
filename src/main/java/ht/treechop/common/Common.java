package ht.treechop.common;

import ht.treechop.TreeChopMod;
import ht.treechop.common.capabilities.ChopSettingsCapability;
import ht.treechop.common.capabilities.ChopSettingsProvider;
import ht.treechop.common.config.ConfigHandler;
import ht.treechop.common.event.ChopEvent;
import ht.treechop.common.network.PacketHandler;
import ht.treechop.common.util.ChopResult;
import ht.treechop.common.util.ChopUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static ht.treechop.common.util.ChopUtil.isBlockALog;

public class Common {

    public void preInit() {
        ChopSettingsCapability.register();
        PacketHandler.init();
    }

    @SubscribeEvent
    public void onBreakEvent(BlockEvent.BreakEvent event) {
        EntityPlayer agent = event.getPlayer();
        ItemStack tool = agent.getHeldItemMainhand();
        IBlockState blockState = event.getState();
        BlockPos pos = event.getPos();

        // Reuse some permission logic from PlayerInteractionManager.tryHarvestBlock
        if (
                !isBlockALog(blockState)
                        || !ConfigHandler.enabled
                        || !ChopUtil.canChopWithTool(tool)
                        || !ChopUtil.playerWantsToChop(agent)
                        || event.isCanceled()
        ) {
            return;
        }

        World world = event.getWorld();
        boolean canceled = MinecraftForge.EVENT_BUS.post(new ChopEvent.StartChopEvent(event, world, agent, pos, blockState));
        if (canceled) {
            return;
        }

        ChopResult chopResult = ChopUtil.getChopResult(
                world,
                pos,
                agent,
                ChopUtil.getNumChopsByTool(tool),
                ChopUtil.playerWantsToFell(agent),
                logPos -> isBlockALog(world, logPos)
        );

        if (chopResult != ChopResult.IGNORED) {
            if (chopResult.apply(pos, agent, tool, ConfigHandler.breakLeaves)) {
                event.setCanceled(true);

                if (!agent.isCreative()) {
                    ChopUtil.doItemDamage(tool, world, blockState, pos, agent);
                }
            }

            MinecraftForge.EVENT_BUS.post(new ChopEvent.FinishChopEvent(world, agent, pos, blockState));
        }
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        final ResourceLocation loc = new ResourceLocation(TreeChopMod.MOD_ID + "chop_settings_capability");

        Entity entity = event.getObject();
        if (entity instanceof EntityPlayer) {
            event.addCapability(loc, new ChopSettingsProvider());
        }
    }

}
