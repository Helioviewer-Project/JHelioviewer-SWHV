package org.helioviewer.jhv.io.samp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.EDTQueue;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.json.JSONObject;

final class LoadImageHandler {

    static AbstractMessageHandler create() {
        return SampHandlers.create("jhv.load.image", (senderId, sender, msg) -> {
            Commands.OperationContext context = SampClient.operationContext(senderId, msg, "jhv.load.image", "jhv.load.image.completed");
            try {
                JSONObject imageParams = imageParams(msg);
                if (!LoadHandlers.loadURIList(msg,
                        uri -> waitImageLoad(context, Commands.loadImage(List.of(uri), imageParams)),
                        uris -> waitImageLoad(context, Commands.loadImage(uris, imageParams)))) {
                    context.complete(false, "Missing jhv.load.image url.", null);
                }
            } catch (Exception e) {
                context.complete(false, message(e), null);
            }
        });
    }

    private static @Nullable JSONObject imageParams(Message msg) {
        Object value = msg.getParam("imageParams");
        return switch (value) {
            case null -> null;
            case JSONObject jo -> jo;
            case Map<?, ?> map -> new JSONObject(map);
            default -> new JSONObject(value.toString());
        };
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
            AppThread.create(() -> {
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

    private LoadImageHandler() {}
}
