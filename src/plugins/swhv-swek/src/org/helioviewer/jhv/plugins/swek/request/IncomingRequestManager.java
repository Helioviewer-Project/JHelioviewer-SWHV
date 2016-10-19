package org.helioviewer.jhv.plugins.swek.request;

import org.helioviewer.jhv.base.GZIPUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventCacheRequestHandler;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.database.EventDatabase.JsonEvent;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;

public class IncomingRequestManager implements JHVEventCacheRequestHandler {

    private static final IncomingRequestManager instance = new IncomingRequestManager();

    private IncomingRequestManager() {
    }

    public static IncomingRequestManager getSingletonInstance() {
        return instance;
    }

    @Override
    public void handleRequestForInterval(JHVEventType eventType, Interval interval) {
        SWEKDownloadManager.getSingletonInstance().newRequestForInterval(eventType, interval);
    }

    private JHVEvent parseJSON(JsonEvent jsonEvent, boolean full) {
        SWEKParser parser = jsonEvent.type.getSupplier().getSource().getParser();
        return parser.parseEventJSON(JSONUtils.getJSONStream(GZIPUtils.decompress(jsonEvent.json)), jsonEvent.type, jsonEvent.id, jsonEvent.start, jsonEvent.end, full);
    }

}
