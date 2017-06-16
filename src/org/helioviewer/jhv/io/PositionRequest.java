package org.helioviewer.jhv.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class PositionRequest {

    private static final String baseURL = "http://swhv.oma.be/position?";
    private static final String observer = "SUN";

    private final SpaceObject target;
    private final String frame;
    private final long startTime;
    private final long endTime;
    private final long deltat;

    public final URL url;

    public PositionRequest(SpaceObject _target, String _frame, long _startTime, long _endTime, long _deltat) {
        target = _target;
        frame = _frame;
        startTime = _startTime;
        endTime = _endTime;
        deltat = _deltat;

        String req = baseURL + "ref=" + frame + "&kind=latitudinal" + "&observer=" + observer + "&target=" + target.getUrlName() +
                    "&utc=" + TimeUtils.format(startTime) + "&utc_end=" + TimeUtils.format(endTime) + "&deltat=" + deltat;

        URL _url = null;
        try {
            _url = new URL(req);
        } catch (MalformedURLException e) {
            Log.error("Malformed request URL: " + req);
        }
        url = _url;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PositionRequest))
            return false;
        PositionRequest r = (PositionRequest) o;
        return startTime == r.startTime && endTime == r.endTime && deltat == r.deltat && frame.equals(r.frame) && target.equals(r.target);
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    public static Position.L[] parseResponse(JSONObject jo) throws Exception {
        JSONArray res = jo.getJSONArray("result");
        int len = res.length();
        Position.L[] ret = new Position.L[len];

        for (int j = 0; j < len; j++) {
            JSONObject posObject = res.getJSONObject(j);
            Iterator<String> iterKeys = posObject.keys();
            if (!iterKeys.hasNext())
                throw new Exception("unexpected format");

            String dateString = iterKeys.next();
            JSONArray posArray = posObject.getJSONArray(dateString);

            double rad = posArray.getDouble(0) * (1000. / Sun.RadiusMeter);
            double lon = posArray.getDouble(1);
            double lat = posArray.getDouble(2);
            ret[j] = new Position.L(new JHVDate(dateString), rad, lon, lat);
        }
        return ret;
    }

}
