package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.client.ChatWindowCitizen;
import com.goodbird.mindofthecolony.network.ChatMenuStateMessage;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.views.View;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add AI chat tab button to the citizen main window navigation.
 */
@Mixin(MainWindowCitizen.class)
public abstract class MixinMainWindowCitizen {

    @Shadow public abstract ICitizenDataView getCitizen();

    @Unique
    private static final String CHAT_TAB_ID = "mindofthecolony_chatTab";
    @Unique
    private static final String CHAT_ICON_ID = "mindofthecolony_chatIcon";

    /**
     * Inject at the end of onOpened to add our chat tab button and freeze the citizen.
     */
    @Inject(method = "onOpened", at = @At("TAIL"), remap = false)
    private void onOpenedInject(CallbackInfo ci) {
        MainWindowCitizen self = (MainWindowCitizen)(Object)this;

        ICitizenDataView citizen = getCitizen();
        PacketDistributor.sendToServer(new ChatMenuStateMessage(
            citizen.getColonyId(),
            citizen.getId(),
            true
        ));

        View window = ((Pane)self).getWindow();
        if (window == null) return;

        // Add chat tab button in the same style as other nav tabs
        // Position below the family tab (y=144) - we'll use y=170 like jobTab
        // Tab background
        ButtonImage chatTab = new ButtonImage();
        chatTab.setID(CHAT_TAB_ID);
        chatTab.setImage(ResourceLocation.parse("minecolonies:textures/gui/modules/tab_left_side2.png"));
        chatTab.setSize(32, 26);
        chatTab.setPosition(0, 170);
        window.addChild(chatTab);

        // Chat icon on top of tab
        ButtonImage chatIcon = new ButtonImage();
        chatIcon.setID(CHAT_ICON_ID);
        // Use the requests icon as a placeholder (speech bubble-like)
        chatIcon.setImage(ResourceLocation.parse("minecolonies:textures/gui/modules/requests.png"));
        chatIcon.setSize(20, 20);
        chatIcon.setPosition(5, 173);
        window.addChild(chatIcon);

        // Add tooltip
        PaneBuilders.tooltipBuilder()
            .hoverPane(chatIcon)
            .build()
            .setText(Component.literal("Chat with " + getCitizen().getName()));
    }

    /**
     * Inject into onButtonClicked to handle our chat tab button.
     */
    @Inject(method = "onButtonClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void onButtonClickedInject(Button button, CallbackInfo ci) {
        if (CHAT_TAB_ID.equals(button.getID()) || CHAT_ICON_ID.equals(button.getID())) {
            new ChatWindowCitizen(getCitizen()).open();
            ci.cancel();
        }
    }
}
