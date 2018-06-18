package org.helioviewer.jhv.position;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

public class LoadPosition extends JHVWorker<PositionResponse, Void> {

    private static final int MAX_POINTS = 10000;
    private static final String baseURL = "http://swhv.oma.be/position?";
    private static final String observer = "SUN";

    private final LoadPositionFire receiver;
    private final SpaceObject target;
    private final Frame frame;
    private final long start;
    private final long end;

    private PositionResponse response;
    private String report;

    public LoadPosition(LoadPositionFire _receiver, SpaceObject _target, Frame _frame, long _start, long _end) {
        receiver = _receiver;
        target = _target;
        frame = _frame;
        start = _start;
        end = _end;
        receiver.fireLoaded("Loading...");
        setThreadName("MAIN--PositionLoad");
    }

    @Nullable
    @Override
    protected PositionResponse backgroundWork() {
        long deltat = 60, span = (end - start) / 1000;
        if (span / deltat > MAX_POINTS)
            deltat = span / MAX_POINTS;

        try (NetClient nc = NetClient.of(getURL(target, frame, start, end, deltat), true)) {
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
            receiver.fireLoaded("Cancelled");
            return;
        }

        if (report != null)
            receiver.fireLoaded(report);
        else {
            try {
                response = get();
                receiver.fireLoaded("Loaded");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String getURL(SpaceObject _target, Frame _frame, long _start, long _end, long _deltat) {
        return baseURL + "ref=" + _frame + "&observer=" + observer + "&target=" + _target.getUrlName() +
               "&utc=" + TimeUtils.format(_start) + "&utc_end=" + TimeUtils.format(_end) + "&deltat=" + _deltat;
    }

    public SpaceObject getTarget() {
        return target;
    }

    @Nullable
    public PositionResponse getResponse() {
        return response;
    }

}
