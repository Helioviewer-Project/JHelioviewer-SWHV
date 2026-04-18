package org.helioviewer.jhv.io.samp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.threads.JHVThread;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;

public final class SampClient extends HubConnector {

    static {
        Logger.getLogger("org.astrogrid.samp").setLevel(Level.WARNING); // shut-up SAMP info logs
    }

    private static SampClient instance; // keep instance built at startup
    private static final Commands.CompletionListener completionListener = new Commands.CompletionListener() {
        @Override
        public void loadStateFinished(@Nullable Commands.OperationContext context, boolean success, String message) {
            if (context == null || context.owner() != SampClient.class || context.clientId() == null)
                return;
            if (!"jhv.load.state".equals(context.mtype()))
                return;
            notifyCompletion(context, "jhv.load.state.completed", success, message, null);
        }

        @Override
        public void recordingFinished(@Nullable Commands.OperationContext context, boolean success, String message,
                                      @Nullable String output) {
            if (context == null || context.owner() != SampClient.class || context.clientId() == null)
                return;
            if (!"jhv.record.start".equals(context.mtype()))
                return;
            notifyCompletion(context, "jhv.record.start.completed", success, message, output);
        }
    };

    public static void init() {
        JHVThread.create(() -> {
            if (Boolean.parseBoolean(Settings.getProperty("startup.sampHub"))) {
                try {
                    if (Hub.getRunningHubs().length == 0) {
                        Hub.runHub(HubServiceMode.NO_GUI);
                    }
                } catch (Exception e) {
                    Log.warn(e);
                }
            }
            instance = new SampClient(DefaultClientProfile.getProfile());
        }, "JHV-StartSamp").start();
    }

    @FunctionalInterface
    interface CheckedHandler {
        void accept(String senderId, String senderName, Message msg) throws Exception;
    }

    static class JHVSampHandler extends AbstractMessageHandler {

        private static final Map<String, String> harmless = Collections.singletonMap("x-samp.mostly-harmless", "1"); // allow SAMP messages from web
        private final String type;
        private final CheckedHandler consumer;

        JHVSampHandler(String _type, CheckedHandler _consumer) {
            super(Collections.singletonMap(_type, harmless));
            type = _type;
            consumer = _consumer;
        }

        @Nullable
        @Override
        public Map<?, ?> processCall(HubConnection c, String senderId, Message msg) {
            try {
                String sender = c.getMetadata(senderId).getName();
                // Log.info("{\"sender\": \"" + sender + "\",\"message\": " + SampUtils.toJson(msg, false));
                consumer.accept(senderId, sender, msg);
            } catch (Exception e) {
                Log.warn(type, e);
            }
            return null;
        }

    }

    private SampClient(ClientProfile _profile) {
        super(_profile);

        Map<String, String> meta = new HashMap<>();
        meta.put("samp.name", "JHelioviewer");
        meta.put("samp.description.text", JHVGlobals.userAgent);
        meta.put("samp.icon.url", "https://swhv.oma.be/user_manual/hvImage_160x160.png");
        meta.put("samp.documentation.url", JHVGlobals.documentationURL);
        meta.put("author.mail", JHVGlobals.emailAddress);
        meta.put("author.name", "ESA JHelioviewer Team");
        declareMetadata(Metadata.asMetadata(meta));

        registerLoadHandlers();
        registerPlaybackHandlers();
        registerRecordingHandlers();
        declareSubscriptions(computeSubscriptions());
        Commands.addCompletionListener(completionListener);

        setAutoconnect(10);
    }

    private void registerLoadHandlers() {
        addMessageHandler(SampLoadHandlers.uriHandler("image.load.fits", Commands.LOAD_IMAGE));
        addMessageHandler(SampLoadHandlers.votableHandler());
        addMessageHandler(SampLoadHandlers.fitsTableHandler());
        // advertise we can load CDF, although we can do only MAG and SWA
        addMessageHandler(SampLoadHandlers.uriHandler("table.load.cdf", Commands.LOAD_CDF));
        addMessageHandler(SampLoadHandlers.uriListHandler("jhv.load.image", Commands.LOAD_IMAGE));
        // Add handler for the HAPI csv files
        addMessageHandler(SampLoadHandlers.uriListHandler("jhv.load.hapi", Commands.LOAD_HAPI));
        addMessageHandler(SampLoadHandlers.uriOrValueHandler("jhv.load.request", Commands.LOAD_REQUEST));
        addMessageHandler(SampLoadHandlers.uriOrValueHandler("jhv.load.sunjson", Commands.LOAD_SUN_JSON));
        addMessageHandler(new JHVSampHandler("jhv.load.state", (senderId, sender, msg) -> SampLoadHandlers.loadState(msg, senderId)));
    }

    private void registerPlaybackHandlers() {
        addMessageHandler(SampPlaybackHandlers.setPlaybackHandler());
        addMessageHandler(SampPlaybackHandlers.commandHandler("jhv.play", Commands::play));
        addMessageHandler(SampPlaybackHandlers.commandHandler("jhv.pause", Commands::pause));
        addMessageHandler(SampPlaybackHandlers.commandHandler("jhv.toggle.playback", Commands::togglePlayback));
        addMessageHandler(SampPlaybackHandlers.commandHandler("jhv.next.frame", Commands::nextFrame));
        addMessageHandler(SampPlaybackHandlers.commandHandler("jhv.previous.frame", Commands::previousFrame));
    }

    private void registerRecordingHandlers() {
        addMessageHandler(SampRecordingHandlers.startHandler());
        addMessageHandler(SampRecordingHandlers.stopHandler());
    }

    private static void notifyCompletion(Commands.OperationContext context, String completionMType,
                                         boolean success, String message, @Nullable String output) {
        if (instance == null)
            return;
        Message msg = new Message(completionMType);
        if (context.requestId() != null)
            msg.addParam("requestId", context.requestId());
        if (context.mtype() != null)
            msg.addParam("mtype", context.mtype());
        msg.addParam("status", success ? "success" : "failure");
        msg.addParam("message", message);
        if (output != null)
            msg.addParam("output", output);
        try {
            HubConnection c = instance.getConnection();
            if (c != null && context.clientId() != null)
                c.notify(context.clientId(), msg);
        } catch (Exception e) {
            Log.warn(e);
        }
    }

    public static void notifyRequestData() {
        if (instance == null) {
            Log.warn("SAMP client not initialized yet");
            return;
        }

        Message msg = new Message("jhv.vso.load");
        ImageLayers.getSAMPMessage(msg);
        try {
            HubConnection c = instance.getConnection();
            if (c != null)
                c.notifyAll(msg);
        } catch (Exception e) {
            Log.warn(e);
        }
    }

    static @Nullable String optionalString(Message msg, String key) {
        Object value = msg.getParam(key);
        return value == null ? null : value.toString();
    }

}
