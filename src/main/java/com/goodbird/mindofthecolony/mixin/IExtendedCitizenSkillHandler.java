package com.goodbird.mindofthecolony.mixin;

import com.minecolonies.api.colony.ICitizenData;

/**
 * Extended interface for CitizenSkillHandler to access the owning citizen.
 */
public interface IExtendedCitizenSkillHandler {

    /**
     * Set the citizen that owns this skill handler.
     */
    void mindOfTheColony$setCitizen(ICitizenData citizen);

    /**
     * Get the citizen that owns this skill handler.
     */
    ICitizenData mindOfTheColony$getCitizen();
}
