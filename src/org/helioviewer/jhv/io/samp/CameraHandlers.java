package org.helioviewer.jhv.io.samp;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.thread.UiThread;

final class CameraHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(SampHandlers.command("jhv.view.zoom-in", DisplayController::zoomIn));
        client.addMessageHandler(SampHandlers.command("jhv.view.zoom-out", DisplayController::zoomOut));
        client.addMessageHandler(SampHandlers.command("jhv.view.zoom-fit", DisplayController::zoomFit));
        client.addMessageHandler(SampHandlers.command("jhv.view.zoom-1-to-1", DisplayController::zoomOneToOne));
        client.addMessageHandler(SampHandlers.command("jhv.view.reset", DisplayController::resetView));
        client.addMessageHandler(SampHandlers.command("jhv.view.reset-axis", DisplayController::resetViewAxis));
        client.addMessageHandler(SampHandlers.create("jhv.view.rotate90",
                (senderId, sender, msg) -> UiThread.post(() -> DisplayController.rotateView90(SampHandlers.optionalString(msg, "axis")))));
    }

    private CameraHandlers() {}
}
