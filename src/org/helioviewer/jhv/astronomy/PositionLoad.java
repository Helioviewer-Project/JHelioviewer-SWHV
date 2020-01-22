package org.helioviewer.jhv.astronomy;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.CancelTask;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

import spice.basic.Body;

public class PositionLoad extends JHVWorker<PositionResponse, Void> {

    private static final int MAX_POINTS = 10000;
    private static final String baseURL = "http://swhv.oma.be/position?";

    private final PositionReceiver receiver;
    private final SpaceObject observer;
    private final SpaceObject target;
    private final Frame frame;
    private final long start;
    private final long end;
    private final long deltat;

    private PositionResponse response;
    private String report;

    private PositionLoad(PositionReceiver _receiver, SpaceObject _observer, SpaceObject _target, Frame _frame, long _start, long _end) {
        receiver = _receiver;
        observer = _observer;
        target = _target;
        frame = _frame;
        start = _start;
        end = _end;

        long dt = 60, span = (end - start) / 1000;
        if (span / dt > MAX_POINTS)
            dt = span / MAX_POINTS;
        deltat = dt;

        receiver.setStatus("Loading...");
        setThreadName("MAIN--PositionLoad");
    }

    @Nullable
    @Override
    protected PositionResponse backgroundWork() {
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

        System.out.println(">>> " + this);
        try (NetClient nc = NetClient.of(toString(), true)) {
            JSONObject result = JSONUtils.get(nc.getReader());
            if (nc.isSuccessful())
                return new PositionResponse(result);
            else
                report = result.optString("faultstring", "Invalid network response");
        } catch (UnknownHostException e) {
            Log.debug("Unknown host, network down?", e);
        } catch (IOException e) {
            report = "Failed: server error";
        } catch (Exception e) {
            report = e.getMessage();
        }
        return null;
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            receiver.setStatus("Cancelled");
            return;
        }

        if (report != null)
            receiver.setStatus(report);
        else {
            try {
                response = get();
                receiver.setStatus("Loaded");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return baseURL + "ref=" + frame + "&observer=" + observer.getUrlName() + "&target=" + target.getUrlName() +
                "&utc=" + TimeUtils.format(start) + "&utc_end=" + TimeUtils.format(end) + "&deltat=" + deltat;
    }

    public static PositionLoad execute(PositionReceiver _receiver, SpaceObject _observer, SpaceObject _target, Frame _frame, long _start, long _end) {
        PositionLoad load = new PositionLoad(_receiver, _observer, _target, _frame, _start, _end);
        JHVExecutor.cachedPool.execute(load);
        JHVExecutor.reaperPool.schedule(new CancelTask(load), 120, TimeUnit.SECONDS);
        return load;
    }

    public void stop() {
        cancel(true);
        receiver.setStatus(null);
    }

    public boolean isDownloading() {
        return !isDone();
    }

    public SpaceObject getTarget() {
        return target;
    }

    @Nullable
    public PositionResponse getResponse() {
        return response;
    }

}
