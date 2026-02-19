package com.goodbird.mindofthecolony.mc.core.colony.buildings.moduleviews;

import com.ldtteam.blockui.views.BOWindow;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.client.gui.modules.building.WindowMineGuardModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * Miner guard assignment module.
 */
public class MinerGuardAssignModuleView extends AbstractBuildingModuleView
{
    @Override
    public void deserialize(@NotNull final RegistryFriendlyByteBuf buf)
    {

    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public BOWindow getWindow()
    {
        return new WindowMineGuardModule(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/modules/sword.png");
    }
    
    @Override
    public Component getDesc()
    {
        return Component.translatable("com.goodbird.mindofthecolony.mc.coremod.gui.miner.guardassign");
    }
}
