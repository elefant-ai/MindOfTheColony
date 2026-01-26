package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityCitizen.class)
public abstract class MixinEntityCitizen {

    @Shadow
    public abstract ICitizenData getCitizenData();

    /**
     * Intercept player right-click on citizen to initiate AI chat.
     * When player right-clicks without sneaking and with empty main hand,
     * send a greeting to the AI instead of opening the normal GUI.
     */
    @Inject(
        method = "checkAndHandleImportantInteractions",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void onPlayerInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // Only handle server-side
        if (player.level().isClientSide()) {
            return;
        }

        // Only handle main hand interactions
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        // Only trigger chat when not sneaking (sneaking = open inventory/GUI)
        if (player.isShiftKeyDown()) {
            return;
        }

        ICitizenData citizenData = getCitizenData();
        if (citizenData == null) {
            return;
        }

        CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridge(citizenData.getId());
        if (bridge == null || !bridge.isReady()) {
            return;
        }

        // Make citizen look at player
        EntityCitizen self = (EntityCitizen) (Object) this;
        self.getLookControl().setLookAt(player);
        self.getNavigation().stop();

        // Send context message to NPC indicating player approached
        String playerName = player.getName().getString();
        bridge.sendInteractionMessage(playerName, "[" + playerName + " approached and wants to talk]");

        // Notify player
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.literal("[" + citizenData.getName() + " turns to face you...]")
            );
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
