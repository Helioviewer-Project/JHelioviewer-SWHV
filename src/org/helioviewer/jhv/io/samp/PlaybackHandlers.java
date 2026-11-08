package org.helioviewer.jhv.io.samp;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.time.JHVTime;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.json.JSONObject;

final class PlaybackHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(playbackSetHandler());
        client.addMessageHandler(SampHandlers.command("jhv.playback.play", Player::play));
        client.addMessageHandler(SampHandlers.command("jhv.playback.pause", Player::pause));
        client.addMessageHandler(SampHandlers.command("jhv.playback.toggle", Player::toggle));
        client.addMessageHandler(SampHandlers.command("jhv.playback.next-frame", Player::nextFrame));
        client.addMessageHandler(SampHandlers.command("jhv.playback.previous-frame", Player::previousFrame));
        client.addMessageHandler(SampHandlers.create("jhv.playback.seek-frame",
                (senderId, sender, msg) -> playbackSeekFrame(msg)));
        client.addMessageHandler(SampHandlers.create("jhv.playback.seek-time",
                (senderId, sender, msg) -> playbackSeekTime(msg)));
    }

    private static AbstractMessageHandler playbackSetHandler() {
        return SampHandlers.create("jhv.playback.set", (senderId, sender, msg) -> {
            JSONObject parameters = SampHandlers.parameters(msg, "advanceMode", "speed", "speedUnit", "firstFrame", "lastFrame");
            UiThread.post(() -> ViewState.applyPlayback(parameters));
        });
    }

    private static void playbackSeekFrame(Message msg) {
        String frame = SampHandlers.optionalString(msg, "frame");
        if (frame == null)
            return;
        try {
            int value = Integer.parseInt(frame);
            UiThread.post(() -> Player.setFrame(value));
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
            UiThread.post(() -> Player.setTime(value));
        } catch (RuntimeException e) {
            Log.warn("Ignoring invalid seek time value: " + time, e);
        }
    }

    private PlaybackHandlers() {}
}
