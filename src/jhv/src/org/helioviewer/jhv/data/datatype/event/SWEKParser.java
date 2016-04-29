package org.helioviewer.jhv.data.datatype.event;

import org.json.JSONException;
import org.json.JSONObject;

public interface SWEKParser {

    public abstract JHVEvent parseEventJSON(JSONObject json, JHVEventType type, int id, long start, long end, boolean full) throws JSONException;

}
