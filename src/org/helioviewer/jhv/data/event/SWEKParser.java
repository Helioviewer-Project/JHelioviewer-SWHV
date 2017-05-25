package org.helioviewer.jhv.data.event;

import org.json.JSONException;
import org.json.JSONObject;

public interface SWEKParser {

    JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException;

}
