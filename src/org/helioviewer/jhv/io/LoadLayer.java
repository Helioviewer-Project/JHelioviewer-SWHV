package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KView;
import org.helioviewer.jhv.view.uri.URIView;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.threads.JHVThread;

import com.google.common.util.concurrent.FutureCallback;

public class LoadLayer {

    public static Future<View> submit(@Nonnull ImageLayer layer, @Nonnull APIRequest req) {
        return EventQueueCallbackExecutor.pool.submit(new LoadRemote(layer, req), new Callback(layer));
    }

    public static void submit(@Nonnull ImageLayer layer, @Nonnull List<URI> uriList, boolean forceFITS) {
        EventQueueCallbackExecutor.pool.submit(new LoadURIImage(layer, uriList, forceFITS), new Callback(layer));
    }

    private record LoadRemote(ImageLayer layer, APIRequest req) implements Callable<View> {
        @Override
        public View call() throws Exception {
            URI uri = APIResponse.get(req.toJpipRequest());
            return uri == null ? null : loadView(layer.getExecutor(), req, uri, false);
        }
    }

    private record LoadURIImage(ImageLayer layer, List<URI> uriList, boolean forceFITS) implements Callable<View> {
        @Override
        public View call() throws Exception {
            DecodeExecutor executor = layer.getExecutor();
            if (uriList.size() == 1) {
                return loadView(executor, null, uriList.get(0), forceFITS);
            } else {
                List<View> views = uriList.parallelStream().map(uri -> {
                    try {
                        return loadView(executor, null, uri, forceFITS);
                    } catch (Exception e) {
                        Log.warn(uri.toString(), e);
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                return new ManyView(views);
            }
        }
    }

    private record Callback(ImageLayer layer) implements FutureCallback<View> {

        @Override
        public void onSuccess(View result) {
            if (result != null) // LoadRemote can return null
                layer.setView(result);
            else
                layer.unload();
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            if (JHVThread.isInterrupted(t)) { // ignore
                Log.warn(t);
                return;
            }

            layer.unload();

            Log.error(t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

    @Nonnull
    private static View loadView(DecodeExecutor executor, APIRequest req, URI uri, boolean forceFITS) throws Exception {
        String loc = uri.toString().toLowerCase(Locale.ENGLISH);
        if (forceFITS || loc.endsWith(".fits") || loc.endsWith(".fts") || loc.endsWith(".fits.gz")) {
            return new URIView(executor, req, uri, URIView.URIType.FITS);
        } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
            return new URIView(executor, req, uri, URIView.URIType.GENERIC);
        } else {
            return new J2KView(executor, req, uri);
        }
    }

}
