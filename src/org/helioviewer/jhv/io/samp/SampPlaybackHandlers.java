package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.app.Commands;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;

final class SampPlaybackHandlers {

    private SampPlaybackHandlers() {
    }

    static AbstractMessageHandler setPlaybackHandler() {
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

    static AbstractMessageHandler commandHandler(String type, Runnable command) {
        return new SampClient.JHVSampHandler(type, (senderId, sender, msg) -> EventQueue.invokeLater(command));
    }
}
