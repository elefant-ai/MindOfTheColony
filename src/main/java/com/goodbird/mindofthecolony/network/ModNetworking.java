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

        // Client -> Server: Player opens/closes chat menu (freeze/unfreeze citizen)
        registrar.playToServer(
            ChatMenuStateMessage.TYPE,
            ChatMenuStateMessage.STREAM_CODEC,
            ChatMenuStateMessage::handle
        );

        // Server -> Client: NPC response sent to player
        registrar.playToClient(
            AIChatResponseMessage.TYPE,
            AIChatResponseMessage.STREAM_CODEC,
            AIChatResponseMessage::handle
        );

        // Client -> Server: Request background data for a citizen
        registrar.playToServer(
            BackgroundRequestMessage.TYPE,
            BackgroundRequestMessage.STREAM_CODEC,
            BackgroundRequestMessage::handle
        );

        // Server -> Client: Background data response
        registrar.playToClient(
            BackgroundResponseMessage.TYPE,
            BackgroundResponseMessage.STREAM_CODEC,
            BackgroundResponseMessage::handle
        );

        // Client -> Server: Request event data for a colony
        registrar.playToServer(
            EventDataRequestMessage.TYPE,
            EventDataRequestMessage.STREAM_CODEC,
            EventDataRequestMessage::handle
        );

        // Server -> Client: Event data response
        registrar.playToClient(
            EventDataResponseMessage.TYPE,
            EventDataResponseMessage.STREAM_CODEC,
            EventDataResponseMessage::handle
        );
    }
}
