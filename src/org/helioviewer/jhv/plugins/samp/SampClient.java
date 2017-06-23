package org.helioviewer.jhv.plugins.samp;

import java.io.IOException;

import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import org.helioviewer.jhv.JHVGlobals;

class SampClient extends HubConnector {

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
        declareSubscriptions(computeSubscriptions());

        setAutoconnect(10);
    }

    void notifyRequestData() {
    }

}
