package org.helioviewer.jhv.event;

import java.net.URI;
import java.util.List;

import org.helioviewer.jhv.io.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class SWEKHandler {

    record RemotePage(boolean overmax, List<SWEK.RemoteEvent> events, List<JHVEvent.LinkRef> associations) {}

    RemotePage fetchPage(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params, int page) throws Exception {
        JSONObject eventJSON = JSONUtils.get(createURI(supplier, start, end, params, page));
        return new RemotePage(eventJSON.optBoolean("overmax", false), parseEvents(eventJSON, supplier), parseAssociations(eventJSON, supplier));
    }

    protected abstract List<SWEK.RemoteEvent> parseEvents(JSONObject eventJSON, SWEKSupplier supplier) throws Exception;

    protected abstract List<JHVEvent.LinkRef> parseAssociations(JSONObject eventJSON, SWEKSupplier supplier) throws Exception;

    protected abstract URI createURI(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params, int page) throws Exception;

    public abstract JHVEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException;

}
