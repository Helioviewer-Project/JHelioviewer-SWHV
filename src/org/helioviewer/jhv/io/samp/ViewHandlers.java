package org.helioviewer.jhv.io.samp;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.thread.UiThread;

import org.json.JSONObject;

final class ViewHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(SampHandlers.create("jhv.view.set", (senderId, sender, msg) -> {
            JSONObject parameters = SampHandlers.parameters(msg, "projection", "annotationMode", "multiview", "tracking",
                    "refresh", "showCorona", "differentialRotation");
            UiThread.post(() -> Commands.setViewState(parameters));
        }));
    }

    private ViewHandlers() {}
}
