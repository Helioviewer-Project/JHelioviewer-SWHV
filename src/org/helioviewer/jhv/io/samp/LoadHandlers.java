package org.helioviewer.jhv.io.samp;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.thread.UiThread;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;

final class LoadHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(singleURIHandler("image.load.fits", Load::image));
        // load VOTable only from SOAR
        client.addMessageHandler(SampHandlers.create("table.load.votable", (senderId, sender, msg) -> {
            if ("SolarOrbiterARchive".equals(sender))
                loadURI(msg, Load::votable);
        }));
        // lie about support for FITS tables to get SOAR and SSA to send us (compressed) FITS
        client.addMessageHandler(SampHandlers.create("table.load.fits", (senderId, sender, msg) -> {
            if ("SolarOrbiterARchive".equals(sender) || "SSA".equals(sender))
                loadURI(msg, Load::image);
        }));
        // advertise we can load CDF, although we can do only MAG and SWA
        client.addMessageHandler(singleURIHandler("table.load.cdf", Load::cdf));
        client.addMessageHandler(LoadImageHandler.create());
        client.addMessageHandler(uriListHandler("jhv.load.cdf", Load::cdf));
        // Add handler for the HAPI csv files
        client.addMessageHandler(uriListHandler("jhv.load.hapi", Load::hapi));
        client.addMessageHandler(uriOrValueHandler("jhv.load.request", Load::request, Load::request));
        client.addMessageHandler(uriOrValueHandler("jhv.load.sunjson", Load::sunJSON, Load::sunJSON));
        client.addMessageHandler(SampHandlers.create("jhv.load.state", (senderId, sender, msg) -> loadState(msg, senderId)));
    }

    private static AbstractMessageHandler singleURIHandler(String type, Consumer<URI> consumer) {
        return SampHandlers.create(type, (senderId, sender, msg) -> loadURI(msg, consumer));
    }

    private static AbstractMessageHandler uriListHandler(String type, Consumer<List<URI>> consumer) {
        return SampHandlers.create(type, (senderId, sender, msg) -> loadURIList(msg, consumer));
    }

    private static AbstractMessageHandler uriOrValueHandler(String type, Consumer<URI> uriConsumer, Consumer<String> valueConsumer) {
        return SampHandlers.create(type, (senderId, sender, msg) -> {
            Object url = msg.getParam("url");
            if (url != null) {
                URI uri = toURI(url.toString());
                UiThread.post(() -> uriConsumer.accept(uri));
                return;
            }

            String value = SampHandlers.optionalString(msg, "value");
            if (value != null)
                UiThread.post(() -> valueConsumer.accept(value));
        });
    }

    private static void loadState(Message msg, String senderId) throws Exception {
        Commands.Completion completion = SampClient.completion(senderId, msg, "jhv.load.state", "jhv.load.state.completed");

        try {
            Object input = msg.getParam("url");
            if (input != null) {
                URI uri = toURI(input.toString());
                UiThread.post(() -> Load.state(completion, uri));
                return;
            }
            String value = SampHandlers.optionalString(msg, "value");
            if (value != null) {
                UiThread.post(() -> Load.state(completion, value));
                return;
            }
            completion.finished(false, "Missing jhv.load.state url or value.", null);
        } catch (Exception e) {
            Log.warn("jhv.load.state", e);
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "State load failed." : e.getMessage();
            completion.finished(false, message, null);
        }
    }

    private static void loadURI(Message msg, Consumer<URI> consumer) throws Exception {
        Object url = msg.getParam("url");
        if (url == null)
            return;
        URI uri = toURI(url.toString());
        UiThread.post(() -> consumer.accept(uri));
    }

    static boolean loadURIList(Message msg, Consumer<List<URI>> consumer) throws Exception {
        Object url = msg.getParam("url");
        if (url == null)
            return false;

        Iterable<?> urls = url instanceof Iterable<?> items ? items : List.of(url);
        ArrayList<URI> uris = new ArrayList<>();
        for (Object obj : urls)
            uris.add(toURI(obj.toString()));
        UiThread.post(() -> consumer.accept(uris));
        return true;
    }

    private static URI toURI(String url) throws Exception {
        URI uri = new URI(url);
        if (uri.getScheme() == null) // assume local file
            uri = Path.of(url).toUri();
        return uri;
    }

    private LoadHandlers() {}
}
