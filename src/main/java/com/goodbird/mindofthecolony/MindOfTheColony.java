package com.goodbird.mindofthecolony;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("mindofthecolony")
public class MindOfTheColony {
    public MindOfTheColony() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CitizenAIManager.getInstance().onServerTick();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        System.out.println("Mind of the Colony is shutting down AI bridges.");
        CitizenAIManager.getInstance().clearAllAIs();
    }
}
