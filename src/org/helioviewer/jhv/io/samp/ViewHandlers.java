package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.app.Commands;

final class ViewHandlers {

    private ViewHandlers() {}

    static void register(SampClient client) {
        client.addMessageHandler(SampHandlers.create("jhv.view.set", (senderId, sender, msg) -> {
            String projection = SampHandlers.optionalString(msg, "projection");
            String annotationMode = SampHandlers.optionalString(msg, "annotationMode");
            String multiview = SampHandlers.optionalString(msg, "multiview");
            String tracking = SampHandlers.optionalString(msg, "tracking");
            String refresh = SampHandlers.optionalString(msg, "refresh");
            String showCorona = SampHandlers.optionalString(msg, "showCorona");
            String differentialRotation = SampHandlers.optionalString(msg, "differentialRotation");
            EventQueue.invokeLater(() -> Commands.setViewStateRaw(projection, annotationMode, multiview, tracking,
                    refresh, showCorona, differentialRotation));
        }));
    }
}
