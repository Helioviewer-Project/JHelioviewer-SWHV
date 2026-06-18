package org.helioviewer.jhv.event;

import java.net.URI;
import java.util.List;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.io.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class SWEKHandler {

    record RemotePage(List<SWEK.RemoteEvent> events, List<JHVEvent.LinkRef> associations) {}

    interface PageConsumer {
        void accept(RemotePage page);
    }

    boolean fetch(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params, PageConsumer consumer) {
        try {
            int page = 0;
            boolean overmax = true;
            while (overmax) {
                JSONObject eventJSON = JSONUtils.get(createURI(supplier, start, end, params, page));
                overmax = eventJSON.optBoolean("overmax", false);
                consumer.accept(new RemotePage(parseEvents(eventJSON, supplier), parseAssociations(eventJSON)));
                page++;
            }
            return true;
        } catch (Exception e) {
            Log.error("Error loading SWEK", e);
        }
        return false;
    }

    protected abstract List<SWEK.RemoteEvent> parseEvents(JSONObject eventJSON, SWEKSupplier supplier) throws Exception;

    protected abstract List<JHVEvent.LinkRef> parseAssociations(JSONObject eventJSON) throws Exception;

    protected abstract URI createURI(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params, int page) throws Exception;

    public abstract JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException;

}
