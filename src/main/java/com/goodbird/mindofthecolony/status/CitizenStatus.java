package com.goodbird.mindofthecolony.status;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import net.minecraft.world.entity.LivingEntity;

import java.util.stream.Collectors;

public class CitizenStatus extends ObjectStatus {

    public static CitizenStatus fromCitizen(ICitizenData data) {
        CitizenStatus status = new CitizenStatus();
        if (data == null) {
            status.add("error", "Citizen data is null");
            return status;
        }

        LivingEntity entity = data.getEntity().orElse(null);

        status.add("name", data.getName());
        status.add("id", String.valueOf(data.getId()));
        status.add("gender", data.isFemale() ? "female" : "male");
        status.add("is_child", String.valueOf(data.isChild()));

        status.add("health", entity != null ? String.format("%.2f/%.2f", entity.getHealth(), entity.getMaxHealth()) : "N/A");
        status.add("saturation", String.format("%.2f/20.0", data.getSaturation()));

        boolean isSick = data.getEntity().map(e -> e.getCitizenData().getCitizenDiseaseHandler().isSick()).orElse(false);
        status.add("is_sick", String.valueOf(isSick));
        if (isSick) {
            status.add("sickness", data.getEntity().get().getCitizenData().getCitizenDiseaseHandler().getDisease().name().getString());
        }

        var citizenStatus = data.getStatus();
        status.add("status_key", citizenStatus != null ? citizenStatus.getTranslationKey() : "unknown");
        status.add("is_asleep", String.valueOf(data.isAsleep()));
        status.add("is_mourning", String.valueOf(data.getCitizenMournHandler().isMourning()));
        status.add("location", MinecoloniesStatusUtils.formatBlockPos(data.getLastPosition()));
        status.add("bed_location", MinecoloniesStatusUtils.formatBlockPos(data.getBedPos()));

        status.add("job_info", MinecoloniesStatusUtils.getJobStatusString(data));
        status.add("current_needs", MinecoloniesStatusUtils.getCitizenRequestsString(data));
        status.add("social_status", MinecoloniesStatusUtils.getDetailedSocialStatusString(data));

        status.add("skills", MinecoloniesStatusUtils.getSkillsString(data));
        status.add("happiness", String.format("%.2f/10.0", data.getCitizenHappinessHandler().getHappiness(data.getColony(), data)));
        status.add("happiness_modifiers", MinecoloniesStatusUtils.getHappinessModifiersString(data));

        status.add("inventory", MinecoloniesStatusUtils.getInventoryString(data));
        status.add("equipment", MinecoloniesStatusUtils.getEquipmentString(data));

        if (data instanceof IExtendedCitizenData extData) {
            CitizenBackground bg = extData.getCitizenBackground();
            if (bg != null && bg.isInitialized()) {
                status.add("background_origin", bg.getOrigin());
                status.add("background_personality", bg.getPersonalityTrait());
                status.add("background_penalties",
                    "[" + bg.getPenalties().stream()
                        .map(p -> "\"" + p + "\"")
                        .collect(Collectors.joining(", ")) + "]");
            }
        }

        return status;
    }
}