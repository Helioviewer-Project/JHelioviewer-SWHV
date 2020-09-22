package org.helioviewer.jhv.io;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;

import org.helioviewer.jhv.Settings;

public class SampHub {

    static {
        Logger.getLogger("org.astrogrid.samp").setLevel(Level.WARNING);
    }

    private static final boolean startHub = Boolean.parseBoolean(Settings.getProperty("startup.sampHub"));

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

}
