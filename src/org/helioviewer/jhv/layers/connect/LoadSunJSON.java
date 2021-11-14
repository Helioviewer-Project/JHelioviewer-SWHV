package org.helioviewer.jhv.layers.connect;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

public class LoadSunJSON {

    public static void submit(@Nonnull URI uri) {
        submit(uri, Layers.getConnectionLayer());
    }

    public static void submit(@Nonnull String json) {
        ReceiverSunJSON receiver = Layers.getConnectionLayer();
        if (receiver != null) // ConnectionLayer() can be null in current releases
            EventQueueCallbackExecutor.pool.submit(new LoadSunJSONString(json), new Callback(receiver));
    }

    public static Void submit(@Nonnull URI uri, @Nullable ReceiverSunJSON receiver) {
        if (receiver != null) // ConnectionLayer() can be null in current releases
            EventQueueCallbackExecutor.pool.submit(new LoadSunJSONURI(uri), new Callback(receiver));
        return null;
    }

    private record LoadSunJSONURI(URI uri) implements Callable<SunJSON.GeometryCollection> {
        @Override
        public SunJSON.GeometryCollection call() throws Exception {
            return SunJSON.process(JSONUtils.get(uri));
        }
    }

    private record LoadSunJSONString(String json) implements Callable<SunJSON.GeometryCollection> {
        @Override
        public SunJSON.GeometryCollection call() throws Exception {
            return SunJSON.process(new JSONObject(json));
        }
    }

    private record Callback(ReceiverSunJSON receiver) implements FutureCallback<SunJSON.GeometryCollection> {

        @Override
        public void onSuccess(SunJSON.GeometryCollection result) {
            receiver.setGeometry(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
