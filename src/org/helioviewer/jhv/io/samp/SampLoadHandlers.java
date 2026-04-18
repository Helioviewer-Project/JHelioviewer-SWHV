package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.Log;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;

final class SampLoadHandlers {

    private SampLoadHandlers() {
    }

    static AbstractMessageHandler uriHandler(String type, String commandId) {
        return new SampClient.JHVSampHandler(type, (senderId, sender, msg) -> loadURI(msg, commandId));
    }

    // load VOTable only from SOAR
    static AbstractMessageHandler votableHandler() {
        return new SampClient.JHVSampHandler("table.load.votable", (senderId, sender, msg) -> {
            if ("SolarOrbiterARchive".equals(sender))
                loadURI(msg, Commands.LOAD_VOTABLE);
        });
    }

    // lie about support for FITS tables to get SOAR and SSA to send us (compressed) FITS
    static AbstractMessageHandler fitsTableHandler() {
        return new SampClient.JHVSampHandler("table.load.fits", (senderId, sender, msg) -> {
            if ("SolarOrbiterARchive".equals(sender) || "SSA".equals(sender))
                loadURI(msg, Commands.LOAD_IMAGE);
        });
    }

    static AbstractMessageHandler uriListHandler(String type, String commandId) {
        return new SampClient.JHVSampHandler(type, (senderId, sender, msg) -> loadURIList(msg, commandId));
    }

    static AbstractMessageHandler uriOrValueHandler(String type, String commandId) {
        return new SampClient.JHVSampHandler(type, (senderId, sender, msg) -> {
            if (!loadURI(msg, commandId)) {
                String value = SampClient.optionalString(msg, "value");
                if (value != null)
                    invokeCommand(commandId, value);
            }
        });
    }

    static void loadState(Message msg, String senderId) throws Exception {
        String requestId = SampClient.optionalString(msg, "requestId");
        Commands.OperationContext context =
                new Commands.OperationContext(SampClient.class, senderId, requestId, "jhv.load.state");
        Object input = msg.getParam("url");
        if (input != null) {
            invokeLoadState(context, toURI(input.toString()));
            return;
        }
        String value = SampClient.optionalString(msg, "value");
        if (value != null)
            invokeLoadState(context, value);
    }

    private static void invokeLoadState(Commands.OperationContext context, Object input) {
        EventQueue.invokeLater(() -> {
            try {
                if (input instanceof URI uri)
                    Commands.loadState(context, uri);
                else
                    Commands.loadState(context, input.toString());
            } catch (Exception e) {
                Log.warn(Commands.LOAD_STATE, e);
            }
        });
    }

    static boolean loadURI(Message msg, String commandId) throws Exception {
        Object url = msg.getParam("url");
        if (url == null)
            return false;
        URI uri = toURI(url.toString());
        invokeCommand(commandId, uri);
        return true;
    }

    private static void loadURIList(Message msg, String commandId) throws Exception {
        Object url = msg.getParam("url");
        if (!(url instanceof Iterable<?> urls)) {
            loadURI(msg, commandId);
            return;
        }

        ArrayList<URI> uris = new ArrayList<>();
        for (Object obj : urls)
            uris.add(toURI(obj.toString()));
        invokeCommand(commandId, uris);
    }

    private static URI toURI(String url) throws Exception {
        URI uri = new URI(url);
        if (uri.getScheme() == null) // assume local file
            uri = Path.of(url).toUri();
        return uri;
    }

    private static void invokeCommand(String commandId, @Nullable Object input) {
        EventQueue.invokeLater(() -> {
            try {
                Commands.Registry.run(commandId, input);
            } catch (Exception e) {
                Log.warn(commandId, e);
            }
        });
    }
}
