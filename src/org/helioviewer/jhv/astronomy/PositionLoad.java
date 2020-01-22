package org.helioviewer.jhv.astronomy;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

//import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;

import spice.basic.Body;

public class PositionLoad {

    private static final int MAX_POINTS = 10000;
    private static final String baseURL = "http://swhv.oma.be/position?";

    private static class LoadPosition implements Callable<PositionResponse> {

        private final SpaceObject observer;
        private final SpaceObject target;
        private final Frame frame;
        private final long start;
        private final long end;

        LoadPosition(SpaceObject _observer, SpaceObject _target, Frame _frame, long _start, long _end) {
            observer = _observer;
            target = _target;
            frame = _frame;
            start = _start;
            end = _end;
        }

        @Override
        public PositionResponse call() throws Exception {
            long dt = 60, span = (end - start) / 1000;
            if (span / dt > MAX_POINTS)
                dt = span / MAX_POINTS;
            long deltat = dt;

            Body observerBody = observer.getBody(), targetBody = target.getBody();
            if (observerBody != null && targetBody != null) {
                PositionCartesian[] p = Spice.getPosition(observerBody, targetBody, frame.referenceFrame, start, end, deltat);
                if (p != null)
                    try {
                        return new PositionResponse(p);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }

            //Stopwatch sw = Stopwatch.createStarted();
            String url = baseURL + "ref=" + frame + "&observer=" + observer.getUrlName() + "&target=" + target.getUrlName() +
                    "&utc=" + TimeUtils.format(start) + "&utc_end=" + TimeUtils.format(end) + "&deltat=" + deltat;
            try (NetClient nc = NetClient.of(url, true)) {
                JSONObject result = JSONUtils.get(nc.getReader());
                if (nc.isSuccessful())
                    return new PositionResponse(result);
                else
                    throw new Exception(result.optString("faultstring", "Invalid network response"));
                //} finally {
                //    System.out.println((sw.elapsed().toNanos() / 1e9));
            }
        }

    }

    private static class Callback implements FutureCallback<PositionResponse> {

        private final PositionReceiver receiver;

        Callback(PositionReceiver _receiver) {
            receiver = _receiver;
        }

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

    private final PositionReceiver receiver;
    private final SpaceObject target;
    private final Future<PositionResponse> future;

    private PositionLoad(PositionReceiver _receiver, SpaceObject _target, Future<PositionResponse> _future) {
        receiver = _receiver;
        target = _target;
        future = _future;
    }

    public static PositionLoad submit(PositionReceiver receiver, SpaceObject observer, SpaceObject target, Frame frame, long start, long end) {
        receiver.setStatus("Loading...");
        return new PositionLoad(receiver, target, EventQueueCallbackExecutor.pool.submit(new LoadPosition(observer, target, frame, start, end), new Callback(receiver)));
    }

    public void stop() {
        future.cancel(true);
        receiver.setStatus(null);
    }

    public boolean isDownloading() {
        return !future.isDone();
    }

    public SpaceObject getTarget() {
        return target;
    }

    @Nullable
    public PositionResponse getResponse() {
        try {
            return future.isDone() ? future.get() : null;
        } catch (Exception e) { // should not happen
            Log.error(e);
        }
        return null;
    }

}
