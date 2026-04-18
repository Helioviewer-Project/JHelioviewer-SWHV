package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Commands;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;

final class SampRecordingHandlers {

    private SampRecordingHandlers() {
    }

    static AbstractMessageHandler startHandler() {
        return new SampClient.JHVSampHandler("jhv.record.start", (senderId, sender, msg) -> start(msg, senderId));
    }

    static AbstractMessageHandler stopHandler() {
        return new SampClient.JHVSampHandler("jhv.record.stop", (senderId, sender, msg) -> EventQueue.invokeLater(Commands::recordStop));
    }

    static void start(Message msg, String senderId) {
        String requestId = optionalString(msg, "requestId");
        Commands.OperationContext context = new Commands.OperationContext(SampClient.class, senderId, requestId, "jhv.record.start");
        Commands.RecordStartArgs args = new Commands.RecordStartArgs(
                optionalString(msg, "mode"),
                optionalString(msg, "size"),
                optionalString(msg, "advanceMode"),
                optionalString(msg, "speed"),
                optionalString(msg, "speedUnit"));
        EventQueue.invokeLater(() -> Commands.recordStart(context, args));
    }

    private static @Nullable String optionalString(Message msg, String key) {
        Object value = msg.getParam(key);
        return value == null ? null : value.toString();
    }
}
