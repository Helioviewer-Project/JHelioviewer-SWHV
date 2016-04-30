package org.helioviewer.jhv.plugins.swek.request;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.GZIPUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainerRequestHandler;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.data.datatype.event.SWEKRelatedEvents;
import org.helioviewer.jhv.data.datatype.event.SWEKRelatedOn;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.database.EventDatabase.JsonEvent;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;
import org.helioviewer.jhv.plugins.swek.sources.SWEKSourceManager;

public class IncomingRequestManager implements JHVEventContainerRequestHandler {

    private static IncomingRequestManager instance;

    public static IncomingRequestManager getSingletonInstance() {
        if (instance == null) {
            instance = new IncomingRequestManager();
        }
        return instance;
    }

    @Override
    public void handleRequestForInterval(JHVEventType eventType, Interval interval) {
        SWEKDownloadManager.getSingletonInstance().newRequestForInterval(eventType, interval);
    }

    @Override
    public ArrayList<JHVEvent> getOtherRelations(JHVEvent event) {
        SWEKEventType evt = event.getJHVEventType().getEventType();
        ArrayList<JHVEvent> nEvents = new ArrayList<JHVEvent>();
        ArrayList<JsonEvent> jsonEvents = new ArrayList<JsonEvent>();

        for (SWEKRelatedEvents re : evt.getSwekRelatedEvents()) {
            if (re.getEvent() == evt) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();
                for (SWEKRelatedOn swon : relon) {
                    String f = swon.parameterFrom.getParameterName().toLowerCase();
                    String w = swon.parameterWith.getParameterName().toLowerCase();
                    SWEKEventType reType = re.getRelatedWith();
                    for (SWEKSupplier supplier : reType.getSuppliers()) {
                        JHVEventType othert = JHVEventType.getJHVEventType(reType, supplier);
                        jsonEvents.addAll(EventDatabase.relations2Program(event.getUniqueID(), event.getJHVEventType(), othert, f, w));
                    }
                }
            }
            if (re.getRelatedWith() == evt) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();
                for (SWEKRelatedOn swon : relon) {
                    String f = swon.parameterFrom.getParameterName().toLowerCase();
                    String w = swon.parameterWith.getParameterName().toLowerCase();
                    SWEKEventType reType = re.getEvent();
                    for (SWEKSupplier supplier : reType.getSuppliers()) {
                        JHVEventType fromt = JHVEventType.getJHVEventType(reType, supplier);
                        jsonEvents.addAll(EventDatabase.relations2Program(event.getUniqueID(), fromt, event.getJHVEventType(), f, w));
                    }
                }
            }
            for (JsonEvent jsonEvent : jsonEvents) {
                nEvents.add(parseJSON(jsonEvent, false));
            }
            jsonEvents.clear();
        }

        nEvents.add(parseJSON(EventDatabase.event2Program(event.getUniqueID()), true));
        return nEvents;

    }

    private JHVEvent parseJSON(JsonEvent jsonEvent, boolean full) {
        SWEKParser parser = SWEKSourceManager.getSingletonInstance().getParser(jsonEvent.type.getSupplier().getSource());
        return parser.parseEventJSON(JSONUtils.getJSONStream(GZIPUtils.decompress(jsonEvent.json)), jsonEvent.type, jsonEvent.id, jsonEvent.start, jsonEvent.end, full);
    }

}
