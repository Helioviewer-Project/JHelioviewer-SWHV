package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.math.Quat;

import org.astrogrid.samp.Message;

final class CameraHandlers {

    private CameraHandlers() {
    }

    static void register(SampClient client) {
        client.addMessageHandler(commandHandler("jhv.view.zoom-in", Commands::zoomIn));
        client.addMessageHandler(commandHandler("jhv.view.zoom-out", Commands::zoomOut));
        client.addMessageHandler(commandHandler("jhv.view.zoom-fit", Commands::zoomFit));
        client.addMessageHandler(commandHandler("jhv.view.zoom-one-to-one", Commands::zoomOneToOne));
        client.addMessageHandler(commandHandler("jhv.view.reset", Commands::resetView));
        client.addMessageHandler(commandHandler("jhv.view.reset-axis", Commands::resetViewAxis));
        client.addMessageHandler(new SampClient.JHVSampHandler("jhv.view.rotate90",
                (senderId, sender, msg) -> viewRotate90(msg)));
    }

    private static void viewRotate90(Message msg) {
        String axis = SampClient.optionalString(msg, "axis");
        if (axis == null)
            return;

        Quat rotation = switch (axis.toUpperCase()) {
            case "X" -> Quat.X90;
            case "Y" -> Quat.Y90;
            case "Z" -> Quat.Z90;
            default -> null;
        };
        if (rotation == null) {
            Log.warn("Ignoring invalid rotate view axis value: " + axis);
            return;
        }
        EventQueue.invokeLater(() -> Commands.rotateView90(rotation));
    }

    private static SampClient.JHVSampHandler commandHandler(String type, Runnable command) {
        return new SampClient.JHVSampHandler(type, (senderId, sender, msg) -> EventQueue.invokeLater(command));
    }
}
