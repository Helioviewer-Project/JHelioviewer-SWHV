package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.EventDispatchQueue;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.BandDataProvider;
import org.helioviewer.jhv.timelines.band.CDFReader;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

class LoadRequest {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    static void submit(@Nonnull URI uri) {
        EventQueueCallbackExecutor.pool.submit(new LoadRequestURI(uri), new Callback());
    }

    static void submit(@Nonnull String json) {
        EventQueueCallbackExecutor.pool.submit(new LoadRequestString(json), new Callback());
    }

    static void submitCDF(@Nonnull List<URI> uriList) {
        EventQueueCallbackExecutor.pool.submit(new LoadRequestCDF(uriList), new CallbackCDF());
        Timelines.dc.setStatus("Loading...");
    }

    private static void parseRequest(JSONObject jo) throws Exception {
        JSONArray ji = jo.optJSONArray("org.helioviewer.jhv.request.image");
        if (ji != null) {
            int len = ji.length();
            for (int i = 0; i < len; i++) {
                APIRequest req = APIRequest.fromRequestJson(ji.getJSONObject(i));
                ImageLayer layer = EventDispatchQueue.invokeAndWait(() -> ImageLayer.create(null));
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
                CDFReader.load(uri);
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
                    CDFReader.load(uri);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "", e);
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
            LOGGER.log(Level.SEVERE, "An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
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
            LOGGER.log(Level.SEVERE, "An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
