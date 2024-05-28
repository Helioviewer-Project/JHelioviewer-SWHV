package org.helioviewer.jhv.layers.connect;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

public class LoadSunJSON {

    public interface Receiver {
        void setGeometry(List<SunJSON.GeometryCollection> g);
    }

    public static void submit(@Nonnull List<URI> uriList) {
        Receiver receiver = Layers.getConnectionLayer();
        if (receiver != null) // ConnectionLayer() can be null in current releases
            EDTCallbackExecutor.pool.submit(new LoadSunJSONURI(uriList), new Callback(receiver));
    }

    public static void submit(@Nonnull String json) {
        Receiver receiver = Layers.getConnectionLayer();
        if (receiver != null) // ConnectionLayer() can be null in current releases
            EDTCallbackExecutor.pool.submit(new LoadSunJSONString(json), new Callback(receiver));
    }

    private record LoadSunJSONURI(List<URI> uriList) implements Callable<List<SunJSON.GeometryCollection>> {
        @Override
        public List<SunJSON.GeometryCollection> call() {
            return uriList.parallelStream().map(uri -> {
                try {
                    return SunJSON.process(JSONUtils.get(uri));
                } catch (Exception e) {
                    Log.warn(uri.toString(), e);
                    return null;
                }
            }).filter(Objects::nonNull).toList();
        }
    }

    private record LoadSunJSONString(String json) implements Callable<List<SunJSON.GeometryCollection>> {
        @Override
        public List<SunJSON.GeometryCollection> call() {
            return List.of(SunJSON.process(new JSONObject(json)));
        }
    }

    private record Callback(Receiver receiver) implements FutureCallback<List<SunJSON.GeometryCollection>> {
        @Override
        public void onSuccess(List<SunJSON.GeometryCollection> result) {
            receiver.setGeometry(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred opening the remote file", t.getMessage());
        }
    }

}
