package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.event.ColonyEvent;
import com.goodbird.mindofthecolony.event.ColonyEventManager;
import com.goodbird.mindofthecolony.event.WeatherState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Client -> Server: Request event data for a colony.
 */
public record EventDataRequestMessage(
    int colonyId
) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDataRequestMessage.class);

    public static final Type<EventDataRequestMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "event_data_request")
    );

    public static final StreamCodec<FriendlyByteBuf, EventDataRequestMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, EventDataRequestMessage::colonyId,
        EventDataRequestMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EventDataRequestMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ColonyEventManager eventManager = ColonyEventManager.getIfExists(msg.colonyId());

                String weatherStatus;
                List<String> recentEvents = new ArrayList<>();

                if (eventManager != null) {
                    WeatherState weather = eventManager.getWeatherState();

                    // Build weather status string
                    if (weather.isCurrentlyThundering()) {
                        int minutes = weather.getTicksThundering() / 1200;
                        weatherStatus = "Thunderstorm" + (minutes > 0 ? " (for " + minutes + " min)" : "");
                    } else if (weather.isCurrentlyRaining()) {
                        int minutes = weather.getTicksRaining() / 1200;
                        weatherStatus = "Raining" + (minutes > 0 ? " (for " + minutes + " min)" : "");
                    } else {
                        weatherStatus = "Clear";
                    }

                    // Get recent events formatted
                    long currentTick = player.level().getGameTime();
                    for (ColonyEvent event : eventManager.getRecentEvents(10)) {
                        long ticksAgo = currentTick - event.getTimestamp();
                        String timeAgo = formatTicksAgo(ticksAgo);
                        recentEvents.add(timeAgo + ": " + event.getDescription());
                    }
                } else {
                    weatherStatus = "Unknown";
                }

                PacketDistributor.sendToPlayer(player,
                    new EventDataResponseMessage(msg.colonyId(), weatherStatus, recentEvents));
            }
        });
    }

    private static String formatTicksAgo(long ticks) {
        long minutes = ticks / 1200;
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        long days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }
}
