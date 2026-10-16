package org.helioviewer.jhv.io.samp;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.UiThread;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;
import org.json.JSONObject;

final class SampHandlers {

    private static final Map<String, String> harmless = Collections.singletonMap("x-samp.mostly-harmless", "1");

    @FunctionalInterface
    interface CheckedHandler {
        void accept(String senderId, String senderName, Message msg) throws Exception;
    }

    static AbstractMessageHandler create(String type, CheckedHandler consumer) {
        return new AbstractMessageHandler(Collections.singletonMap(type, harmless)) {
            @Nullable
            @Override
            public Map<?, ?> processCall(HubConnection c, String senderId, Message msg) {
                try {
                    String sender = c.getMetadata(senderId).getName();
                    consumer.accept(senderId, sender, msg);
                } catch (Exception e) {
                    Log.warn(type, e);
                }
                return null;
            }
        };
    }

    static AbstractMessageHandler command(String type, Runnable command) {
        return create(type, (senderId, sender, msg) -> UiThread.post(command));
    }

    // Copy only the command's supported parameters before dispatching to the UI thread.
    // SAMP booleans are strings; state files keep their stricter Boolean contract.
    static JSONObject parameters(Message msg, String... keys) {
        JSONObject parameters = new JSONObject();
        for (String key : keys) {
            String value = optionalString(msg, key);
            if (value != null)
                parameters.put(key, "true".equalsIgnoreCase(value) ? Boolean.TRUE :
                        "false".equalsIgnoreCase(value) ? Boolean.FALSE : value);
        }
        return parameters;
    }

    static @Nullable String optionalString(Message msg, String key) {
        Object value = msg.getParam(key);
        return value == null ? null : value.toString();
    }

    private SampHandlers() {}
}
