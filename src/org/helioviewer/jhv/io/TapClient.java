package org.helioviewer.jhv.io;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

public class TapClient {

    public interface Receiver {
        void setTapResponse(Object o);
    }

    public static void submitQuery(@Nonnull Receiver receiver, @Nonnull String serverUrl, @Nonnull String adql, Function<JSONObject, Object> func) {
        EventQueueCallbackExecutor.pool.submit(new ADQLQuery(serverUrl, adql, func), new Callback(receiver));
    }

    public record ADQLQuery(String serverUrl, String adql, Function<JSONObject, Object> func)
            implements Callable<Object> {
        @Override
        public Object call() throws Exception {
            URI uri = new URI(serverUrl + "/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=" + URLEncoder.encode(adql, StandardCharsets.UTF_8));
            return func.apply(JSONUtils.get(uri));
        }
    }

    private record Callback(Receiver receiver) implements FutureCallback<Object> {

        @Override
        public void onSuccess(Object result) {
            receiver.setTapResponse(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred while querying the server:", t.getMessage(), false);
        }

    }

}
