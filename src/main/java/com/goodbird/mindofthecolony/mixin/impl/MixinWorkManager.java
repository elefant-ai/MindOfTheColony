package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.goodbird.mindofthecolony.preference.WorkPreferences;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.modules.settings.StringSetting;
import com.minecolonies.core.colony.workorders.WorkManager;
import com.minecolonies.core.colony.workorders.WorkOrderBuilding;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder.MANUAL_SETTING;
import static com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder.MODE;

/**
 * Mixin to add preference-based work order assignment.
 * NPCs can express preferences that influence which work orders they get assigned.
 */
@Mixin(WorkManager.class)
public abstract class MixinWorkManager {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(MixinWorkManager.class);

    @Shadow
    private Colony colony;

    @Shadow
    public abstract Map<Integer, IServerWorkOrder> getWorkOrders();

    /**
     * Inject preference-based sorting into work order assignment.
     * This runs after the standard assignment loop and re-assigns work orders
     * to builders based on their preferences when multiple work orders are available.
     */
    @Inject(method = "onColonyTick", at = @At("TAIL"), remap = false)
    private void applyWorkPreferences(@NotNull IColony colony, CallbackInfo ci) {
        // Get list of unclaimed work orders
        List<IServerWorkOrder> unclaimedOrders = new ArrayList<>();
        for (IServerWorkOrder order : getWorkOrders().values()) {
            if (!order.isClaimed()) {
                unclaimedOrders.add(order);
            }
        }

        if (unclaimedOrders.isEmpty()) {
            return;
        }

        // Get available builders without work who have preferences
        List<BuilderWithPrefs> availableBuilders = new ArrayList<>();
        for (IBuilding building : this.colony.getBuildingManager().getBuildings().values()) {
            if (building instanceof AbstractBuildingStructureBuilder builder) {
                // Skip if already has work
                if (builder.hasWorkOrder()) {
                    continue;
                }

                // Skip manual mode builders
                StringSetting setting = building.getSetting(MODE);
                if (setting != null && setting.getValue().equals(MANUAL_SETTING)) {
                    continue;
                }

                ICitizenData citizen = building.getFirstModuleOccurance(WorkerBuildingModule.class).getFirstCitizen();
                if (citizen == null) {
                    continue;
                }

                // Check if citizen has preferences
                if (citizen instanceof IExtendedCitizenData extData) {
                    WorkPreferences prefs = extData.getWorkPreferences();
                    if (prefs != null && prefs.hasPreferences()) {
                        availableBuilders.add(new BuilderWithPrefs(builder, citizen, prefs));
                    }
                }
            }
        }

        if (availableBuilders.isEmpty()) {
            return;
        }

        // For each available builder with preferences, find best matching unclaimed work order
        for (BuilderWithPrefs bwp : availableBuilders) {
            IServerWorkOrder bestOrder = null;
            int bestScore = Integer.MIN_VALUE;

            for (IServerWorkOrder order : unclaimedOrders) {
                if (!order.canBuild(bwp.builder)) {
                    continue;
                }

                int score = mindOfTheColony$calculatePreferenceScore(bwp.prefs, order);
                // Also factor in original priority
                score += order.getPriority();

                if (score > bestScore) {
                    bestScore = score;
                    bestOrder = order;
                }
            }

            if (bestOrder != null) {
                bwp.builder.setWorkOrder(bestOrder);
                bestOrder.setClaimedBy(bwp.builder.getID());
                unclaimedOrders.remove(bestOrder);

                LOGGER.debug("Assigned work order {} to {} based on preferences (score: {})",
                    bestOrder.getID(), bwp.citizen.getName(), bestScore);
            }
        }
    }

    @Unique
    private int mindOfTheColony$calculatePreferenceScore(WorkPreferences prefs, IServerWorkOrder order) {
        String category = mindOfTheColony$getWorkOrderCategory(order);
        String action = order.getWorkOrderType().name();
        String buildingType = "";

        if (order instanceof WorkOrderBuilding wobld) {
            // Extract building type from translation key
            // e.g., "com.minecolonies.building.residence" -> "residence"
            String transKey = wobld.getTranslationKey();
            if (transKey != null && transKey.contains(".")) {
                buildingType = transKey.substring(transKey.lastIndexOf(".") + 1);
            }
        }

        return prefs.calculateScore(category, action, buildingType);
    }

    @Unique
    private String mindOfTheColony$getWorkOrderCategory(IServerWorkOrder order) {
        // Map work order class to category string
        String className = order.getClass().getSimpleName().toLowerCase();
        if (className.contains("miner")) return "miner";
        if (className.contains("decoration")) return "decoration";
        if (className.contains("plantation")) return "plantation_field";
        return "building"; // Default for WorkOrderBuilding and others
    }

    @Unique
    private static class BuilderWithPrefs {
        final AbstractBuildingStructureBuilder builder;
        final ICitizenData citizen;
        final WorkPreferences prefs;

        BuilderWithPrefs(AbstractBuildingStructureBuilder builder, ICitizenData citizen, WorkPreferences prefs) {
            this.builder = builder;
            this.citizen = citizen;
            this.prefs = prefs;
        }
    }
}
