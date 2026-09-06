package org.helioviewer.jhv.event;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.io.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class SWEKHandler {

    public record RemoteEvent(byte[] compressedJson, long start, long end, long archiv, String uid, Map<String, Number> indexedValues) {
        public RemoteEvent {
            indexedValues = Map.copyOf(indexedValues);
        }
    }

    public record RemotePage(boolean overmax, List<RemoteEvent> events, List<SolarEvent.LinkRef> associations) {}

    RemotePage fetchPage(SWEKSupplier supplier, long start, long end, int page) throws Exception {
        JSONObject eventJSON = JSONUtils.get(createURI(supplier, start, end, page));
        return parseRemotePage(eventJSON, supplier);
    }

    protected abstract RemotePage parseRemotePage(JSONObject eventJSON, SWEKSupplier supplier) throws Exception;

    protected abstract URI createURI(SWEKSupplier supplier, long start, long end, int page) throws Exception;

    public abstract SolarEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) throws JSONException;

}
