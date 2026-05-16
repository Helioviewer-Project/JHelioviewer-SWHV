package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.app.Commands;

import org.astrogrid.samp.client.AbstractMessageHandler;

final class CameraHandlers {

    private CameraHandlers() {}

    static void register(SampClient client) {
        client.addMessageHandler(commandHandler("jhv.view.zoom-in", Commands::zoomIn));
        client.addMessageHandler(commandHandler("jhv.view.zoom-out", Commands::zoomOut));
        client.addMessageHandler(commandHandler("jhv.view.zoom-fit", Commands::zoomFit));
        client.addMessageHandler(commandHandler("jhv.view.zoom-1-to-1", Commands::zoomOneToOne));
        client.addMessageHandler(commandHandler("jhv.view.reset", Commands::resetView));
        client.addMessageHandler(commandHandler("jhv.view.reset-axis", Commands::resetViewAxis));
        client.addMessageHandler(SampHandlers.create("jhv.view.rotate90",
                (senderId, sender, msg) -> EventQueue.invokeLater(() -> Commands.rotateView90(SampHandlers.optionalString(msg, "axis")))));
    }

    private static AbstractMessageHandler commandHandler(String type, Runnable command) {
        return SampHandlers.create(type, (senderId, sender, msg) -> EventQueue.invokeLater(command));
    }
}
