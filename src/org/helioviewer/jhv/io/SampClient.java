package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

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

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.layers.ImageLayers;
import org.json.JSONArray;
import org.json.JSONObject;

public class SampClient extends HubConnector {

    static {
        Logger.getLogger("org.astrogrid.samp").setLevel(Level.WARNING); // shut-up SAMP info logs
    }

    private static final String MTYPE_VIEW_DATA = "jhv.vso.load";
    private static final boolean startHub = Boolean.parseBoolean(Settings.getProperty("startup.sampHub"));
    private static final SampClient instance = new SampClient(DefaultClientProfile.getProfile());

    public static void init() {
        if (startHub) {
            new Thread(() -> {
                try {
                    if (Hub.getRunningHubs().length == 0)
                        Hub.runHub(HubServiceMode.NO_GUI);
                } catch (Exception e) {
                    Log.warn(e);
                }
            }).start();
        }
    }

    private static URI toURI(String url) throws Exception {
        URI uri = new URI(url);
        if (uri.getScheme() == null) // assume local file
            uri = Path.of(url).toUri();
        return uri;
    }

    @FunctionalInterface
    private interface CheckedConsumer<T, U> {
        void accept(T t, U u) throws Exception;
    }

    private static class JHVSampHandler extends AbstractMessageHandler {

        private static final Map<String, String> harmless = Collections.singletonMap("x-samp.mostly-harmless", "1"); // allow SAMP messages from web
        private final String type;
        private final CheckedConsumer<String, Message> consumer;

        JHVSampHandler(String _type, CheckedConsumer<String, Message> _consumer) {
            super(Collections.singletonMap(_type, harmless));
            type = _type;
            consumer = _consumer;
        }

        @Nullable
        @Override
        public Map<?, ?> processCall(HubConnection c, String senderId, Message msg) {
            try {
                String sender = c.getMetadata(senderId).getName();
                Log.info("{\"sender\": \"" + sender + "\",\"message\": " + SampUtils.toJson(msg, false));
                consumer.accept(sender, msg);
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

        addMessageHandler(new JHVSampHandler("image.load.fits", (sender, msg) -> {
            Object url = msg.getParam("url");
            if (url != null) {
                URI uri = toURI(url.toString());
                EventQueue.invokeLater(() -> Load.image.get(uri));
            }
        }));
        // load VOTable only from SOAR
        addMessageHandler(new JHVSampHandler("table.load.votable", (sender, msg) -> {
            if ("SolarOrbiterARchive".equals(sender)) {
                Object url = msg.getParam("url");
                if (url != null) {
                    URI uri = toURI(url.toString());
                    SoarClient.submitTable(uri);
                }
            }
        }));
        // lie about support for FITS tables to get SOAR and SSA to send us (compressed) FITS
        addMessageHandler(new JHVSampHandler("table.load.fits", (sender, msg) -> {
            if ("SolarOrbiterARchive".equals(sender) || "SSA".equals(sender)) {
                Object url = msg.getParam("url");
                if (url != null) {
                    URI uri = toURI(url.toString());
                    EventQueue.invokeLater(() -> Load.image.get(uri));
                }
            }
        }));
        // advertise we can load CDF, although we can do only MAG and SWA
        addMessageHandler(new JHVSampHandler("table.load.cdf", (sender, msg) -> {
            Object url = msg.getParam("url");
            if (url != null) {
                URI uri = toURI(url.toString());
                EventQueue.invokeLater(() -> Load.cdf.get(uri));
            }
        }));
        addMessageHandler(new JHVSampHandler("jhv.load.image", (sender, msg) -> {
            JSONObject jo = new JSONObject(SampUtils.toJson(msg.getParams(), false));
            JSONArray ja = jo.optJSONArray("url");
            if (ja == null) {
                URI uri = toURI(jo.optString("url"));
                EventQueue.invokeLater(() -> Load.image.get(uri));
            } else {
                ArrayList<URI> uris = new ArrayList<URI>(ja.length());
                for (Object obj : ja) {
                    uris.add(toURI(obj.toString()));
                }
                EventQueue.invokeLater(() -> Load.Image.getAll(uris));
            }
        }));
        addMessageHandler(inlineHandler("jhv.load.request", Load.request));
        addMessageHandler(inlineHandler("jhv.load.state", Load.state));
        addMessageHandler(inlineHandler("jhv.load.sunjson", Load.sunJSON));
        declareSubscriptions(computeSubscriptions());

        setAutoconnect(10);
    }

    private static AbstractMessageHandler inlineHandler(String type, Load.LoadString loader) {
        return new JHVSampHandler(type, (sender, msg) -> {
            Object url = msg.getParam("url");
            if (url != null) {
                URI uri = toURI(url.toString());
                EventQueue.invokeLater(() -> loader.get(uri));
            } else {
                Object value = msg.getParam("value");
                if (value != null) {
                    String json = value.toString();
                    EventQueue.invokeLater(() -> loader.get(json));
                }
            }
        });
    }

    public static void notifyRequestData() {
        Message msg = new Message(MTYPE_VIEW_DATA);
        ImageLayers.getSAMPMessage(msg);
        try {
            HubConnection c = instance.getConnection();
            if (c != null)
                c.notifyAll(msg);
        } catch (Exception e) {
            Log.warn(e);
        }
    }

}
