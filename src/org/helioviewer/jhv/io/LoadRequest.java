package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.EDTQueue;
import org.helioviewer.jhv.threads.Tasks;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.BandDataProvider;
import org.helioviewer.jhv.timelines.band.BandReaderCdf;

import org.json.JSONArray;
import org.json.JSONObject;

class LoadRequest {

    static void submit(@Nonnull URI uri) {
        Tasks.submit(uri.toString(), new LoadRequestURI(uri), Tasks::doNothing, "Error getting the data");
    }

    static void submit(@Nonnull String json) {
        Tasks.submit("request", new LoadRequestString(json), Tasks::doNothing, "Error getting the data");
    }

    static void submitCDF(@Nonnull List<URI> uriList) {
        Tasks.submit("cdf", new LoadRequestCDF(uriList), LoadRequest::onSuccessCDF, LoadRequest::onFailureCDF);
        Timelines.dc.setStatus("Loading...");
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

    private record LoadRequestString(String json) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            parseRequest(new JSONObject(json));
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

    private static void onSuccessCDF(Void ignoredResult) {
        Timelines.dc.setStatus(null);
    }

    private static void onFailureCDF(String logContext, Throwable t) {
        Timelines.dc.setStatus(null);
        Log.error(logContext, t);
        Message.err("Error getting the data", t.getMessage());
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

}
