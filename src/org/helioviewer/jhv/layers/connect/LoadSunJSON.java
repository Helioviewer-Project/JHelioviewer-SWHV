package org.helioviewer.jhv.layers.connect;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
//import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.json.JSONObject;

//import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

public class LoadSunJSON {

    public interface Receiver {
        void setGeometry(List<SunJSONTypes.GeometryCollection> g);
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

    private record LoadSunJSONURI(List<URI> uriList) implements Callable<List<SunJSONTypes.GeometryCollection>> {
        @Override
        public List<SunJSONTypes.GeometryCollection> call() {
            return uriList.parallelStream().map(uri -> {
                // return SunOrgJSON.process(JSONUtils.get(uri));
                try (NetClient nc = NetClient.of(uri)) {
                    return SunFastJSON.process(nc.getStream());
                } catch (Exception e) {
                    Log.warn(uri.toString(), e);
                    return null;
                }
            }).filter(Objects::nonNull).toList();
        }
    }

    private record LoadSunJSONString(String json) implements Callable<List<SunJSONTypes.GeometryCollection>> {
        @Override
        public List<SunJSONTypes.GeometryCollection> call() {
            // Stopwatch sw = Stopwatch.createStarted();
            // System.out.println(">>> " + sw.elapsed().toNanos() / 1e9);
            // return List.of(SunOrgJSON.process(new JSONObject(json)));
            return List.of(SunFastJSON.process(json));
        }
    }

    private record Callback(Receiver receiver) implements FutureCallback<List<SunJSONTypes.GeometryCollection>> {
        @Override
        public void onSuccess(List<SunJSONTypes.GeometryCollection> result) {
            receiver.setGeometry(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(Throwables.getStackTraceAsString(t));
            Message.err("An error occurred opening the remote file", t.getMessage());
        }
    }

}
