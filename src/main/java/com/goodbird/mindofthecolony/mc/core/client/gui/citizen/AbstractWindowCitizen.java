package com.goodbird.mindofthecolony.mc.core.client.gui.citizen;

import com.goodbird.mindofthecolony.client.TabbedDebugWindow;
import com.goodbird.mindofthecolony.network.ChatMenuStateMessage;
import com.ldtteam.blockui.PaneBuilders;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenDataView;
import com.goodbird.mindofthecolony.mc.api.colony.IColonyView;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.views.IBuildingView;
import com.goodbird.mindofthecolony.mc.core.client.gui.AbstractWindowSkeleton;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.views.AbstractBuildingView;
import com.goodbird.mindofthecolony.mc.core.debug.DebugPlayerManager;
import com.goodbird.mindofthecolony.mc.core.debug.gui.DebugWindowCitizen;
import com.goodbird.mindofthecolony.mc.core.network.messages.server.colony.OpenInventoryMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * BOWindow for the citizen.
 */
public abstract class AbstractWindowCitizen extends AbstractWindowSkeleton
{
    protected final IColonyView colony;

    /**
     * The citizenData.View object.
     */
    protected final ICitizenDataView citizen;

    /**
     * Constructor to initiate the citizen windows.
     *
     * @param citizen citizen to bind the window to.
     * @param ui the xml res loc.
     */
    public AbstractWindowCitizen(final ICitizenDataView citizen, final ResourceLocation ui)
    {
        super(ui);
        this.colony = citizen.getColony();
        this.citizen = citizen;

        registerButton("mainTab", () -> new MainWindowCitizen(citizen).open());
        registerButton("mainIcon", () -> new MainWindowCitizen(citizen).open());
        PaneBuilders.tooltipBuilder().hoverPane(findPaneByID("mainIcon")).build().setText(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.citizen.main"));

        registerButton("requestTab", () -> new RequestWindowCitizen(citizen).open());
        registerButton("requestIcon", () -> new RequestWindowCitizen(citizen).open());
        PaneBuilders.tooltipBuilder().hoverPane(findPaneByID("requestIcon")).build().setText(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.citizen.requests"));

        registerButton("inventoryTab", () -> new OpenInventoryMessage(citizen.getColony(), citizen.getName(), citizen.getEntityId()).sendToServer());
        registerButton("inventoryIcon", () -> new OpenInventoryMessage(citizen.getColony(), citizen.getName(), citizen.getEntityId()).sendToServer());
        PaneBuilders.tooltipBuilder().hoverPane(findPaneByID("inventoryIcon")).build().setText(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.citizen.inventory"));

        registerButton("happinessTab", () -> new HappinessWindowCitizen(citizen).open());
        registerButton("happinessIcon", () -> new HappinessWindowCitizen(citizen).open());
        PaneBuilders.tooltipBuilder().hoverPane(findPaneByID("happinessIcon")).build().setText(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.citizen.happiness"));

        registerButton("familyTab", () -> new FamilyWindowCitizen(citizen).open());
        registerButton("familyIcon", () -> new FamilyWindowCitizen(citizen).open());
        PaneBuilders.tooltipBuilder().hoverPane(findPaneByID("familyIcon")).build().setText(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.citizen.family"));

        if (DebugPlayerManager.hasDebugEnabled(mc.player))
        {
            findPaneByID("debugTab").setVisible(true);
            findPaneByID("debugIcon").setVisible(true);
            // MOTC: Redirect debug buttons to TabbedDebugWindow
            registerButton("debugTab", () -> new TabbedDebugWindow(citizen).open());
            registerButton("debugIcon", () -> new TabbedDebugWindow(citizen).open());
            PaneBuilders.singleLineTooltip(Component.translatable("com.goodbird.mindofthecolony.mc.coremod.debug.gui.tabicon"), findPaneByID("debugIcon"));
        }

        final IBuildingView building = citizen.getColony().getClientBuildingManager().getBuilding(citizen.getWorkBuilding());

        if (building instanceof AbstractBuildingView && building.getBuildingType() != ModBuildings.library.get())
        {
            findPaneByID("jobTab").setVisible(true);
            findPaneByID("jobIcon").setVisible(true);

            registerButton("jobTab", () -> new JobWindowCitizen(citizen).open());
            registerButton("jobIcon", () -> new JobWindowCitizen(citizen).open());
            PaneBuilders.tooltipBuilder().hoverPane(findPaneByID("jobIcon")).build().setText(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.citizen.job"));
        }
        else
        {
            findPaneByID("jobTab").setVisible(false);
            findPaneByID("jobIcon").setVisible(false);
        }
    }

    // MOTC: Unfreeze the citizen when any citizen window is closed
    @Override
    public void onClosed() {
        super.onClosed();
        PacketDistributor.sendToServer(new ChatMenuStateMessage(
            citizen.getColonyId(),
            citizen.getId(),
            false
        ));
    }
}
