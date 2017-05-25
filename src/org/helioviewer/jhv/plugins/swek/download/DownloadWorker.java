package org.helioviewer.jhv.plugins.swek.download;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.GZIPUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.cache.JHVEventCache;
import org.helioviewer.jhv.data.event.JHVAssociation;
import org.helioviewer.jhv.data.event.JHVEvent;
import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.data.event.SWEKParser;
import org.helioviewer.jhv.data.event.SWEKSource;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.database.EventDatabase.JsonEvent;

// A download worker will download events for a type of event from a source.
class DownloadWorker implements Runnable {

    private final SWEKSupplier supplier;
    private final List<SWEKParam> params;
    private final Interval requestInterval;

    public DownloadWorker(SWEKSupplier _supplier, Interval _interval, List<SWEKParam> _params) {
        requestInterval = _interval;
        supplier = _supplier;
        params = _params;
    }

    public void stopWorker() {
        //TBD
    }

    @Override
    public void run() {
        SWEKSource swekSource = supplier.getSource();
        boolean success = swekSource.getDownloader().extern2db(supplier, requestInterval.start, requestInterval.end, params);
        if (success) {
            ArrayList<JHVAssociation> associationList = EventDatabase.associations2Program(requestInterval.start, requestInterval.end, supplier);
            EventQueue.invokeLater(() -> {
                for (JHVAssociation assoc : associationList) {
                    JHVEventCache.add(assoc);
                }
            });

            SWEKParser parser = swekSource.getParser();
            ArrayList<JsonEvent> eventList = EventDatabase.events2Program(requestInterval.start, requestInterval.end, supplier, params);
            for (JsonEvent event : eventList) {
                JHVEvent ev = parser.parseEventJSON(JSONUtils.getJSONStream(GZIPUtils.decompress(event.json)), event.type, event.id, event.start, event.end, false);
                EventQueue.invokeLater(() -> JHVEventCache.add(ev));
            }

            EventQueue.invokeLater(() -> {
                JHVEventCache.finishedDownload();
                SWEKDownloadManager.workerFinished(this);
            });
            EventDatabase.addDaterange2db(requestInterval.start, requestInterval.end, supplier);
        } else {
            EventQueue.invokeLater(() -> SWEKDownloadManager.workerForcedToStop(this));
        }
    }

    public SWEKSupplier getSupplier() {
        return supplier;
    }

    public SWEKGroup getGroup() {
        return supplier.getGroup();
    }

    public Interval getRequestInterval() {
        return requestInterval;
    }

}
