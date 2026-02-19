package com.goodbird.mindofthecolony.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays TTS audio data positionally from an entity's location in the world.
 * Audio volume attenuates with distance to simulate 3D spatial audio.
 */
@OnlyIn(Dist.CLIENT)
public class ClientAudioPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientAudioPlayer.class);

    private static final float MAX_DISTANCE = 32.0f;
    private static final float FULL_VOLUME_DISTANCE = 4.0f;

    private static final ExecutorService AUDIO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MindOfTheColony-TTS-Audio");
        t.setDaemon(true);
        return t;
    });

    private static volatile Clip currentClip = null;

    /**
     * Plays audio data positionally from the given entity.
     */
    public static void play(int entityId, byte[] audioData) {
        AUDIO_EXECUTOR.submit(() -> {
            try {
                playInternal(entityId, audioData);
            } catch (Exception e) {
                LOGGER.error("Failed to play TTS audio: {}", e.getMessage());
            }
        });
    }

    /**
     * Stops any currently playing TTS audio.
     */
    public static void stop() {
        Clip clip = currentClip;
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
            currentClip = null;
        }
    }

    private static void playInternal(int entityId, byte[] audioData) throws Exception {
        // Stop any currently playing clip
        stop();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Find the entity for positional audio
        Entity entity = player.level().getEntity(entityId);

        // Calculate distance-based volume
        float volume = 1.0f;
        if (entity != null) {
            double distance = player.distanceTo(entity);
            volume = calculateVolume(distance);
        }

        if (volume <= 0.0f) return;

        AudioInputStream audioStream = AudioSystem.getAudioInputStream(
            new ByteArrayInputStream(audioData)
        );

        Clip clip = AudioSystem.getClip();
        currentClip = clip;
        clip.open(audioStream);

        // Apply volume
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            // Convert linear volume (0-1) to decibels
            float dB = (float) (20.0 * Math.log10(Math.max(volume, 0.0001)));
            dB = Math.max(dB, gainControl.getMinimum());
            dB = Math.min(dB, gainControl.getMaximum());
            gainControl.setValue(dB);
        }

        // Register listener before start to catch STOP events on short clips
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                clip.close();
                if (currentClip == clip) {
                    currentClip = null;
                }
            }
        });

        clip.start();
    }

    private static float calculateVolume(double distance) {
        if (distance <= FULL_VOLUME_DISTANCE) {
            return 1.0f;
        }
        if (distance >= MAX_DISTANCE) {
            return 0.0f;
        }
        // Linear falloff between FULL_VOLUME_DISTANCE and MAX_DISTANCE
        return 1.0f - (float) ((distance - FULL_VOLUME_DISTANCE) / (MAX_DISTANCE - FULL_VOLUME_DISTANCE));
    }
}
