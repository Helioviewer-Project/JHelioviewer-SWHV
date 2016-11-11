package org.helioviewer.jhv.plugins.swek.download;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.GZIPUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.database.EventDatabase.JsonEvent;

// A download worker will download events for a type of event from a source.
class DownloadWorker implements Runnable {

    private final JHVEventType jhvType;
    private final List<SWEKParam> params;
    private final Interval requestInterval;

    public DownloadWorker(JHVEventType _jhvType, Interval interval, List<SWEKParam> params) {
        requestInterval = interval;
        jhvType = _jhvType;
        this.params = params;
    }

    public void stopWorker() {
        //TBD
    }

    @Override
    public void run() {
        SWEKSource swekSource = jhvType.getSupplier().getSource();

        boolean success = swekSource.getDownloader().extern2db(jhvType, requestInterval.start, requestInterval.end, params);
        if (success) {
            ArrayList<JHVAssociation> associationList = EventDatabase.associations2Program(requestInterval.start, requestInterval.end, jhvType);
            EventQueue.invokeLater(() -> {
                for (JHVAssociation assoc : associationList) {
                    JHVEventCache.add(assoc);
                }
            });

            SWEKParser parser = swekSource.getParser();
            ArrayList<JsonEvent> eventList = EventDatabase.events2Program(requestInterval.start, requestInterval.end, jhvType, params);
            for (JsonEvent event : eventList) {
                JHVEvent ev = parser.parseEventJSON(JSONUtils.getJSONStream(GZIPUtils.decompress(event.json)), event.type, event.id, event.start, event.end, false);
                EventQueue.invokeLater(() -> JHVEventCache.add(ev));
            }

            EventQueue.invokeLater(() -> {
                JHVEventCache.finishedDownload(false);
                SWEKDownloadManager.getSingletonInstance().workerFinished(DownloadWorker.this);
            });
            EventDatabase.addDaterange2db(requestInterval.start, requestInterval.end, jhvType);
        } else {
            EventQueue.invokeLater(() -> SWEKDownloadManager.getSingletonInstance().workerForcedToStop(DownloadWorker.this));
        }
    }

    public JHVEventType getJHVEventType() {
        return jhvType;
    }

    public SWEKEventType getEventType() {
        return jhvType.getEventType();
    }

    public Interval getRequestInterval() {
        return requestInterval;
    }

}
