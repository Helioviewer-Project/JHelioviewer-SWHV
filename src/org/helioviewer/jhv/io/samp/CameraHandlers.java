package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.app.Commands;

final class CameraHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(SampHandlers.command("jhv.view.zoom-in", Commands::zoomIn));
        client.addMessageHandler(SampHandlers.command("jhv.view.zoom-out", Commands::zoomOut));
        client.addMessageHandler(SampHandlers.command("jhv.view.zoom-fit", Commands::zoomFit));
        client.addMessageHandler(SampHandlers.command("jhv.view.zoom-1-to-1", Commands::zoomOneToOne));
        client.addMessageHandler(SampHandlers.command("jhv.view.reset", Commands::resetView));
        client.addMessageHandler(SampHandlers.command("jhv.view.reset-axis", Commands::resetViewAxis));
        client.addMessageHandler(SampHandlers.create("jhv.view.rotate90",
                (senderId, sender, msg) -> EventQueue.invokeLater(() -> Commands.rotateView90(SampHandlers.optionalString(msg, "axis")))));
    }

    private CameraHandlers() {}
}
