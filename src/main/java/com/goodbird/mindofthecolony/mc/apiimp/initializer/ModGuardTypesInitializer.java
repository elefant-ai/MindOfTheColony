package com.goodbird.mindofthecolony.mc.apiimp.initializer;

import com.goodbird.mindofthecolony.mc.api.colony.guardtype.GuardType;
import com.goodbird.mindofthecolony.mc.api.colony.guardtype.registry.ModGuardTypes;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.ModJobs;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.Skill;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.apiimp.CommonMinecoloniesAPIImpl;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.JobDruid;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.JobKnight;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.JobRanger;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.goodbird.mindofthecolony.mc.api.util.constant.translation.JobTranslationConstants.*;

public final class ModGuardTypesInitializer
{
    public final static DeferredRegister<GuardType> DEFERRED_REGISTER = DeferredRegister.create(CommonMinecoloniesAPIImpl.GUARD_TYPES, Constants.MOD_ID);

    private ModGuardTypesInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModGuardTypesInitializer but this is a Utility class.");
    }

    static
    {
        ModGuardTypes.knight = DEFERRED_REGISTER.register(ModGuardTypes.KNIGHT_ID.getPath(), () -> new GuardType.Builder()
                                 .setJobTranslationKey(JOB_KNIGHT)
                                 .setButtonTranslationKey(JOB_KNIGHT_BUTTON)
                                 .setPrimarySkill(Skill.Adaptability)
                                 .setSecondarySkill(Skill.Stamina)
                                 .setWorkerSoundName("archer")
                                 .setJobEntry(() -> ModJobs.knight.get())
                                 .setRegistryName(ModGuardTypes.KNIGHT_ID)
                                 .setClazz(JobKnight.class)
                                 .createGuardType());

        ModGuardTypes.ranger = DEFERRED_REGISTER.register(ModGuardTypes.RANGER_ID.getPath(), () -> new GuardType.Builder()
                                 .setJobTranslationKey(JOB_RANGER)
                                 .setButtonTranslationKey(JOB_RANGER_BUTTON)
                                 .setPrimarySkill(Skill.Agility)
                                 .setSecondarySkill(Skill.Adaptability)
                                 .setWorkerSoundName("archer")
                                 .setJobEntry(() -> ModJobs.archer.get())
                                 .setRegistryName(ModGuardTypes.RANGER_ID)
                                 .setClazz(JobRanger.class)
                                 .createGuardType());

        ModGuardTypes.druid = DEFERRED_REGISTER.register(ModGuardTypes.DRUID_ID.getPath(), () -> new GuardType.Builder()
          .setJobTranslationKey(JOB_DRUID)
          .setButtonTranslationKey(JOB_DRUID_BUTTON)
          .setPrimarySkill(Skill.Mana)
          .setSecondarySkill(Skill.Focus)
          .setWorkerSoundName("druid")
          .setJobEntry(() -> ModJobs.druid.get())
          .setRegistryName(ModGuardTypes.DRUID_ID)
          .setClazz(JobDruid.class)
          .createGuardType());
    }
}
