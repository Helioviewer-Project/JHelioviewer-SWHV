package org.helioviewer.jhv.astronomy;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.thread.JHVThread;
import org.helioviewer.jhv.thread.Tasks;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONObject;

public final class PositionLoad {

    private static final int MAX_POINTS = 50000;
    private static final UriTemplate template = new UriTemplate("https://swhv.oma.be/position");

    private final StatusReceiver receiver;
    private final SpaceObject target;
    private final Future<PositionResponse> future;

    public interface StatusReceiver {
        void setStatus(String status);
    }

    private PositionLoad(StatusReceiver _receiver, SpaceObject _target, Future<PositionResponse> _future) {
        receiver = _receiver;
        target = _target;
        future = _future;
    }

    private record LoadPosition(SpaceObject observer, SpaceObject target, Frame frame, long start,
                                long end) implements Callable<PositionResponse> {
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
                    return new PositionResponse(p, target.getSpiceName());
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

    public boolean isDownloading() {
        return !future.isDone();
    }

    public boolean hasFailed() {
        return future.isCancelled() || (future.isDone() && getResponse() == null);
    }

    public SpaceObject target() {
        return target;
    }

    @Nullable
    public PositionResponse getResponse() {
        try {
            return future.isDone() ? future.get() : null;
        } catch (Exception ignore) { // logged to UI by onFailure
        }
        return null;
    }

    public static PositionLoad submit(StatusReceiver receiver, SpaceObject observer, SpaceObject target, Frame frame, long start, long end) {
        receiver.setStatus("Loading...");

        Future<PositionResponse> future = Tasks.submit(target.getSpiceName(), new LoadPosition(observer, target, frame, start, end),
                result -> onSuccess(receiver), (logContext, t) -> onFailure(receiver, t));
        return new PositionLoad(receiver, target, future);
    }

    private static void onSuccess(StatusReceiver receiver) {
        receiver.setStatus("Loaded");
    }

    private static void onFailure(StatusReceiver receiver, Throwable t) {
        if (!JHVThread.isInterrupted(t))
            receiver.setStatus(t.getMessage());
    }

    public void cancel() {
        if (future.cancel(true))
            receiver.setStatus(null);
    }
}
