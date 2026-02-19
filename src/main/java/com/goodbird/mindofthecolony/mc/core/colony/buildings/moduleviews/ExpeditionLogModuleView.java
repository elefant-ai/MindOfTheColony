package com.goodbird.mindofthecolony.mc.core.colony.buildings.moduleviews;

import com.ldtteam.blockui.views.BOWindow;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.client.gui.modules.building.ExpeditionLogModuleWindow;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.modules.expedition.ExpeditionLog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * Building module view to display an expedition log
 */
public class ExpeditionLogModuleView extends AbstractBuildingModuleView
{
    private boolean updated;
    private boolean unlocked;
    private ExpeditionLog log = new ExpeditionLog();

    @Override
    public void deserialize(@NotNull final RegistryFriendlyByteBuf buf)
    {
        this.unlocked = buf.readBoolean();
        if (this.unlocked)
        {
            this.log.deserialize(buf);
        }
        this.updated = true;
    }

    public boolean checkAndResetUpdated()
    {
        final boolean wasUpdated = this.updated;
        this.updated = false;
        return wasUpdated;
    }

    public ExpeditionLog getLog()
    {
        return this.log;
    }

    @Override
    public boolean isPageVisible()
    {
        return this.unlocked;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public BOWindow getWindow()
    {
        return new ExpeditionLogModuleWindow(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/modules/sword.png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable("com.goodbird.mindofthecolony.mc.gui.workerhuts.expeditionlog");
    }
}
