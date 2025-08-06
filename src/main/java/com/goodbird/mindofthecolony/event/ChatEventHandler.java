package com.goodbird.mindofthecolony.event;

import com.goodbird.mindofthecolony.CitizenAIManager;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mindofthecolony")
public class ChatEventHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        CitizenAIManager.getInstance().broadcastPlayerMessage(event.getPlayer(), event.getMessage().getString());
    }
}
