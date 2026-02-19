package com.goodbird.mindofthecolony.client;

import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.SwitchView;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.debug.gui.DebugWindowCitizen;
import com.minecolonies.core.debug.messages.DebugEnablePathfindingMessage;
import com.minecolonies.core.debug.messages.QueryCitizenAIHistoryMessage;
import com.goodbird.mindofthecolony.network.BackgroundRequestMessage;
import com.goodbird.mindofthecolony.network.EventDataRequestMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Tabbed debug window showing background, debug, and events tabs.
 */
public class TabbedDebugWindow extends AbstractWindowSkeleton {
    private static final Logger LOGGER = LoggerFactory.getLogger(TabbedDebugWindow.class);
    private static final ResourceLocation WINDOW_RESOURCE =
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "gui/citizen/debug_tabbed.xml");

    private final ICitizenDataView citizen;
    private final int citizenId;
    private final int colonyId;

    private SwitchView contentPages;
    private String currentTab = "debugPage";

    // Pathfinding tracking state (shared with vanilla debug)
    private static boolean trackingDebug = false;

    // Cached reference to MineColonies network
    private static Object mineColoniesNetwork = null;
    private static Method sendToServerMethod = null;

    public TabbedDebugWindow(Object citizenObj) {
        super(WINDOW_RESOURCE);
        this.citizen = (ICitizenDataView) citizenObj;
        this.citizenId = citizen.getId();
        this.colonyId = citizen.getColonyId();

        // Get the switch view
        contentPages = findPaneOfTypeByID("contentPages", SwitchView.class);

        // Set up tab buttons
        setupTabButtons();

        // Set up debug page buttons
        setupDebugPage();

        // Request background data from server
        PacketDistributor.sendToServer(new BackgroundRequestMessage(colonyId, citizenId));

        // Request event data from server
        PacketDistributor.sendToServer(new EventDataRequestMessage(colonyId));

        // Default to debug tab
        switchToTab("debugPage");
    }

    private void setupTabButtons() {
        registerButton("tabBackground", b -> switchToTab("backgroundPage"));
        registerButton("tabDebug", b -> switchToTab("debugPage"));
        registerButton("tabEvents", b -> switchToTab("eventsPage"));
    }

    private void setupDebugPage() {
        // Set citizen/colony ID text
        findPaneOfTypeByID("citizenid", Text.class)
            .setText(Component.literal("Citizen ID: " + citizenId));
        findPaneOfTypeByID("colonyid", Text.class)
            .setText(Component.literal("Colony ID: " + colonyId));

        // AI History button
        findPaneOfTypeByID("aihistory", Button.class).setHandler(b -> {
            sendMineColoniesMessage(new QueryCitizenAIHistoryMessage(citizen));
        });

        // Pathfinding button
        findPaneOfTypeByID("pathfinding", Button.class).setHandler(b -> {
            trackingDebug = !trackingDebug;
            if (trackingDebug) {
                DebugWindowCitizen.outputMessage = Component.literal("Receiving pathfinding data");
            }
            sendMineColoniesMessage(new DebugEnablePathfindingMessage(citizen, trackingDebug));
            updatePathfindingButtonText();
        });
        updatePathfindingButtonText();
    }

    /**
     * Sends a message using MineColonies' internal Network class via reflection.
     * This is necessary because Network is not part of the public API.
     */
    private void sendMineColoniesMessage(Object message) {
        try {
            if (mineColoniesNetwork == null) {
                Class<?> networkClass = Class.forName("com.minecolonies.core.Network");
                Method getNetworkMethod = networkClass.getMethod("getNetwork");
                mineColoniesNetwork = getNetworkMethod.invoke(null);
                sendToServerMethod = mineColoniesNetwork.getClass().getMethod("sendToServer",
                    Class.forName("com.minecolonies.core.network.messages.IMessage"));
            }
            sendToServerMethod.invoke(mineColoniesNetwork, message);
        } catch (Exception e) {
            LOGGER.error("Failed to send MineColonies message: {}", e.getMessage());
        }
    }

    private void updatePathfindingButtonText() {
        findPaneOfTypeByID("pathfinding", Button.class)
            .setText(Component.literal(trackingDebug ? "Disable Pathfinding" : "Enable Pathfinding"));
    }

    private void switchToTab(String tabId) {
        currentTab = tabId;
        if (contentPages != null) {
            contentPages.setView(tabId);
        }

        // Update tab button appearance
        updateTabButtonStyles();

        // Refresh content for the selected tab
        switch (tabId) {
            case "backgroundPage" -> updateBackgroundTab();
            case "debugPage" -> updateDebugTab();
            case "eventsPage" -> updateEventsTab();
        }
    }

    private void updateTabButtonStyles() {
        // Highlight active tab (optional styling)
        Button bgTab = findPaneOfTypeByID("tabBackground", Button.class);
        Button debugTab = findPaneOfTypeByID("tabDebug", Button.class);
        Button eventsTab = findPaneOfTypeByID("tabEvents", Button.class);

        // Simple active indicator via label
        bgTab.setText(Component.literal(currentTab.equals("backgroundPage") ? "[Background]" : "Background"));
        debugTab.setText(Component.literal(currentTab.equals("debugPage") ? "[Debug]" : "Debug"));
        eventsTab.setText(Component.literal(currentTab.equals("eventsPage") ? "[Events]" : "Events"));
    }

    private void updateBackgroundTab() {
        // Set citizen name
        findPaneOfTypeByID("citizenName", Text.class)
            .setText(Component.literal(citizen.getName()));

        // Get cached background data
        ClientBackgroundData data = ClientBackgroundCache.getData(citizenId);
        if (data != null) {
            // Backstory
            Text backstoryText = findPaneOfTypeByID("backstoryText", Text.class);
            backstoryText.setText(Component.literal(data.backstory() != null ? data.backstory() : "No backstory available"));

            // Permanent traits
            Text permanentTraitsText = findPaneOfTypeByID("permanentTraits", Text.class);
            if (data.permanentTraits() != null && !data.permanentTraits().isEmpty()) {
                permanentTraitsText.setText(Component.literal(String.join("\n", data.permanentTraits())));
            } else {
                permanentTraitsText.setText(Component.literal("None"));
            }

            // Temporary traits
            Text temporaryTraitsText = findPaneOfTypeByID("temporaryTraits", Text.class);
            if (data.temporaryTraits() != null && !data.temporaryTraits().isEmpty()) {
                temporaryTraitsText.setText(Component.literal(String.join("\n", data.temporaryTraits())));
            } else {
                temporaryTraitsText.setText(Component.literal("None"));
            }

            // Permanent Effects
            Text permanentEffectsText = findPaneOfTypeByID("permanentEffects", Text.class);
            if (data.permanentEffects() != null && !data.permanentEffects().isEmpty()) {
                permanentEffectsText.setText(Component.literal(String.join("\n", data.permanentEffects())));
            } else {
                permanentEffectsText.setText(Component.literal("None"));
            }

            // Temporary Effects
            Text temporaryEffectsText = findPaneOfTypeByID("temporaryEffects", Text.class);
            if (data.temporaryEffects() != null && !data.temporaryEffects().isEmpty()) {
                temporaryEffectsText.setText(Component.literal(String.join("\n", data.temporaryEffects())));
            } else {
                temporaryEffectsText.setText(Component.literal("None"));
            }
        } else {
            // No data yet
            findPaneOfTypeByID("backstoryText", Text.class)
                .setText(Component.literal("Loading..."));
            findPaneOfTypeByID("permanentTraits", Text.class)
                .setText(Component.literal("Loading..."));
            findPaneOfTypeByID("temporaryTraits", Text.class)
                .setText(Component.literal("Loading..."));
            findPaneOfTypeByID("permanentEffects", Text.class)
                .setText(Component.literal("Loading..."));
            findPaneOfTypeByID("temporaryEffects", Text.class)
                .setText(Component.literal("Loading..."));
        }
    }

    private void updateDebugTab() {
        // Debug output is updated in onUpdate()
    }

    private void updateEventsTab() {
        // Get cached event data
        ClientEventCache.ColonyEventData eventData = ClientEventCache.get(colonyId);
        if (eventData != null) {
            // Weather status
            Text weatherStatus = findPaneOfTypeByID("weatherStatus", Text.class);
            weatherStatus.setText(Component.literal(eventData.weather() != null ? eventData.weather() : "Unknown"));

            // Events list
            Text eventsList = findPaneOfTypeByID("eventsList", Text.class);
            if (eventData.recentEvents() != null && !eventData.recentEvents().isEmpty()) {
                eventsList.setText(Component.literal(String.join("\n\n", eventData.recentEvents())));
            } else {
                eventsList.setText(Component.literal("No recent events"));
            }
        } else {
            // No data yet
            findPaneOfTypeByID("weatherStatus", Text.class)
                .setText(Component.literal("Loading..."));
            findPaneOfTypeByID("eventsList", Text.class)
                .setText(Component.literal("Loading..."));
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // Update debug output if on debug tab
        if (currentTab.equals("debugPage")) {
            Text output = findPaneOfTypeByID("output", Text.class);
            output.setText(DebugWindowCitizen.outputMessage);
        }

        // Refresh background tab periodically to catch new data
        if (currentTab.equals("backgroundPage")) {
            updateBackgroundTab();
        }

        // Refresh events tab periodically
        if (currentTab.equals("eventsPage")) {
            updateEventsTab();
        }
    }
}
