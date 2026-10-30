package org.helioviewer.jhv.astronomy;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.UriTemplate;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONObject;

public final class PositionLoad {

    private static final int MAX_POINTS = 50000;
    private static final UriTemplate positionTemplate = new UriTemplate("https://swhv.oma.be/position");
    private static final UriTemplate stateTemplate = new UriTemplate("https://swhv.oma.be/state");

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

    private record LoadPosition(SpaceObject observer, SpaceObject target, Frame frame, String abcorr, boolean state, long start,
                                long end) implements Callable<PositionResponse> {
        @Override
        public PositionResponse call() throws Exception {
            if (start > end)
                throw new Exception("End before start");

            long deltat = 60, span = (end - start) / 1000;
            if (span / deltat > MAX_POINTS)
                deltat = span / MAX_POINTS;

            if (observer.isInternal() && target.isInternal()) {
                PositionResponse response = Spice.getRange(observer.getSpiceName(), target.getSpiceName(), frame.toString(), abcorr, state, start, end, deltat);
                if (response != null)
                    return response;
            }

            URI uri = new URI((state ? stateTemplate : positionTemplate).expand(UriTemplate.vars()
                    .set("ref", frame)
                    .set("observer", observer.getSpiceName())
                    .set("target", target.getSpiceName())
                    .set("abcorr", abcorr)
                    .set("utc", TimeUtils.format(start))
                    .set("utc_end", TimeUtils.format(end))
                    .set("deltat", deltat)));

            try (NetClient nc = NetClient.of(uri, true)) {
                JSONObject result = JSONUtils.get(nc.getReader());
                if (nc.isSuccessful()) {
                    return new PositionResponse(result, state);
                } else {
                    throw new Exception(result.optString("faultstring", "Invalid network response"));
                }
            }
        }
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

    public static PositionLoad submit(StatusReceiver receiver, SpaceObject observer, SpaceObject target, Frame frame, String abcorr, long start, long end) {
        return submit(receiver, observer, target, frame, abcorr, false, start, end);
    }

    // geometric state of target relative to the solar system barycentre in J2000
    public static PositionLoad submitBarycentricState(StatusReceiver receiver, SpaceObject target, long start, long end) {
        return submit(receiver, SpaceObject.SSB, target, Frame.J2000, "NONE", true, start, end);
    }

    private static PositionLoad submit(StatusReceiver receiver, SpaceObject observer, SpaceObject target, Frame frame, String abcorr, boolean state, long start, long end) {
        receiver.setStatus("Loading...");

        Future<PositionResponse> future = Task.submitBackground(target.getSpiceName(), new LoadPosition(observer, target, frame, abcorr, state, start, end),
                result -> onSuccess(receiver), (logContext, t) -> onFailure(receiver, t));
        return new PositionLoad(receiver, target, future);
    }

    private static void onSuccess(StatusReceiver receiver) {
        receiver.setStatus("Loaded");
    }

    private static void onFailure(StatusReceiver receiver, Throwable t) {
        if (!AppThread.isInterrupted(t))
            receiver.setStatus(t.getMessage());
    }

    public void cancel() {
        if (future.cancel(true))
            receiver.setStatus(null);
    }
}
