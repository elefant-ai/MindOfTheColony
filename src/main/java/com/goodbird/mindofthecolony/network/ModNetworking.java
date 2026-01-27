package com.goodbird.mindofthecolony.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Handles registration of network payloads for the mod.
 */
public class ModNetworking {
    public static final String PROTOCOL_VERSION = "1.0";

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("mindofthecolony").versioned(PROTOCOL_VERSION);

        // Client -> Server: Player sends chat message to citizen
        registrar.playToServer(
            AIChatMessage.TYPE,
            AIChatMessage.STREAM_CODEC,
            AIChatMessage::handle
        );

        // Server -> Client: NPC response sent to player
        registrar.playToClient(
            AIChatResponseMessage.TYPE,
            AIChatResponseMessage.STREAM_CODEC,
            AIChatResponseMessage::handle
        );
    }
}
