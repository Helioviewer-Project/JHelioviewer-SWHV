package org.helioviewer.jhv.data.event;

import org.json.JSONException;
import org.json.JSONObject;
import org.jetbrains.annotations.NotNull;

public interface SWEKParser {

    @NotNull JHVEvent parseEventJSON(@NotNull JSONObject json, @NotNull JHVEventType type, int id, long start, long end, boolean full) throws JSONException;

}
