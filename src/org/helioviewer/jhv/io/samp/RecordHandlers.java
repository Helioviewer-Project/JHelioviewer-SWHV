package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.app.Commands;

import org.astrogrid.samp.Message;

final class RecordHandlers {

    private RecordHandlers() {}

    static void register(SampClient client) {
        client.addMessageHandler(new SampClient.JHVSampHandler("jhv.record.set", (senderId, sender, msg) -> {
            String mode = SampClient.optionalString(msg, "mode");
            String size = SampClient.optionalString(msg, "size");
            EventQueue.invokeLater(() -> Commands.setRecordingRaw(mode, size));
        }));
        client.addMessageHandler(new SampClient.JHVSampHandler("jhv.record.start",
                (senderId, sender, msg) -> recordStart(msg, senderId)));
        client.addMessageHandler(new SampClient.JHVSampHandler("jhv.record.stop",
                (senderId, sender, msg) -> EventQueue.invokeLater(Commands::recordStop)));
    }

    private static void recordStart(Message msg, String senderId) {
        String requestId = SampClient.optionalString(msg, "requestId");
        Commands.OperationContext context = new Commands.OperationContext(SampClient.class, senderId, requestId, "jhv.record.start");
        Commands.RecordStartInput input = new Commands.RecordStartInput(
                SampClient.optionalString(msg, "mode"),
                SampClient.optionalString(msg, "size"),
                SampClient.optionalString(msg, "advanceMode"),
                SampClient.optionalString(msg, "speed"),
                SampClient.optionalString(msg, "speedUnit"));
        EventQueue.invokeLater(() -> Commands.recordStart(context, input));
    }
}
