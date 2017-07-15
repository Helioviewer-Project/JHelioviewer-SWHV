package org.helioviewer.jhv.plugins.samp;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.renderable.gui.ImageLayers;

class SampClient extends HubConnector {

    private final static String MTYPE_VIEW_DATA = "jhv.vso.load";

    SampClient(ClientProfile _profile) {
        super(_profile);

        Hub[] runningHubs = Hub.getRunningHubs();
        if (runningHubs.length == 0) {
            try {
                Hub.checkExternalHubAvailability();
                Hub.runExternalHub(HubServiceMode.CLIENT_GUI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        HashMap<String, String> meta = new HashMap<>();
        meta.put("samp.name", "JHelioviewer");
        meta.put("samp.description.text", JHVGlobals.userAgent);
        meta.put("samp.icon.url", "http://swhv.oma.be/user_manual/hvImage_160x160.png");
        meta.put("author.mail", JHVGlobals.emailAddress);
        meta.put("author.name", "ESA JHelioviewer Team");
        declareMetadata(Metadata.asMetadata(meta));

        addMessageHandler(new AbstractMessageHandler("image.load.fits") {
            @Override
            public Map<?,?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    Object url = msg.getParam("url");
                    if (url != null)
                        Load.fits.get(new URI(url.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        // lie about support for FITS tables to get SSA to send us FITS
        addMessageHandler(new AbstractMessageHandler("table.load.fits") {
            @Override
            public Map<?,?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    if ("SSA".equals(c.getMetadata(senderId).getName())) {
                        Object url = msg.getParam("url");
                        if (url != null)
                            Load.fits.get(new URI(url.toString()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        addMessageHandler(new AbstractMessageHandler("jhv.load.image") {
            @Override
            public Map<?,?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    Object url = msg.getParam("url");
                    if (url != null)
                        Load.image.get(new URI(url.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        addMessageHandler(new AbstractMessageHandler("jhv.load.request") {
            @Override
            public Map<?,?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    Object url = msg.getParam("url");
                    if (url != null)
                        Load.request.get(new URI(url.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        addMessageHandler(new AbstractMessageHandler("jhv.load.timeline") {
            @Override
            public Map<?,?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    Object url = msg.getParam("url");
                    if (url != null)
                        Load.timeline.get(new URI(url.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        declareSubscriptions(computeSubscriptions());

        setAutoconnect(10);
    }

    void notifyRequestData() {
        Message msg = new Message(MTYPE_VIEW_DATA);
        ImageLayers.getSAMPMessage(msg);
        try {
            HubConnection c = getConnection();
            if (c != null)
                c.notifyAll(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
