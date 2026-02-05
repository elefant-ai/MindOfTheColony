package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.mixin.IExtendedCitizenSkillHandler;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

/**
 * Mixin to allow direct skill level modifications from traits.
 */
@Mixin(value = CitizenSkillHandler.class, remap = false)
public class MixinCitizenSkillHandler implements IExtendedCitizenSkillHandler {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(MixinCitizenSkillHandler.class);

    @Shadow
    public Map<Skill, CitizenSkillHandler.SkillData> skillMap;

    @Unique
    private ICitizenData mindOfTheColony$citizen;

    @Override
    public void mindOfTheColony$setCitizen(ICitizenData citizen) {
        this.mindOfTheColony$citizen = citizen;
    }

    @Override
    public ICitizenData mindOfTheColony$getCitizen() {
        return mindOfTheColony$citizen;
    }

    @Override
    public void mindOfTheColony$applySkillBonus(String skillName, int bonus) {
        try {
            // Skill enum uses PascalCase (e.g., Stamina, Strength), not UPPERCASE
            String pascalCase = skillName.substring(0, 1).toUpperCase() + skillName.substring(1).toLowerCase();
            Skill skill = Skill.valueOf(pascalCase);

            CitizenSkillHandler.SkillData data = skillMap.get(skill);
            if (data != null) {
                int beforeLevel = data.getLevel();
                CitizenSkillHandler self = (CitizenSkillHandler) (Object) this;
                self.incrementLevel(skill, bonus);
                LOGGER.debug("Applied skill bonus to {}: {} -> {} (bonus {})",
                    skill.name(), beforeLevel, skillMap.get(skill).getLevel(), bonus);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown skill name: {}", skillName);
        }
    }
}
