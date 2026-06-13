package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.time.JHVTime;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;

final class PlaybackHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(playbackSetHandler());
        client.addMessageHandler(commandHandler("jhv.playback.play", Commands::play));
        client.addMessageHandler(commandHandler("jhv.playback.pause", Commands::pause));
        client.addMessageHandler(commandHandler("jhv.playback.toggle", Commands::togglePlayback));
        client.addMessageHandler(commandHandler("jhv.playback.next-frame", Commands::nextFrame));
        client.addMessageHandler(commandHandler("jhv.playback.previous-frame", Commands::previousFrame));
        client.addMessageHandler(SampHandlers.create("jhv.playback.seek-frame",
                (senderId, sender, msg) -> playbackSeekFrame(msg)));
        client.addMessageHandler(SampHandlers.create("jhv.playback.seek-time",
                (senderId, sender, msg) -> playbackSeekTime(msg)));
    }

    private static AbstractMessageHandler playbackSetHandler() {
        return SampHandlers.create("jhv.playback.set", (senderId, sender, msg) -> {
            Commands.PlaybackInput input = new Commands.PlaybackInput(
                    SampHandlers.optionalString(msg, "advanceMode"),
                    SampHandlers.optionalString(msg, "speed"),
                    SampHandlers.optionalString(msg, "speedUnit"),
                    SampHandlers.optionalString(msg, "firstFrame"),
                    SampHandlers.optionalString(msg, "lastFrame"));
            EventQueue.invokeLater(() -> Commands.setPlayback(input));
        });
    }

    private static void playbackSeekFrame(Message msg) {
        String frame = SampHandlers.optionalString(msg, "frame");
        if (frame == null)
            return;
        try {
            int value = Integer.parseInt(frame);
            EventQueue.invokeLater(() -> Commands.seekFrame(value));
        } catch (NumberFormatException e) {
            Log.warn("Ignoring invalid seek frame value: " + frame, e);
        }
    }

    private static void playbackSeekTime(Message msg) {
        String time = SampHandlers.optionalString(msg, "time");
        if (time == null)
            return;
        try {
            JHVTime value = new JHVTime(time);
            EventQueue.invokeLater(() -> Commands.seekTime(value));
        } catch (RuntimeException e) {
            Log.warn("Ignoring invalid seek time value: " + time, e);
        }
    }

    private static AbstractMessageHandler commandHandler(String type, Runnable command) {
        return SampHandlers.create(type, (senderId, sender, msg) -> EventQueue.invokeLater(command));
    }

    private PlaybackHandlers() {}
}
