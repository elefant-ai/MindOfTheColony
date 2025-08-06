package com.goodbird.mindofthecolony.status;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenHappinessHandler;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenMournHandler;
import com.minecolonies.api.entity.citizen.happiness.IHappinessModifier;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.constant.TranslationConstants;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MinecoloniesStatusUtils {

    public static String getInventoryString(ICitizenData data) {
        InventoryCitizen inventory = data.getInventory();
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                String name = stack.getItem().getDescriptionId();
                counts.put(name, counts.getOrDefault(name, 0) + stack.getCount());
            }
        }
        ObjectStatus status = new ObjectStatus();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            status.add(entry.getKey(), entry.getValue().toString());
        }
        return status.toString();
    }

    public static String getEquipmentString(ICitizenData data) {
        InventoryCitizen inventory = data.getInventory();
        ObjectStatus status = new ObjectStatus();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack;
            if (slot.isArmor()) {
                stack = inventory.getArmorInSlot(slot);
            } else if (slot == EquipmentSlot.MAINHAND) {
                stack = data.getEntity().map(e -> e.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND)).orElse(ItemStack.EMPTY);
            } else if (slot == EquipmentSlot.OFFHAND) {
                stack = data.getEntity().map(e -> e.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND)).orElse(ItemStack.EMPTY);
            } else {
                continue;
            }
            status.add(slot.getName(), stack.isEmpty() ? "none" : stack.getItem().getDescriptionId());
        }
        return status.toString();
    }

    public static String getJobStatusString(ICitizenData data) {
        ObjectStatus status = new ObjectStatus();
        IJob<?> job = data.getJob();
        IBuilding workBuilding = data.getWorkBuilding();
        IBuilding homeBuilding = data.getHomeBuilding();

        status.add("job", job != null ? job.getJobRegistryEntry().getKey().getPath() : "unemployed");
        status.add("work_building", workBuilding != null ? formatBuildingInfo(workBuilding) : "none");
        status.add("home_building", homeBuilding != null ? formatBuildingInfo(homeBuilding) : "none");
        status.add("is_idle_at_job", String.valueOf(data.isIdleAtJob()));

        return status.toString();
    }

    public static String getSkillsString(ICitizenData data) {
        ObjectStatus status = new ObjectStatus();
        Map<Skill, CitizenSkillHandler.SkillData> skills = data.getCitizenSkillHandler().getSkills();
        for (Map.Entry<Skill, CitizenSkillHandler.SkillData> entry : skills.entrySet()) {
            String skillValue = String.format("Level %d (%.2f XP)", entry.getValue().getLevel(), entry.getValue().getExperience());
            status.add(entry.getKey().name().toLowerCase(), skillValue);
        }
        return status.toString();
    }

    public static String getDetailedSocialStatusString(ICitizenData data) {
        ObjectStatus status = new ObjectStatus();
        IColony colony = data.getColony();

        ICitizenData partner = data.getPartner();
        if (partner != null) {
            ObjectStatus partnerStatus = new ObjectStatus();
            partnerStatus.add("id", String.valueOf(partner.getId()));
            partnerStatus.add("name", partner.getName());
            status.add("partner", partnerStatus.toString());
        } else {
            status.add("partner", "\"none\"");
        }

        List<String> childrenInfo = data.getChildren().stream()
                .map(id -> colony.getCitizenManager().getCivilian(id))
                .filter(Objects::nonNull)
                .map(MinecoloniesStatusUtils::formatRelativeInfo)
                .collect(Collectors.toList());
        status.add("children", "[" + String.join(", ", childrenInfo) + "]");

        List<String> siblingsInfo = data.getSiblings().stream()
                .map(id -> colony.getCitizenManager().getCivilian(id))
                .filter(Objects::nonNull)
                .map(MinecoloniesStatusUtils::formatRelativeInfo)
                .collect(Collectors.toList());
        status.add("siblings", "[" + String.join(", ", siblingsInfo) + "]");

        Tuple<String, String> parents = data.getParents();
        status.add("parents", parents != null ? "\"" + parents.getA() + " and " + parents.getB() + "\"" : "\"unknown\"");

        return status.toString();
    }

    private static String formatRelativeInfo(ICitizenData relative) {
        ObjectStatus relativeStatus = new ObjectStatus();
        IJob<?> job = relative.getJob();
        relativeStatus.add("id", String.valueOf(relative.getId()));
        relativeStatus.add("name", relative.getName());
        relativeStatus.add("job", job != null ? job.getJobRegistryEntry().getKey().getPath() : (relative.isChild() ? "child" : "unemployed"));
        return relativeStatus.toString();
    }

    public static String getMourningStatusString(IColony colony) {
        ObjectStatus status = new ObjectStatus();
        for (ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
            ICitizenMournHandler mournHandler = citizen.getCitizenMournHandler();
            if (mournHandler.isMourning()) {
                status.add("is_mourning", "true");
                Set<String> deceasedNames = mournHandler.getDeceasedCitizens();
                String names = "[" + deceasedNames.stream()
                        .map(name -> "\"" + name + "\"")
                        .collect(Collectors.joining(", ")) + "]";
                status.add("in_memory_of", names);
                return status.toString();
            }
        }
        status.add("is_mourning", "false");
        status.add("in_memory_of", "[]");
        return status.toString();
    }

    public static String getPopulationString(IColony colony) {
        return String.format("%d/%d (potential: %d)",
                colony.getCitizenManager().getCurrentCitizenCount(),
                colony.getCitizenManager().getMaxCitizens(),
                colony.getCitizenManager().getPotentialMaxCitizens());
    }

    public static String getBuildingsString(IColony colony) {
        ObjectStatus status = new ObjectStatus();
        Map<String, Integer> buildingCounts = new HashMap<>();
        for (IBuilding building : colony.getBuildingManager().getBuildings().values()) {
            String name = building.getBuildingType().getRegistryName().getPath();
            buildingCounts.put(name, buildingCounts.getOrDefault(name, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : buildingCounts.entrySet()) {
            status.add(entry.getKey(), entry.getValue().toString());
        }
        return status.toString();
    }

    private static String formatBuildingInfo(IBuilding building) {
        BlockPos pos = building.getPosition();
        return String.format("%s (Level %d) at (%d, %d, %d)",
                building.getBuildingType().getRegistryName().getPath(),
                building.getBuildingLevel(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    public static String formatBlockPos(BlockPos pos) {
        if (pos == null) return "none";
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }

    public static String getCitizenRequestsString(ICitizenData data) {
        ObjectStatus requestsStatus = new ObjectStatus();
        IBuilding workBuilding = data.getWorkBuilding();

        if (workBuilding == null) {
            requestsStatus.add("status", "No workplace, no work-related requests.");
            return requestsStatus.toString();
        }
        
        Collection<IRequest<?>> openRequests = workBuilding.getOpenRequests(data.getId());
        if (openRequests.isEmpty()) {
            requestsStatus.add("status", "No open requests.");
            return requestsStatus.toString();
        }

        int requestIndex = 0;
        for (IRequest<?> request : openRequests) {
            if (request.getState() == RequestState.ASSIGNED || request.getState() == RequestState.REPORTED) {
                ObjectStatus singleRequestStatus = getObjectStatus(request);
                requestsStatus.add("request_" + requestIndex, singleRequestStatus.toString());
                requestIndex++;
            }
        }

        if (requestIndex == 0) {
            requestsStatus.add("status", "No pending items or tools needed.");
        }

        return requestsStatus.toString();
    }

    private static @NotNull ObjectStatus getObjectStatus(IRequest<?> request) {
        ObjectStatus singleRequestStatus = new ObjectStatus();
        String description = request.getShortDisplayString().getString();

        singleRequestStatus.add("description", description);
        singleRequestStatus.add("status", request.getState().name());

        List<ItemStack> displayStacks = request.getDisplayStacks();
        if (!displayStacks.isEmpty()) {
            ItemStack firstStack = displayStacks.get(0);
            singleRequestStatus.add("item", firstStack.getItem().getDescriptionId());
            singleRequestStatus.add("count", String.valueOf(firstStack.getCount()));
        }
        return singleRequestStatus;
    }

    public static String getHappinessModifiersString(ICitizenData data) {
        ObjectStatus status = new ObjectStatus();
        ICitizenHappinessHandler happinessHandler = data.getCitizenHappinessHandler();

        if (happinessHandler == null) {
            status.add("error", "Happiness handler not available.");
            return status.toString();
        }

        List<String> modifierIds = happinessHandler.getModifiers();

        for (String modifierId : modifierIds) {
            IHappinessModifier modifier = happinessHandler.getModifier(modifierId);
            if (modifier != null) {
                double factor = modifier.getFactor(data);
                String state;
                if (factor > 1.0) {
                    state = "Positive";
                } else if (factor < 1.0) {
                    state = "Negative";
                } else {
                    state = "Neutral";
                }

                String cleanId = modifier.getId().replace("minecolonies:", "");
                String translatableName = Component.translatable(TranslationConstants.PARTIAL_HAPPINESS_MODIFIER_NAME + cleanId).getString();

                if (translatableName.startsWith("com.minecolonies")) {
                    translatableName = cleanId.substring(0, 1).toUpperCase() + cleanId.substring(1);
                }

                status.add(translatableName, String.format("%s (Value: %.2f, Weight: %.2f)", state, factor, modifier.getWeight()));
            }
        }

        return status.toString();
    }
}