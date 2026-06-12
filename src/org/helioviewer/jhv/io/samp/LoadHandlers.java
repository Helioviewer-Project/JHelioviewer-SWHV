package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.app.Commands;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;

final class LoadHandlers {

    static void register(SampClient client) {
        client.addMessageHandler(singleURIHandler("image.load.fits", Commands::loadImage));
        // load VOTable only from SOAR
        client.addMessageHandler(SampHandlers.create("table.load.votable", (senderId, sender, msg) -> {
            if ("SolarOrbiterARchive".equals(sender))
                loadURI(msg, Commands::loadVOTable);
        }));
        // lie about support for FITS tables to get SOAR and SSA to send us (compressed) FITS
        client.addMessageHandler(SampHandlers.create("table.load.fits", (senderId, sender, msg) -> {
            if ("SolarOrbiterARchive".equals(sender) || "SSA".equals(sender))
                loadURI(msg, Commands::loadImage);
        }));
        // advertise we can load CDF, although we can do only MAG and SWA
        client.addMessageHandler(singleURIHandler("table.load.cdf", Commands::loadCDF));
        client.addMessageHandler(LoadImageHandler.create());
        client.addMessageHandler(uriListHandler("jhv.load.cdf", Commands::loadCDF, Commands::loadCDF));
        // Add handler for the HAPI csv files
        client.addMessageHandler(uriListHandler("jhv.load.hapi", Commands::loadHapi, Commands::loadHapi));
        client.addMessageHandler(uriOrValueHandler("jhv.load.request", Commands::loadRequest, Commands::loadRequest));
        client.addMessageHandler(uriOrValueHandler("jhv.load.sunjson", Commands::loadSunJSON, Commands::loadSunJSON));
        client.addMessageHandler(SampHandlers.create("jhv.load.state", (senderId, sender, msg) -> loadState(msg, senderId)));
    }

    private static AbstractMessageHandler singleURIHandler(String type, Consumer<URI> consumer) {
        return SampHandlers.create(type, (senderId, sender, msg) -> loadURI(msg, consumer));
    }

    private static AbstractMessageHandler uriListHandler(String type, Consumer<URI> singleConsumer, Consumer<List<URI>> listConsumer) {
        return SampHandlers.create(type, (senderId, sender, msg) ->
                loadURIList(msg, singleConsumer, listConsumer));
    }

    private static AbstractMessageHandler uriOrValueHandler(String type, Consumer<URI> uriConsumer, Consumer<String> valueConsumer) {
        return SampHandlers.create(type, (senderId, sender, msg) -> {
            Object url = msg.getParam("url");
            if (url != null) {
                URI uri = toURI(url.toString());
                EventQueue.invokeLater(() -> uriConsumer.accept(uri));
                return;
            }

            String value = SampHandlers.optionalString(msg, "value");
            if (value != null)
                EventQueue.invokeLater(() -> valueConsumer.accept(value));
        });
    }

    private static void loadState(Message msg, String senderId) throws Exception {
        Commands.OperationContext context = SampClient.operationContext(senderId, msg, "jhv.load.state", "jhv.load.state.completed");

        try {
            Object input = msg.getParam("url");
            if (input != null) {
                URI uri = toURI(input.toString());
                EventQueue.invokeLater(() -> Commands.loadState(context, uri));
                return;
            }
            String value = SampHandlers.optionalString(msg, "value");
            if (value != null) {
                EventQueue.invokeLater(() -> Commands.loadState(context, value));
                return;
            }
            Commands.notifyLoadStateFinished(context, false, "Missing jhv.load.state url or value.");
        } catch (Exception e) {
            Log.warn("jhv.load.state", e);
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "State load failed." : e.getMessage();
            Commands.notifyLoadStateFinished(context, false, message);
        }
    }

    private static boolean loadURI(Message msg, Consumer<URI> consumer) throws Exception {
        URI uri = requiredURI(msg);
        if (uri == null)
            return false;
        EventQueue.invokeLater(() -> consumer.accept(uri));
        return true;
    }

    static boolean loadURIList(Message msg, Consumer<URI> singleConsumer, Consumer<List<URI>> listConsumer) throws Exception {
        Object url = msg.getParam("url");
        if (!(url instanceof Iterable<?> urls))
            return loadURI(msg, singleConsumer);

        ArrayList<URI> uris = new ArrayList<>();
        for (Object obj : urls)
            uris.add(toURI(obj.toString()));
        EventQueue.invokeLater(() -> listConsumer.accept(uris));
        return true;
    }

    private static URI toURI(String url) throws Exception {
        URI uri = new URI(url);
        if (uri.getScheme() == null) // assume local file
            uri = Path.of(url).toUri();
        return uri;
    }

    private static URI requiredURI(Message msg) throws Exception {
        Object url = msg.getParam("url");
        if (url == null)
            return null;
        return toURI(url.toString());
    }

    private LoadHandlers() {}
}
