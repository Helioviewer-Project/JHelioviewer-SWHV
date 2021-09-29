package org.helioviewer.jhv.events;

import java.awt.EventQueue;
import java.util.List;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.database.EventDatabase;

class SWEKDownloadWorker implements Runnable {

    private final SWEKSupplier supplier;
    private final long start;
    private final long end;
    private final List<SWEKParam> params;

    SWEKDownloadWorker(SWEKSupplier _supplier, long _start, long _end, List<SWEKParam> _params) {
        supplier = _supplier;
        start = _start;
        end = _end;
        params = _params;
    }

    void stopWorker() {
        //TBD
    }

    @Override
    public void run() {
        boolean success = supplier.getSource().getHandler().remote2db(supplier, start, end, params);
        if (success) {
            List<Pair<Integer, Integer>> assocList = EventDatabase.associations2Program(start, end, supplier);
            List<JHVEvent> eventList = EventDatabase.events2Program(start, end, supplier, params);
            EventQueue.invokeLater(() -> {
                assocList.forEach(JHVEventCache::addAssociation);
                eventList.forEach(JHVEventCache::addEvent);
                JHVEventCache.fireEventCacheChanged();

                SWEKDownloadManager.workerFinished(supplier, this);
            });
            EventDatabase.addDaterange2db(start, end, supplier);
        } else {
            EventQueue.invokeLater(() -> SWEKDownloadManager.workerForcedToStop(supplier, this));
        }
    }

    long getStart() {
        return start;
    }

    long getEnd() {
        return end;
    }

}
