package org.helioviewer.jhv.data.event;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.io.NetStream;
import org.helioviewer.jhv.log.Log;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class SWEKHandler {

    public boolean remote2db(SWEKSupplier supplier, long start, long end, List<SWEKParam> params) {
        ArrayList<Interval> range = EventDatabase.db2daterange(supplier);
        for (Interval interval : range) {
            if (interval.start <= start && interval.end >= end) {
                return true;
            }
        }

        try {
            int page = 0;
            boolean success = true;
            boolean overmax = true;
            while (overmax && success) {
                try (NetStream ns = new NetStream(createURL(supplier.getGroup(), start, end, params, page))) {
                    JSONObject eventJSON = JSONUtils.getJSONStream(ns.getInput());
                    overmax = eventJSON.optBoolean("overmax", false);
                    success = parseRemote(eventJSON, supplier) && parseAssociations(eventJSON);
                    page++;
                }
            }
            return success;
        } catch (JSONException e) {
            Log.error("JSON parse error: " + e);
        } catch (IOException e) {
            Log.error("Could not create input stream for given URL error: " + e);
        }
        return false;
    }

    protected abstract boolean parseRemote(JSONObject eventJSON, SWEKSupplier supplier);

    protected abstract boolean parseAssociations(JSONObject eventJSON);

    protected abstract String createURL(SWEKGroup group, long start, long end, List<SWEKParam> params, int page);

    public abstract JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException;

}
