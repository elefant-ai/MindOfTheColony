package com.goodbird.mindofthecolony.mc.core.client.gui.citizen;

import com.goodbird.mindofthecolony.client.ChatWindowCitizen;
import com.goodbird.mindofthecolony.network.ChatMenuStateMessage;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.*;
import com.ldtteam.blockui.views.View;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenDataView;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.Skill;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.network.messages.server.colony.citizen.AdjustSkillCitizenMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.util.constant.WindowConstants.*;

/**
 * BOWindow for the citizen.
 */
public class MainWindowCitizen extends AbstractWindowCitizen
{
    private static final String CHAT_TAB_ID = "mindofthecolony_chatTab";
    private static final String CHAT_ICON_ID = "mindofthecolony_chatIcon";

    /**
     * Tick function for updating every second.
     */
    private int tick = 0;

    /**
     * Constructor to initiate the citizen windows.
     *
     * @param citizen citizen to bind the window to.
     */
    public MainWindowCitizen(final ICitizenDataView citizen)
    {
        super(citizen, new ResourceLocation(Constants.MOD_ID, "gui/citizen/main.xml"));

        final Image statusIcon = findPaneOfTypeByID(STATUS_ICON, Image.class);
        if (citizen.getVisibleStatus() == null)
        {
            statusIcon.hide();
        }
        else
        {
            statusIcon.show();
            statusIcon.setImage(citizen.getVisibleStatus().getIcon(), false);
            PaneBuilders.tooltipBuilder()
                .append(Component.translatable(citizen.getVisibleStatus().getTranslationKey()))
                .hoverPane(statusIcon)
                .build();
        }
    }

    public ICitizenDataView getCitizen()
    {
        return citizen;
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (tick++ == 20)
        {
            tick = 0;
            CitizenWindowUtils.createSkillContent(citizen, this);
        }
    }

    /**
     * Called when the gui is opened by an player.
     */
    @Override
    public void onOpened()
    {
        super.onOpened();
        findPaneOfTypeByID(WINDOW_ID_NAME, Text.class).setText(Component.literal(citizen.getName()));

        CitizenWindowUtils.createHealthBar(citizen, findPaneOfTypeByID(WINDOW_ID_HEALTHBAR, View.class));
        CitizenWindowUtils.createSaturationBar(citizen, this);
        CitizenWindowUtils.createHappinessBar(citizen, this);
        CitizenWindowUtils.createSkillContent(citizen, this);

        //Tool of class:§rwith minimal level:§rWood or Gold§r and§rwith maximal level:§rWood or Gold§r

        if (citizen.isFemale())
        {
            findPaneOfTypeByID(WINDOW_ID_GENDER, Image.class).setImage(ResourceLocation.parse(FEMALE_SOURCE), false);
        }

        // MOTC: Add chat tab button and freeze citizen
        ICitizenDataView citizenView = getCitizen();
        PacketDistributor.sendToServer(new ChatMenuStateMessage(
            citizenView.getColonyId(),
            citizenView.getId(),
            true
        ));

        View window = ((Pane)this).getWindow();
        if (window != null) {
            // Add chat tab button in the same style as other nav tabs
            ButtonImage chatTab = new ButtonImage();
            chatTab.setID(CHAT_TAB_ID);
            chatTab.setImage(ResourceLocation.parse("minecolonies:textures/gui/modules/tab_left_side2.png"));
            chatTab.setSize(32, 26);
            chatTab.setPosition(0, 170);
            window.addChild(chatTab);

            // Chat icon on top of tab
            ButtonImage chatIcon = new ButtonImage();
            chatIcon.setID(CHAT_ICON_ID);
            chatIcon.setImage(ResourceLocation.parse("minecolonies:textures/gui/modules/requests.png"));
            chatIcon.setSize(20, 20);
            chatIcon.setPosition(5, 173);
            window.addChild(chatIcon);

            // Add tooltip
            PaneBuilders.tooltipBuilder()
                .hoverPane(chatIcon)
                .build()
                .setText(Component.literal("Chat with " + getCitizen().getName()));
        }
    }

    /**
     * Called when a button in the citizen has been clicked.
     *
     * @param button the clicked button.
     */
    @Override
    public void onButtonClicked(@NotNull final Button button)
    {
        // MOTC: Handle chat tab button click
        if (CHAT_TAB_ID.equals(button.getID()) || CHAT_ICON_ID.equals(button.getID())) {
            new ChatWindowCitizen(getCitizen()).open();
            return;
        }

        super.onButtonClicked(button);
        if (button.getID().contains(PLUS_PREFIX))
        {
            final String label = button.getID().replace(PLUS_PREFIX, "");
            final Skill skill = Skill.valueOf(StringUtils.capitalize(label));

            new AdjustSkillCitizenMessage(colony, citizen, 1, skill).sendToServer();
        }
        else if (button.getID().contains(MINUS_PREFIX))
        {
            final String label = button.getID().replace(MINUS_PREFIX, "");
            final Skill skill = Skill.valueOf(StringUtils.capitalize(label));

            new AdjustSkillCitizenMessage(colony, citizen, -1, skill).sendToServer();
        }
    }
}
