package com.goodbird.mindofthecolony.mc.core.colony.interactionhandling;

import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.Box;
import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import com.goodbird.mindofthecolony.mc.api.colony.*;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.interactionhandling.IChatPriority;
import com.goodbird.mindofthecolony.mc.api.colony.interactionhandling.IInteractionResponseHandler;
import com.goodbird.mindofthecolony.mc.api.colony.interactionhandling.ModInteractionResponseHandlers;
import com.goodbird.mindofthecolony.mc.api.eventbus.events.colony.citizens.CitizenAddedModEvent;
import com.goodbird.mindofthecolony.mc.api.util.InventoryUtils;
import com.goodbird.mindofthecolony.mc.api.util.MessageUtils;
import com.goodbird.mindofthecolony.mc.api.util.StatsUtil;
import com.goodbird.mindofthecolony.mc.api.util.Tuple;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import java.util.Collections;
import java.util.List;

import static com.goodbird.mindofthecolony.mc.api.util.constant.StatisticsConstants.VISITORS_ABSCONDED;
import static com.goodbird.mindofthecolony.mc.api.util.constant.StatisticsConstants.VISITORS_RECRUITED;
import static com.goodbird.mindofthecolony.mc.api.util.constant.TranslationConstants.*;
import static com.goodbird.mindofthecolony.mc.api.util.constant.WindowConstants.CHAT_LABEL_ID;
import static com.goodbird.mindofthecolony.mc.api.util.constant.WindowConstants.RESPONSE_BOX_ID;
import static com.goodbird.mindofthecolony.mc.core.client.gui.WindowInteraction.BUTTON_RESPONSE_ID;

/**
 * Interaction for recruiting visitors
 */
public class RecruitmentInteraction extends ServerCitizenInteraction
{
    /**
     * The icon NBT tag
     */
    private static final String RECRUITMENT_ICON = "recruitIcon";

    /**
     * The icon's res location which is displayed for this interaction
     */
    private static final ResourceLocation icon = new ResourceLocation(Constants.MOD_ID, "textures/icons/recruiticon.png");

    /**
     * The recruit answer
     */
    private static final Tuple<Component, Component> recruitAnswer = new Tuple<>(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.chat.recruit"), Component.empty());

    @SuppressWarnings("unchecked")
    private static final Tuple<Component, Component>[] responses = (Tuple<Component, Component>[]) new Tuple[] {
      new Tuple<>(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.chat.showstats"), Component.empty()),
      recruitAnswer,
      new Tuple<>(Component.translatableEscape("com.goodbird.mindofthecolony.mc.coremod.gui.chat.notnow"), Component.empty())};

    /**
     * Chance for a bad visitor
     */
    private static final int BAD_VISITOR_CHANCE = 2;

    public RecruitmentInteraction(final ICitizen data)
    {
        super(data);
    }

    public RecruitmentInteraction(
      final Component inquiry,
      final IChatPriority priority)
    {
        super(inquiry, true, priority, d -> true, Component.empty(), responses);
    }

    @Override
    public List<IInteractionResponseHandler> genChildInteractions()
    {
        return Collections.emptyList();
    }

    @Override
    public String getType()
    {
        return ModInteractionResponseHandlers.RECRUITMENT.getPath();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onWindowOpened(final BOWindow window, final ICitizenDataView dataView)
    {
        final ButtonImage recruitButton = window.findPaneOfTypeByID(BUTTON_RESPONSE_ID + 2, ButtonImage.class);
        final Box group = window.findPaneOfTypeByID(RESPONSE_BOX_ID, Box.class);


        if (recruitButton != null && dataView instanceof IVisitorViewData visitorViewData)
        {
            final ItemStack recruitCost = visitorViewData.getRecruitCost();
            final IColonyView colony = (IColonyView) dataView.getColony();

            window.findPaneOfTypeByID(CHAT_LABEL_ID, Text.class).setText(PaneBuilders.textBuilder()
                .append(Component.literal(dataView.getName() + ": "))
                .append(this.getInquiry())
                .emptyLines(1)
                .appendNL(Component.translatable(
                    colony.getCitizens().size() < colony.getCitizenCountLimit() ? "com.goodbird.mindofthecolony.mc.coremod.gui.chat.recruitcost"
                        : "com.goodbird.mindofthecolony.mc.coremod.gui.chat.nospacerecruit",
                    recruitCost.getCount() + " " + recruitCost.getHoverName().getString()))
                .appendNL(Component.literal(""))
                .getText());

            int iconPosX = recruitButton.getX() + recruitButton.getWidth() - 28;
            int iconPosY = recruitButton.getY() + recruitButton.getHeight() - 18;
            ItemIcon icon = new ItemIcon();
            icon.setID(RECRUITMENT_ICON);
            icon.setSize(15, 15);
            group.addChild(icon);
            icon.setItem(recruitCost);
            icon.setPosition(iconPosX, iconPosY);
            icon.setVisible(true);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean onClientResponseTriggered(final int responseId, final Player player, final ICitizenDataView data, final BOWindow window)
    {
        final Component response = getPossibleResponses().get(responseId);
        // Validate recruitment before returning true
        if (response.equals(recruitAnswer.getA()) && data instanceof IVisitorViewData)
        {
            if (player.isCreative() || InventoryUtils.getItemCountInItemHandler(new InvWrapper(player.getInventory()), ((IVisitorViewData) data).getRecruitCost().getItem())
                  >= ((IVisitorViewData) data).getRecruitCost().getCount())
            {
                return super.onClientResponseTriggered(responseId, player, data, window);
            }
            else
            {
                MessageUtils.format(WARNING_RECRUITMENT_INSUFFICIENT_ITEMS).sendTo(player);
            }
        }
        return true;
    }

    @Override
    public void onServerResponseTriggered(final int responseId, final Player player, final ICitizenData data)
    {
        final Component response = getPossibleResponses().get(responseId);
        if (response.equals(recruitAnswer.getA()) && data instanceof IVisitorData)
        {
            IColony colony = data.getColony();
            if (colony.getCitizenManager().getCurrentCitizenCount() < colony.getCitizenManager().getPotentialMaxCitizens())
            {
                if (player.isCreative() || InventoryUtils.attemptReduceStackInItemHandler(new InvWrapper(player.getInventory()),
                  ((IVisitorData) data).getRecruitCost(),
                  ((IVisitorData) data).getRecruitCost().getCount(), true, true))
                {
                    // Recruits visitor as new citizen and respawns entity
                    colony.getVisitorManager().removeCivilian(data);
                    data.setHomeBuilding(null);
                    data.setJob(null);

                    final IBuilding tavern = colony.getServerBuildingManager().getFirstBuildingMatching(b -> b.getBuildingType() == ModBuildings.tavern.get());
                    
                    if (colony.getWorld().random.nextInt(100) <= BAD_VISITOR_CHANCE)
                    {
                        StatsUtil.trackStat(tavern, VISITORS_ABSCONDED, 1);
                        colony.getStatisticsManager().increment(VISITORS_ABSCONDED, colony.getDay());

                        MessageUtils.format(MESSAGE_RECRUITMENT_RAN_OFF, data.getName()).sendTo(colony).forAllPlayers();
                        return;
                    }
                    StatsUtil.trackStat(tavern, VISITORS_RECRUITED, 1);
                    colony.getStatisticsManager().increment(VISITORS_RECRUITED, colony.getDay());

                    // Create and read new citizen
                    ICitizenData newCitizen = colony.getCitizenManager().createAndRegisterCivilianData();
                    newCitizen.deserializeNBT(player.level().registryAccess(), data.serializeNBT(player.level().registryAccess()));
                    newCitizen.setParents("", "");
                    newCitizen.setLastPosition(data.getLastPosition());

                    // Exchange entities
                    newCitizen.updateEntityIfNecessary();
                    data.getEntity().ifPresent(e -> e.remove(Entity.RemovalReason.DISCARDED));

                    if (data.hasCustomTexture())
                    {
                        MessageUtils.format(MESSAGE_RECRUITMENT_SUCCESS_CUSTOM, data.getName()).sendTo(colony).forAllPlayers();
                    }
                    else
                    {
                        MessageUtils.format(MESSAGE_RECRUITMENT_SUCCESS, data.getName()).sendTo(colony).forAllPlayers();
                    }

                    IMinecoloniesAPI.getInstance().getEventBus().post(new CitizenAddedModEvent(newCitizen, CitizenAddedModEvent.CitizenAddedSource.HIRED));
                }
            }
            else
            {
                MessageUtils.format(WARNING_NO_COLONY_SPACE).sendTo(player);
            }
        }
    }

    @Override
    public ResourceLocation getInteractionIcon()
    {
        return icon;
    }
}
