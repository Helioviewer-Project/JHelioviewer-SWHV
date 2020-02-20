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
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.layers.ImageLayers;
import org.json.JSONArray;
import org.json.JSONObject;

public class SampClient extends HubConnector {

    static {
        Logger.getLogger("org.astrogrid.samp").setLevel(Level.WARNING);
    }

    private static final String MTYPE_VIEW_DATA = "jhv.vso.load";
    private static final boolean startHub = Boolean.parseBoolean(Settings.getProperty("startup.sampHub"));
    private static final SampClient instance = new SampClient(DefaultClientProfile.getProfile());

    public static void init() {
        if (startHub) {
            new Thread(() -> {
                try {
                    if (Hub.getRunningHubs().length == 0)
                        Hub.runHub(HubServiceMode.CLIENT_GUI);
                } catch (Exception e) {
                    e.printStackTrace();
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

    private SampClient(ClientProfile _profile) {
        super(_profile);

        HashMap<String, String> meta = new HashMap<>();
        meta.put("samp.name", "JHelioviewer");
        meta.put("samp.description.text", JHVGlobals.userAgent);
        meta.put("samp.icon.url", "http://swhv.oma.be/user_manual/hvImage_160x160.png");
        meta.put("samp.documentation.url", JHVGlobals.documentationURL);
        meta.put("author.mail", JHVGlobals.emailAddress);
        meta.put("author.name", "ESA JHelioviewer Team");
        declareMetadata(Metadata.asMetadata(meta));

        // allow samp message from web
        Map<String, String> harmless = Collections.singletonMap("x-samp.mostly-harmless", "1");

        addMessageHandler(new AbstractMessageHandler(Collections.singletonMap("image.load.fits", harmless)) {
            @Nullable
            @Override
            public Map<?, ?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    Object url = msg.getParam("url");
                    if (url != null) {
                        URI uri = toURI(url.toString());
                        EventQueue.invokeLater(() -> Load.fits.get(uri));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        // lie about support for FITS tables to get SSA to send us FITS
        addMessageHandler(new AbstractMessageHandler(Collections.singletonMap("table.load.fits", harmless)) {
            @Nullable
            @Override
            public Map<?, ?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    if ("SSA".equals(c.getMetadata(senderId).getName())) {
                        Object url = msg.getParam("url");
                        if (url != null) {
                            URI uri = toURI(url.toString());
                            EventQueue.invokeLater(() -> Load.fits.get(uri));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        addMessageHandler(new AbstractMessageHandler(Collections.singletonMap("jhv.load.image", harmless)) {
            @Nullable
            @Override
            public Map<?, ?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    JSONObject jo = new JSONObject(SampUtils.toJson(msg.getParams(), false));
                    JSONArray ja = jo.optJSONArray("url");
                    if (ja == null) {
                        URI uri = toURI(jo.optString("url"));
                        EventQueue.invokeLater(() -> Load.image.get(uri));
                    } else {
                        ArrayList<URI> uris = new ArrayList<>(ja.length());
                        for (Object obj : ja) {
                            uris.add(toURI(obj.toString()));
                        }
                        EventQueue.invokeLater(() -> Load.Image.getAll(uris));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        addMessageHandler(new AbstractMessageHandler(Collections.singletonMap("jhv.load.request", harmless)) {
            @Nullable
            @Override
            public Map<?, ?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    Object url = msg.getParam("url");
                    if (url != null) {
                        URI uri = toURI(url.toString());
                        EventQueue.invokeLater(() -> Load.request.get(uri));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        addMessageHandler(new AbstractMessageHandler(Collections.singletonMap("jhv.load.state", harmless)) {
            @Nullable
            @Override
            public Map<?, ?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    Object url = msg.getParam("url");
                    if (url != null) {
                        URI uri = toURI(url.toString());
                        EventQueue.invokeLater(() -> Load.state.get(uri));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        declareSubscriptions(computeSubscriptions());

        setAutoconnect(10);
    }

    public static void notifyRequestData() {
        Message msg = new Message(MTYPE_VIEW_DATA);
        ImageLayers.getSAMPMessage(msg);
        try {
            HubConnection c = instance.getConnection();
            if (c != null)
                c.notifyAll(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
