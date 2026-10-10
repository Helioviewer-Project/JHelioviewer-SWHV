package org.helioviewer.jhv.io.samp;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.thread.UiThread;

final class ViewHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(SampHandlers.create("jhv.view.set", (senderId, sender, msg) -> {
            String projection = SampHandlers.optionalString(msg, "projection");
            String annotationMode = SampHandlers.optionalString(msg, "annotationMode");
            String multiview = SampHandlers.optionalString(msg, "multiview");
            String tracking = SampHandlers.optionalString(msg, "tracking");
            String refresh = SampHandlers.optionalString(msg, "refresh");
            String showCorona = SampHandlers.optionalString(msg, "showCorona");
            String differentialRotation = SampHandlers.optionalString(msg, "differentialRotation");
            UiThread.post(() -> Commands.setViewStateRaw(projection, annotationMode, multiview, tracking,
                    refresh, showCorona, differentialRotation));
        }));
    }

    private ViewHandlers() {}
}
