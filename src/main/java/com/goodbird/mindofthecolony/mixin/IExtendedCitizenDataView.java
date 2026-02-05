package com.goodbird.mindofthecolony.mixin;

import com.goodbird.mindofthecolony.background.TraitModifiers;

/**
 * Extended interface for CitizenDataView to access trait modifiers on the client side.
 */
public interface IExtendedCitizenDataView {

    /**
     * Get the trait modifiers synced from the server.
     */
    TraitModifiers getTraitModifiers();

    /**
     * Set the trait modifiers (called during deserialization).
     */
    void setTraitModifiers(TraitModifiers modifiers);
}
