package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.threads.EDTQueue;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.BandDataProvider;
import org.helioviewer.jhv.timelines.band.BandReaderCdf;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

class LoadRequest {

    static void submit(@Nonnull URI uri) {
        EDTCallbackExecutor.pool.submit(new LoadRequestURI(uri), new Callback());
    }

    static void submit(@Nonnull String json) {
        EDTCallbackExecutor.pool.submit(new LoadRequestString(json), new Callback());
    }

    static void submitCDF(@Nonnull List<URI> uriList) {
        EDTCallbackExecutor.pool.submit(new LoadRequestCDF(uriList), new CallbackCDF());
        Timelines.dc.setStatus("Loading...");
    }

    private static void parseRequest(JSONObject jo) throws Exception {
        JSONArray ji = jo.optJSONArray("org.helioviewer.jhv.request.image");
        if (ji != null) {
            int len = ji.length();
            for (int i = 0; i < len; i++) {
                APIRequest req = APIRequest.fromRequestJson(ji.getJSONObject(i));
                ImageLayer layer = EDTQueue.invokeAndWait(() -> ImageLayer.create(null));
                LoadLayer.submit(layer, req);
            }
        }

        JSONArray jt = jo.optJSONArray("org.helioviewer.jhv.request.timeline");
        if (jt != null) {
            int len = jt.length();
            for (int i = 0; i < len; i++) {
                BandDataProvider.loadBand(jt.getJSONObject(i));
            }
        }
    }

    private record LoadRequestURI(URI uri) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            if (uri.toString().toLowerCase().endsWith(".cdf")) {
                BandReaderCdf.load(uri);
            } else
                parseRequest(JSONUtils.get(uri));
            return null;
        }
    }

    private record LoadRequestCDF(List<URI> uriList) implements Callable<Void> {
        @Override
        public Void call() {
            uriList.parallelStream().forEach(uri -> {
                try {
                    BandReaderCdf.load(uri);
                } catch (Exception e) {
                    Log.warn(uri.toString(), e);
                }
            });
            return null;
        }
    }

    private record LoadRequestString(String json) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            parseRequest(new JSONObject(json));
            return null;
        }
    }

    private static class Callback implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred opening the remote file", t.getMessage());
        }

    }

    private static class CallbackCDF implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
            Timelines.dc.setStatus(null);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Timelines.dc.setStatus(null);
            Log.error(t);
            Message.err("An error occurred opening the remote file", t.getMessage());
        }

    }

}
