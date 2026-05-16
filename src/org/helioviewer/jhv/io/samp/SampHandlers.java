package org.helioviewer.jhv.io.samp;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.HubConnection;

final class SampHandlers {

    private static final Map<String, String> harmless = Collections.singletonMap("x-samp.mostly-harmless", "1");

    private SampHandlers() {}

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

    static @Nullable String optionalString(Message msg, String key) {
        Object value = msg.getParam(key);
        return value == null ? null : value.toString();
    }

}
