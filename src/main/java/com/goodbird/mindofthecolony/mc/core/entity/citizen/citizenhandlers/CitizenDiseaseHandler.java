package com.goodbird.mindofthecolony.mc.core.entity.citizen.citizenhandlers;

import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.AbstractEntityCitizen;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.citizenhandlers.ICitizenDiseaseHandler;
import com.goodbird.mindofthecolony.mc.core.MineColonies;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.workerbuildings.BuildingCook;
import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.config.DiseaseConfig;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.AbstractJobGuard;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.JobHealer;
import com.goodbird.mindofthecolony.mc.core.datalistener.model.Disease;
import com.goodbird.mindofthecolony.mc.core.datalistener.DiseasesListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import static com.goodbird.mindofthecolony.mc.api.research.util.ResearchConstants.MASKS;
import static com.goodbird.mindofthecolony.mc.api.research.util.ResearchConstants.VACCINES;
import static com.goodbird.mindofthecolony.mc.api.util.constant.CitizenConstants.*;
import static com.goodbird.mindofthecolony.mc.api.util.constant.Constants.ONE_HUNDRED_PERCENT;
import static com.goodbird.mindofthecolony.mc.api.util.constant.StatisticsConstants.CITIZENS_HEALED;

/**
 * Handler taking care of citizens getting stuck.
 */
public class CitizenDiseaseHandler implements ICitizenDiseaseHandler
{
    /**
     * Health at which citizens seek a doctor.
     */
    public static final double SEEK_DOCTOR_HEALTH = 6.0;

    /**
     * Base likelihood of a citizen getting a disease.
     */
    private static final int DISEASE_FACTOR = 100000 / 3;

    /**
     * Number of ticks after recovering a citizen is immune against any illness. 90 Minutes currently.
     */
    private static final int IMMUNITY_TIME = 20 * 60 * 90;

    /**
     * Additional immunity time through vaccines.
     */
    private static final int VACCINE_MODIFIER = 10;

    /**
     * The citizen assigned to this manager.
     */
    private final ICitizenData citizenData;

    /**
     * The disease the citizen has, empty if none.
     */
    @Nullable
    private Disease disease;

    /**
     * Special immunity time after being cured.
     */
    private int immunityTicks = 0;

    /**
     * Whether the citizen sleeps at the hostpital
     */
    private boolean sleepsAtHospital = false;


    /**
     * The initial citizen count
     */
    private static final int initialCitizenCount = IMinecoloniesAPI.getInstance()
      .getConfig()
      .getServer().initialCitizenAmount.get();

    /**
     * Constructor for the experience handler.
     *
     * @param citizen the citizen owning the handler.
     */
    public CitizenDiseaseHandler(final ICitizenData citizen)
    {
        this.citizenData = citizen;
    }

    /**
     * Called in the citizen every few ticks to check for illness. Called every 60 ticks.
     * MOTC: Disabled vanilla random disease rolls - our ColonyEventManager handles disease via DiseaseOutbreakEvaluator.
     * Only keeps the immunity tick countdown logic.
     */
    @Override
    public void update(final int tickRate)
    {
        // DO NOT roll for random disease here - our event system handles this via DiseaseOutbreakEvaluator
        // The event system provides better control based on weather, food, happiness, and traits

        // Keep immunity tick countdown from vanilla
        if (immunityTicks > 0)
        {
            immunityTicks -= tickRate;
        }
    }

    @Override
    public boolean setDisease(final @Nullable Disease disease)
    {
        if (canBecomeSick())
        {
            this.disease = disease;
            return true;
        }
        return false;
    }

    /**
     * Check if the citizen may become sick.
     * MOTC: Removed hardcoded healer immunity - healers now use a very low modifier instead.
     *
     * @return true if so.
     */
    private boolean canBecomeSick()
    {
        return !isSick()
            && citizenData.getEntity().isPresent()
            && citizenData.getColony().isActive()
            && immunityTicks <= 0
            && citizenData.getColony().getCitizenManager().getCurrentCitizenCount() > initialCitizenCount;
    }

    /**
     * MOTC: Apply job and trait contact spread modifiers.
     */
    @Override
    public void onCollission(final ICitizenData citizen)
    {
        if (citizen.getCitizenDiseaseHandler().isSick()
              && canBecomeSick())
        {
            // Get the job-specific contact modifier
            String jobId = getJobId();
            double contactModifier = DiseaseConfig.getContactModifier(jobId);

            // Apply trait-based contact modifier
            if (citizenData instanceof IExtendedCitizenData extData)
            {
                CitizenBackground bg = extData.getCitizenBackground();
                if (bg != null)
                {
                    contactModifier *= bg.getContactDiseaseRateModifier();
                }
            }

            // Base chance is 1% (1 in 100), apply contact modifier
            double effectiveChance = contactModifier;

            if (citizen.getRandom().nextDouble() * ONE_HUNDRED_PERCENT < effectiveChance)
            {
                if (citizen.getColony().getResearchManager().getResearchEffects().getEffectStrength(MASKS) <= 0
                    || citizen.getRandom().nextBoolean())
                {
                    this.disease = citizen.getCitizenDiseaseHandler().getDisease();
                }
            }
        }
    }

    /**
     * Get the job ID for the citizen, or "default" if no job.
     * MOTC: Added helper method for disease config lookups.
     */
    private String getJobId()
    {
        if (citizenData.getJob() != null)
        {
            String jobName = citizenData.getJob().getJobRegistryEntry().getTranslationKey();
            if (jobName.contains("."))
            {
                return jobName.substring(jobName.lastIndexOf(".") + 1).toLowerCase();
            }
            return jobName.toLowerCase();
        }
        return "default";
    }

    @Override
    public boolean isHurt()
    {
        return citizenData.getEntity().isPresent() && !(citizenData.getJob() instanceof AbstractJobGuard) && citizenData.getEntity().get().getHealth() < SEEK_DOCTOR_HEALTH
            && citizenData.getSaturation() > LOW_SATURATION;
    }

    @Override
    public boolean isSick()
    {
        return disease != null;
    }

    @Override
    public void write(final CompoundTag compound)
    {
        CompoundTag diseaseTag = new CompoundTag();
        if (disease != null)
        {
            diseaseTag.putString(TAG_DISEASE_ID, disease.id().toString());
        }
        diseaseTag.putInt(TAG_IMMUNITY, immunityTicks);
        compound.put(TAG_DISEASE, diseaseTag);
    }

    @Override
    public void read(final CompoundTag compound)
    {
        if (!compound.contains(TAG_DISEASE, Tag.TAG_COMPOUND))
        {
            return;
        }

        CompoundTag diseaseTag = compound.getCompound(TAG_DISEASE);
        if (diseaseTag.contains(TAG_DISEASE_ID))
        {
            this.disease = DiseasesListener.getDisease(ResourceLocation.parse(diseaseTag.getString(TAG_DISEASE_ID)));
        }
        this.immunityTicks = diseaseTag.getInt(TAG_IMMUNITY);
    }

    @Override
    @Nullable
    public Disease getDisease()
    {
        return this.disease;
    }

    @Override
    public void cure()
    {
        this.disease = null;
        sleepsAtHospital = false;
        if (citizenData.isAsleep() && citizenData.getEntity().isPresent())
        {
            citizenData.getEntity().get().stopSleeping();
            final BlockPos hospitalPos = citizenData.getColony().getServerBuildingManager().getBestBuilding(citizenData.getEntity().get(), BuildingCook.class);
            final IColony colony = citizenData.getColony();
            final IBuilding hospital = colony.getServerBuildingManager().getBuilding(hospitalPos);
            if (hospital != null)
            {
                hospital.onWakeUp();
            }

            if (citizenData.getColony().getResearchManager().getResearchEffects().getEffectStrength(VACCINES) > 0)
            {
                immunityTicks = IMMUNITY_TIME * VACCINE_MODIFIER;
            }
            else
            {
                immunityTicks = IMMUNITY_TIME;
            }
        }
        else
        {
            // Less immunity time if not cored in bed, but still have immunity time.
            immunityTicks = IMMUNITY_TIME / 2;
        }

        citizenData.getColony().getStatisticsManager().increment(CITIZENS_HEALED, citizenData.getColony().getDay());
        citizenData.markDirty(0);
    }

    @Override
    public boolean sleepsAtHospital()
    {
        return sleepsAtHospital;
    }

    @Override
    public void setSleepsAtHospital(final boolean isAtHospital)
    {
        sleepsAtHospital = isAtHospital;
    }
}
