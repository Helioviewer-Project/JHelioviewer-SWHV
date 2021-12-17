package org.helioviewer.jhv.events;

import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.io.JSONUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SWEKHandler {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public boolean remote2db(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params) {
        for (Interval interval : EventDatabase.db2daterange(supplier)) {
            if (interval.start <= start && interval.end >= end) {
                return true;
            }
        }

        try {
            int page = 0;
            boolean success = true;
            boolean overmax = true;
            while (overmax && success) {
                JSONObject eventJSON = JSONUtils.get(createURL(supplier.getGroup(), start, end, params, page));
                overmax = eventJSON.optBoolean("overmax", false);
                success = parseRemote(eventJSON, supplier) && parseAssociations(eventJSON);
                page++;
            }
            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading SWEK", e);
        }
        return false;
    }

    protected abstract boolean parseRemote(JSONObject eventJSON, SWEKSupplier supplier);

    protected abstract boolean parseAssociations(JSONObject eventJSON);

    protected abstract String createURL(SWEKGroup group, long start, long end, List<SWEK.Param> params, int page);

    public abstract JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException;

}
