package org.helioviewer.jhv.plugins.samp;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.io.LoadURITask;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;

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

        Metadata meta = new Metadata();
        // TODO: name / description
        meta.setName("JHelioviewer");
        meta.setDescriptionText(JHVGlobals.userAgent);
        declareMetadata(meta);

        addMessageHandler(new AbstractMessageHandler("image.load.fits") {
            @Override
            public Map<?,?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    openURI(msg.getParam("url").toString());
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
                    if ("SSA".equals(c.getMetadata(senderId).getName()))
                        openURI(msg.getParam("url").toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        declareSubscriptions(computeSubscriptions());

        setAutoconnect(10);
    }

    private void openURI(String _uri) throws Exception {
        URI uri = new URI(_uri);
        JHVGlobals.getExecutorService().execute(new LoadURITask(ImageLayer.createImageLayer(null), uri));
    }

    void notifyRequestData() {
        Message msg = new Message(MTYPE_VIEW_DATA);
        // Layers.getSAMPMessage(msg);
        try {
            getConnection().notifyAll(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
