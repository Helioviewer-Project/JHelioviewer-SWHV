package org.helioviewer.jhv.data.event;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.cache.JHVEventCache;
import org.helioviewer.jhv.database.EventDatabase;

class SWEKDownloadWorker implements Runnable {

    private final SWEKSupplier supplier;
    private final List<SWEKParam> params;
    private final Interval requestInterval;

    SWEKDownloadWorker(SWEKSupplier _supplier, Interval _interval, List<SWEKParam> _params) {
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
            ArrayList<JHVAssociation> assocList = EventDatabase.associations2Program(requestInterval.start, requestInterval.end, supplier);
            ArrayList<JHVEvent> eventList = EventDatabase.events2Program(requestInterval.start, requestInterval.end, supplier, params);
            EventQueue.invokeLater(() -> {
                for (JHVAssociation assoc : assocList)
                    JHVEventCache.add(assoc);
                for (JHVEvent event : eventList)
                    JHVEventCache.add(event);
                JHVEventCache.fireEventCacheChanged();
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

    public Interval getRequestInterval() {
        return requestInterval;
    }

}
