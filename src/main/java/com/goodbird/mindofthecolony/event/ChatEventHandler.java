package com.goodbird.mindofthecolony.event;

import com.goodbird.mindofthecolony.CitizenAIManager;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "mindofthecolony")
public class ChatEventHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        CitizenAIManager.getInstance().broadcastPlayerMessage(event.getPlayer(), event.getRawText());
    }
}
