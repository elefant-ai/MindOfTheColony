package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.TraitModifiers;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenDataView;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.View;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.CitizenWindowUtils;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Mixin to add skill bonus indicators to the citizen window.
 */
@Mixin(value = CitizenWindowUtils.class, remap = false)
public class MixinCitizenWindowUtils {

    @Unique
    private static final int COLOR_POSITIVE = 0x00AA00;  // Green
    @Unique
    private static final int COLOR_NEGATIVE = 0xAA0000;  // Red

    /**
     * Add skill bonus indicators next to skill levels.
     */
    @Inject(method = "createSkillContent", at = @At("TAIL"))
    private static void addSkillIndicators(ICitizenDataView citizen, AbstractWindowSkeleton window, CallbackInfo ci) {
        if (!(citizen instanceof IExtendedCitizenDataView extView)) {
            return;
        }

        TraitModifiers modifiers = extView.getTraitModifiers();
        if (modifiers == null) {
            return;
        }

        Map<String, Integer> skillBonuses = modifiers.skillBonuses();
        if (skillBonuses == null || skillBonuses.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> entry : skillBonuses.entrySet()) {
            String skillId = entry.getKey().toLowerCase();
            int bonus = entry.getValue();
            if (bonus == 0) {
                continue;
            }

            // Find existing skill text element
            Text skillText = window.findPaneOfTypeByID(skillId, Text.class);
            if (skillText == null) {
                continue;
            }

            // Create indicator text next to skill level
            String indicatorId = skillId + "_trait_indicator";

            // Check if indicator already exists (for refresh)
            Pane existing = window.findPaneByID(indicatorId);
            if (existing != null) {
                if (existing instanceof Text existingText) {
                    mindOfTheColony$updateIndicator(existingText, bonus);
                }
                continue;
            }

            // Create new indicator
            Text indicator = new Text();
            indicator.setID(indicatorId);
            indicator.setSize(20, 12);
            // Position after skill level text
            indicator.setPosition(skillText.getX() + 17, skillText.getY());

            mindOfTheColony$updateIndicator(indicator, bonus);

            // Add to parent view
            View parent = skillText.getParent();
            if (parent != null) {
                parent.addChild(indicator);
            }
        }
    }

    @Unique
    private static void mindOfTheColony$updateIndicator(Text indicator, int bonus) {
        String text = (bonus > 0 ? "+" : "") + bonus;
        indicator.setText(Component.literal(text));
        indicator.setColors(bonus > 0 ? COLOR_POSITIVE : COLOR_NEGATIVE);
    }
}
