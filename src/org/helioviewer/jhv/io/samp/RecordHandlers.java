package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.app.Commands;

import org.astrogrid.samp.Message;

final class RecordHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(SampHandlers.create("jhv.record.set", (senderId, sender, msg) -> {
            String mode = SampHandlers.optionalString(msg, "mode");
            String size = SampHandlers.optionalString(msg, "size");
            EventQueue.invokeLater(() -> Commands.setRecordingRaw(mode, size));
        }));
        client.addMessageHandler(SampHandlers.create("jhv.record.start",
                (senderId, sender, msg) -> recordStart(msg, senderId)));
        client.addMessageHandler(SampHandlers.create("jhv.record.stop",
                (senderId, sender, msg) -> EventQueue.invokeLater(Commands::recordStop)));
    }

    private static void recordStart(Message msg, String senderId) {
        Commands.OperationContext context = SampClient.operationContext(senderId, msg, "jhv.record.start", "jhv.record.start.completed");
        Commands.RecordStartInput input = new Commands.RecordStartInput(
                SampHandlers.optionalString(msg, "mode"),
                SampHandlers.optionalString(msg, "size"),
                SampHandlers.optionalString(msg, "advanceMode"),
                SampHandlers.optionalString(msg, "speed"),
                SampHandlers.optionalString(msg, "speedUnit"));
        EventQueue.invokeLater(() -> Commands.recordStart(context, input));
    }

    private RecordHandlers() {}
}
