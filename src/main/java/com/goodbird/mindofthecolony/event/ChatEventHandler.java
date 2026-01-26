package com.goodbird.mindofthecolony.event;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * Handles player chat events and routes messages to nearby citizens.
 */
@EventBusSubscriber(modid = "mindofthecolony")
public class ChatEventHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        CitizenNpcManager.getInstance().broadcastPlayerMessage(
            event.getPlayer(),
            event.getMessage().getString()
        );
    }
}
