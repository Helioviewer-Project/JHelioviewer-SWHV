package org.helioviewer.jhv.io.samp;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.threads.JHVThread;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubProfile;
import org.astrogrid.samp.hub.HubServiceMode;
import org.astrogrid.samp.web.ClientAuthorizers;
import org.astrogrid.samp.web.HubSwingClientAuthorizer;
import org.astrogrid.samp.web.ListMessageRestriction;
import org.astrogrid.samp.web.WebCredentialPresenter;
import org.astrogrid.samp.web.WebHubProfile;
import org.astrogrid.samp.xmlrpc.StandardHubProfile;

public final class SampClient extends HubConnector {

    static {
        Log.setLoggerLevel("org.astrogrid.samp", Level.WARNING); // shut-up SAMP info logs
    }

    private static final SampClient instance = new SampClient(DefaultClientProfile.getProfile());

    public static void init(boolean webProfilePopup) {
        JHVThread.create(() -> {
            if (Boolean.parseBoolean(Settings.getProperty("startup.sampHub"))) {
                try {
                    if (Hub.getRunningHubs().length == 0) {
                        Hub.runHub(HubServiceMode.NO_GUI, hubProfiles(webProfilePopup), null);
                    }
                } catch (Exception e) {
                    Log.warn(e);
                }
            }
        }, "JHV-StartSamp").start();
    }

    private static HubProfile[] hubProfiles(boolean webProfilePopup) throws IOException, SampException {
        return new HubProfile[]{
                new StandardHubProfile(),
                new WebHubProfile(new WebHubProfile.ServerFactory(),
                        webProfilePopup ? new HubSwingClientAuthorizer(null, WebCredentialPresenter.INSTANCE) : ClientAuthorizers.TRUE,
                        ListMessageRestriction.DEFAULT, WebHubProfile.createKeyGenerator(), true)
        };
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

        LoadHandlers.register(this);
        PlaybackHandlers.register(this);
        RecordHandlers.register(this);
        ViewHandlers.register(this);
        CameraHandlers.register(this);

        declareSubscriptions(computeSubscriptions());

        setAutoconnect(10);
    }

    static Commands.OperationContext operationContext(String senderId, Message msg, String mtype, String completionMType) {
        String requestId = SampHandlers.optionalString(msg, "requestId");
        return new Commands.OperationContext(SampClient.class, senderId, requestId, mtype,
                (context, success, message, output) -> EventQueue.invokeLater(() ->
                        notifyCompletion(context, completionMType, success, message, output)));
    }

    private static void notifyCompletion(Commands.OperationContext context, String completionMType,
                                         boolean success, String message, @Nullable String output) {
        Message msg = new Message(completionMType);
        if (context.clientId() != null)
            msg.addParam("clientId", context.clientId());
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

}
