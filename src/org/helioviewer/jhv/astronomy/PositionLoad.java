package org.helioviewer.jhv.astronomy;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

//import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.util.concurrent.FutureCallback;

public record PositionLoad(Interfaces.StatusReceiver receiver, SpaceObject target, boolean isHCI,
                           Future<PositionResponse> future) {

    private record LoadPosition(SpaceObject observer, SpaceObject target, Frame frame, long start,
                                long end) implements Callable<PositionResponse> {

        private static final int MAX_POINTS = 50000;
        private static final UriTemplate template = new UriTemplate("https://swhv.oma.be/position");

        @Override
        public PositionResponse call() throws Exception {
            if (start > end)
                throw new Exception("End before start");

            long dt = 60, span = (end - start) / 1000;
            if (span / dt > MAX_POINTS)
                dt = span / MAX_POINTS;
            long deltat = dt;

            if (observer.isInternal() && target.isInternal()) {
                Position.Cartesian[] p = Spice.getPositionRange(observer.getSpiceName(), target.getSpiceName(), frame.toString(), start, end, deltat);
                if (p != null)
                    return new PositionResponse(p);
            }

            URI uri = new URI(template.expand(UriTemplate.vars()
                    .set("ref", frame)
                    .set("observer", observer.getSpiceName())
                    .set("target", target.getSpiceName())
                    .set("utc", TimeUtils.format(start))
                    .set("utc_end", TimeUtils.format(end))
                    .set("deltat", deltat)));

            //Stopwatch sw = Stopwatch.createStarted();
            try (NetClient nc = NetClient.of(uri, true)) {
                JSONObject result = JSONUtils.get(nc.getReader());
                if (nc.isSuccessful()) {
                    return new PositionResponse(result);
                } else {
                    throw new Exception(result.optString("faultstring", "Invalid network response"));
                }
                //} finally {
                //    System.out.println((sw.elapsed().toNanos() / 1e9));
            }
        }

    }

    private record Callback(Interfaces.StatusReceiver receiver) implements FutureCallback<PositionResponse> {

        @Override
        public void onSuccess(PositionResponse result) {
            receiver.setStatus("Loaded");
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            if (!JHVThread.isInterrupted(t))
                receiver.setStatus(t.getMessage());
        }

    }

    private void stop() {
        future.cancel(true);
        receiver.setStatus(null);
    }

    public boolean isDownloading() {
        return !future.isDone();
    }

    @Nullable
    public PositionResponse getResponse() {
        try {
            return future.isDone() ? future.get() : null;
        } catch (Exception ignore) { // logged to UI by onFailure
        }
        return null;
    }

    private static final ArrayListMultimap<UpdateViewpoint, PositionLoad> loads = ArrayListMultimap.create();

    public static PositionLoad submit(UpdateViewpoint uv, Interfaces.StatusReceiver receiver, SpaceObject observer, SpaceObject target, Frame frame, long start, long end) {
        receiver.setStatus("Loading...");
        PositionLoad load = new PositionLoad(receiver, target, frame == Frame.SOLO_HCI, EDTCallbackExecutor.pool.submit(
                new LoadPosition(observer, target, frame, start, end), new Callback(receiver)));
        loads.put(uv, load);

        return load;
    }

    public static List<PositionLoad> get(UpdateViewpoint uv) {
        return loads.get(uv);
    }

    public static void remove(UpdateViewpoint uv, PositionLoad load) {
        loads.remove(uv, load);
        load.stop();
    }

    public static void removeAll(UpdateViewpoint uv) {
        for (PositionLoad load : loads.removeAll(uv)) {
            load.stop();
        }
    }

}
