package org.helioviewer.jhv.io.samp;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.io.Load;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.json.JSONObject;

final class LoadImageHandler {

    static AbstractMessageHandler create() {
        return SampHandlers.create("jhv.load.image", (senderId, sender, msg) -> {
            Commands.Completion completion = SampClient.completion(senderId, msg, "jhv.load.image", "jhv.load.image.completed");
            try {
                JSONObject imageParams = imageParams(msg);
                if (!LoadHandlers.loadURIList(msg,
                        uris -> waitImageLoad(completion, Load.image(uris, imageParams)))) {
                    completion.finished(false, "Missing jhv.load.image url.", null);
                }
            } catch (Exception e) {
                completion.finished(false, message(e), null);
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

    private static void waitImageLoad(Commands.Completion completion, CompletableFuture<Boolean> future) {
        future.thenAccept(success -> completion.finished(success, success ? "Image loaded." : "Image load failed.", null));
    }

    private static String message(Throwable t) {
        return t.getMessage() == null || t.getMessage().isBlank() ? "Image load failed." : t.getMessage();
    }

    private LoadImageHandler() {}
}
