package org.helioviewer.jhv.plugins.swek.sources.fhnw;

import java.net.URI;
import java.util.List;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.SWEK;
import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKHandler;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.io.TapClient.ADQLQuery;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class FHNWHandler extends SWEKHandler {

    private static final String QUERY_URL = "https://tap.cs.technik.fhnw.ch/__system__/tap/run/tap";

    @Override
    public boolean remote2db(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params) {
        String adql = "SELECT TOP 10 * FROM rhessi_flares.epn_core WHERE start_time >= '" +
            "2002-01-01T00:00:00" + //TimeUtils.format(start) + 
            "' AND end_time <= '" + TimeUtils.format(end) + "' ORDER BY start_time";

        ADQLQuery callable = new ADQLQuery(QUERY_URL, adql, FHNWHandler::parseResponse);
        try {
            callable.call();
            return true;
        } catch (Exception e) {
            Log.error(e);
        }
        return false;
    }

    private static JSONObject parseResponse(JSONObject jo) {
        System.out.println(">>> " + jo);
        return jo;
    }

    @Override
    protected boolean parseRemote(JSONObject eventJSON, SWEKSupplier supplier) {
        return false;
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        return false;
    }

    @Override
    protected URI createURI(SWEKGroup group, long start, long end, List<SWEK.Param> params, int page) throws Exception {
        return null;
    }

    @Override
    public JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException {
        return null;
    }

}
