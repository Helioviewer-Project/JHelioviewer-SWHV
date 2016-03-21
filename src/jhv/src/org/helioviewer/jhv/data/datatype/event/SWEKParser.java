package org.helioviewer.jhv.data.datatype.event;

import org.json.JSONException;

public interface SWEKParser {

    public abstract boolean parseEventJSON(String json, JHVEventType type, int id, long start, long end) throws JSONException;

}
