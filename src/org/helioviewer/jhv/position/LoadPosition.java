package org.helioviewer.jhv.position;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.CancelTask;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

public class LoadPosition extends JHVWorker<PositionResponse, Void> {

    public enum AberationCorrection {
        NONE("NONE"), LT("LT"), LTS("LT%2BS"), CN("CN"), CNS("CN%2BS"), XLT("XLT"), XLTS("XLT%2BS"), XCN("XCN"), XCNS("XCN%2BS");

        private final String abcorr;

        AberationCorrection(String _abcorr) {
            abcorr = _abcorr;
        }

        @Override
        public String toString() {
            return abcorr;
        }

    }

    static String toUrl(SpaceObject _observer, SpaceObject _target, Frame _frame, long _start, long _end) {
        long dt = 60, span = (_end - _start) / 1000;
        if (span / dt > MAX_POINTS)
            dt = span / MAX_POINTS;
        return baseURL + "ref=" + _frame + "&observer=" + _observer.getUrlName() + "&target=" + _target.getUrlName() +
              "&utc=" + TimeUtils.format(_start) + "&utc_end=" + TimeUtils.format(_end) + "&deltat=" + dt;

    }

    private static final int MAX_POINTS = 10000;
    private static final String baseURL = "http://swhv.oma.be/position?";

    private final StatusReceiver receiver;
    private final SpaceObject target;
    private final String url;

    private PositionResponse response;
    private String report;

    LoadPosition(StatusReceiver _receiver, SpaceObject _target, String _url) {
        receiver = _receiver;
        target = _target;
        url = _url;

        receiver.setStatus("Loading...");
        setThreadName("MAIN--PositionLoad");
    }

    @Nullable
    @Override
    protected PositionResponse backgroundWork() {
        try (NetClient nc = NetClient.of(url, true)) {
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
        return url;
    }

    public void stop() {
        cancel(true);
        receiver.setStatus(null);
    }

    public boolean isDownloading() {
        return !isDone();
    }

    boolean isFailed() {
        return response == null && isDone();
    }

    public SpaceObject getTarget() {
        return target;
    }

    @Nullable
    public PositionResponse getResponse() {
        return response;
    }

}
