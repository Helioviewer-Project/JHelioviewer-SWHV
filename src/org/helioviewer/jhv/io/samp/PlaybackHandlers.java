package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.time.JHVTime;

import org.astrogrid.samp.Message;

final class PlaybackHandlers {

    private PlaybackHandlers() {
    }

    static void register(SampClient client) {
        client.addMessageHandler(setPlaybackHandler());
        client.addMessageHandler(commandHandler("jhv.play", Commands::play));
        client.addMessageHandler(commandHandler("jhv.pause", Commands::pause));
        client.addMessageHandler(commandHandler("jhv.toggle.playback", Commands::togglePlayback));
        client.addMessageHandler(commandHandler("jhv.next.frame", Commands::nextFrame));
        client.addMessageHandler(commandHandler("jhv.previous.frame", Commands::previousFrame));
        client.addMessageHandler(new SampClient.JHVSampHandler("jhv.seek.frame", (senderId, sender, msg) -> seekFrame(msg)));
        client.addMessageHandler(new SampClient.JHVSampHandler("jhv.seek.time", (senderId, sender, msg) -> seekTime(msg)));
    }

    private static SampClient.JHVSampHandler setPlaybackHandler() {
        return new SampClient.JHVSampHandler("jhv.set.playback", (senderId, sender, msg) -> {
            Commands.SetPlaybackArgs args = new Commands.SetPlaybackArgs(
                    SampClient.optionalString(msg, "advanceMode"),
                    SampClient.optionalString(msg, "speed"),
                    SampClient.optionalString(msg, "speedUnit"),
                    SampClient.optionalString(msg, "firstFrame"),
                    SampClient.optionalString(msg, "lastFrame"));
            EventQueue.invokeLater(() -> Commands.setPlayback(args));
        });
    }

    private static void seekFrame(Message msg) {
        String frame = SampClient.optionalString(msg, "frame");
        if (frame == null)
            return;
        try {
            int value = Integer.parseInt(frame);
            EventQueue.invokeLater(() -> Commands.seekFrame(value));
        } catch (NumberFormatException e) {
            Log.warn("Ignoring invalid seek frame value: " + frame, e);
        }
    }

    private static void seekTime(Message msg) {
        String time = SampClient.optionalString(msg, "time");
        if (time == null)
            return;
        try {
            JHVTime value = new JHVTime(time);
            EventQueue.invokeLater(() -> Commands.seekTime(value));
        } catch (RuntimeException e) {
            Log.warn("Ignoring invalid seek time value: " + time, e);
        }
    }

    private static SampClient.JHVSampHandler commandHandler(String type, Runnable command) {
        return new SampClient.JHVSampHandler(type, (senderId, sender, msg) -> EventQueue.invokeLater(command));
    }
}
