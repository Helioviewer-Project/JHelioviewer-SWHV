package org.helioviewer.jhv.io.samp;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.thread.UiThread;

import org.astrogrid.samp.Message;
import org.json.JSONObject;

final class RecordHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(SampHandlers.create("jhv.record.set", (senderId, sender, msg) -> {
            JSONObject parameters = SampHandlers.parameters(msg, "mode", "size");
            UiThread.post(() -> Commands.setRecording(parameters));
        }));
        client.addMessageHandler(SampHandlers.create("jhv.record.start",
                (senderId, sender, msg) -> recordStart(msg, senderId)));
        client.addMessageHandler(SampHandlers.create("jhv.record.stop",
                (senderId, sender, msg) -> UiThread.post(Commands::recordStop)));
    }

    private static void recordStart(Message msg, String senderId) {
        Commands.Completion completion = SampClient.completion(senderId, msg, "jhv.record.start", "jhv.record.start.completed");
        JSONObject parameters = SampHandlers.parameters(msg, "mode", "size", "advanceMode", "speed", "speedUnit");
        UiThread.post(() -> Commands.recordStart(completion, parameters));
    }

    private RecordHandlers() {}
}
