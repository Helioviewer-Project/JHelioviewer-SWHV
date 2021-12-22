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
import org.json.JSONArray;

import com.google.common.util.concurrent.FutureCallback;

public class TAPClient {

    public interface Receiver {
        void setTAPResponse(Object o);
    }

    public static void submitQuery(@Nonnull Receiver receiver, @Nonnull String serverUrl, @Nonnull String adql, Function<Object, JSONArray> func) {
        EventQueueCallbackExecutor.pool.submit(new ADQLQuery(serverUrl, adql, func), new Callback(receiver));
    }

    private record ADQLQuery(String serverUrl, String adql,
                             Function<Object, JSONArray> func) implements Callable<Object> {
        @Override
        public JSONArray call() throws Exception {
            URI uri = new URI(serverUrl + "/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=" + URLEncoder.encode(adql, StandardCharsets.UTF_8));
            JSONArray ja = JSONUtils.get(uri).getJSONArray("data");
            return func.apply(ja);
        }
    }

    private record Callback(Receiver receiver) implements FutureCallback<Object> {

        @Override
        public void onSuccess(Object result) {
            receiver.setTAPResponse(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred while querying the server:", t.getMessage(), false);
        }

    }

}
