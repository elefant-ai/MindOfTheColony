package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.fork.event.MotcClientEventHandler;
import com.goodbird.mindofthecolony.fork.event.MotcDataPackSyncEventHandler;
import com.goodbird.mindofthecolony.fork.event.MotcEventHandler;
import com.goodbird.mindofthecolony.fork.event.MotcFMLEventHandler;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.event.ClientEventHandler;
import com.minecolonies.core.event.DataPackSyncEventHandler;
import com.minecolonies.core.event.EventHandler;
import com.minecolonies.core.event.FMLEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MineColonies.class, remap = false)
public abstract class MixinMineColonies {

    private static final Logger LOGGER = LoggerFactory.getLogger("MindOfTheColony");

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructorTail(FMLModContainer modContainer, Dist dist, CallbackInfo ci) {
        final IEventBus forgeBus = NeoForge.EVENT_BUS;

        // Unregister MineColonies' event handlers from the forge bus
        forgeBus.unregister(EventHandler.class);
        forgeBus.unregister(FMLEventHandler.class);
        forgeBus.unregister(DataPackSyncEventHandler.ServerEvents.class);

        // Register our replacement handlers
        forgeBus.register(MotcEventHandler.class);
        forgeBus.register(MotcFMLEventHandler.class);
        forgeBus.register(MotcDataPackSyncEventHandler.ServerEvents.class);

        if (dist.isClient()) {
            forgeBus.unregister(ClientEventHandler.class);
            forgeBus.unregister(DataPackSyncEventHandler.ClientEvents.class);

            forgeBus.register(MotcClientEventHandler.class);
            forgeBus.register(MotcDataPackSyncEventHandler.ClientEvents.class);
        }

        LOGGER.info("Took over MineColonies event handlers");
    }
}
