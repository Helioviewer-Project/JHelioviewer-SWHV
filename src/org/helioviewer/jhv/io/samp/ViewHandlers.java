package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.app.Commands;

final class ViewHandlers {

    private ViewHandlers() {
    }

    static void register(SampClient client) {
        client.addMessageHandler(new SampClient.JHVSampHandler("jhv.view.set", (senderId, sender, msg) -> {
            String projection = SampClient.optionalString(msg, "projection");
            String annotationMode = SampClient.optionalString(msg, "annotationMode");
            String multiview = SampClient.optionalString(msg, "multiview");
            String tracking = SampClient.optionalString(msg, "tracking");
            String refresh = SampClient.optionalString(msg, "refresh");
            String showCorona = SampClient.optionalString(msg, "showCorona");
            String differentialRotation = SampClient.optionalString(msg, "differentialRotation");
            EventQueue.invokeLater(() -> Commands.setViewStateRaw(projection, annotationMode, multiview, tracking,
                    refresh, showCorona, differentialRotation));
        }));
    }
}
