package org.helioviewer.jhv.io.samp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.threads.EDTQueue;
import org.helioviewer.jhv.threads.JHVThread;

import org.astrogrid.samp.client.AbstractMessageHandler;

final class LoadImageHandler {

    private LoadImageHandler() {}

    static AbstractMessageHandler create() {
        return SampHandlers.create("jhv.load.image", (senderId, sender, msg) -> {
            Commands.OperationContext context = SampClient.operationContext(senderId, msg, "jhv.load.image", "jhv.load.image.completed");
            if (!LoadHandlers.loadURIList(msg,
                    uri -> waitImageLoad(context, Commands.loadImage(uri)),
                    uris -> waitImageLoad(context, Commands.loadImage(uris)))) {
                context.complete(false, "Missing jhv.load.image url.", null);
            }
        });
    }

    private static void waitImageLoad(Commands.OperationContext context, CompletableFuture<ImageLayer> future) {
        future.whenComplete((layer, t) -> {
            if (t != null) {
                context.complete(false, message(t), null);
                return;
            }
            if (layer == null) {
                context.complete(false, "No image files found.", null);
                return;
            }
            JHVThread.create(() -> {
                try {
                    new ImageLayers.WaitUntilLoaded(List.of(layer)).call();
                    boolean success = EDTQueue.invokeAndWait(() -> Layers.getImageLayers().contains(layer) && layer.isViewLoadFinished());
                    context.complete(success, success ? "Image loaded." : "Image load failed.", null);
                } catch (Exception e) {
                    context.complete(false, message(e), null);
                }
            }, "JHV-WaitImageLoad").start();
        });
    }

    private static String message(Throwable t) {
        return t.getMessage() == null || t.getMessage().isBlank() ? "Image load failed." : t.getMessage();
    }

}
