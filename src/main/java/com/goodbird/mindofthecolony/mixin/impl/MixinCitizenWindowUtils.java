package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.TraitModifiers;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenDataView;
import com.ldtteam.blockui.controls.Text;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.CitizenWindowUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add skill bonus indicators to the citizen window.
 * Uses @Redirect to intercept setText() before the text is committed to rendering.
 */
@Mixin(value = CitizenWindowUtils.class, remap = false)
public class MixinCitizenWindowUtils {

    @Unique
    private static ICitizenDataView mindOfTheColony$currentCitizen;

    /**
     * Capture the citizen reference before skill content is created.
     */
    @Inject(method = "createSkillContent", at = @At("HEAD"))
    private static void captureContext(ICitizenDataView citizen, AbstractWindowSkeleton window, CallbackInfo ci) {
        mindOfTheColony$currentCitizen = citizen;
    }

    /**
     * Redirect setText to include skill bonus indicators.
     * This intercepts the setText call BEFORE it happens, allowing us to modify the Component.
     */
    @Redirect(
        method = "createSkillContent",
        at = @At(value = "INVOKE", target = "Lcom/ldtteam/blockui/controls/Text;setText(Lnet/minecraft/network/chat/Component;)V")
    )
    private static void redirectSetSkillText(Text textPane, Component originalText) {
        if (mindOfTheColony$currentCitizen instanceof IExtendedCitizenDataView extView) {
            TraitModifiers modifiers = extView.getTraitModifiers();
            if (modifiers != null && modifiers.skillBonuses() != null) {
                // Get skill ID from the text pane's ID
                String skillId = textPane.getID();
                if (skillId != null) {
                    int bonus = modifiers.skillBonuses().getOrDefault(skillId, 0);
                    if (bonus != 0) {
                        // Append bonus to the original text
                        String bonusStr = (bonus > 0 ? " +" : " ") + bonus;
                        MutableComponent newText = originalText.copy();
                        newText.append(Component.literal(bonusStr)
                            .withStyle(bonus > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
                        textPane.setText(newText);
                        return;
                    }
                }
            }
        }
        // Default: just set the original text
        textPane.setText(originalText);
    }

    /**
     * Clear citizen reference after skill content is created.
     */
    @Inject(method = "createSkillContent", at = @At("TAIL"))
    private static void clearContext(ICitizenDataView citizen, AbstractWindowSkeleton window, CallbackInfo ci) {
        mindOfTheColony$currentCitizen = null;
    }
}
