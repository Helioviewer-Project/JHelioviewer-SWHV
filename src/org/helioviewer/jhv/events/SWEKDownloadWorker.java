package org.helioviewer.jhv.events;

import java.awt.EventQueue;
import java.util.List;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.database.EventDatabase;

record SWEKDownloadWorker(SWEKSupplier supplier, long start, long end, List<SWEK.Param> params) implements Runnable {

    @Override
    public void run() {
        boolean success = supplier.getSource().handler().remote2db(supplier, start, end, params);
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

    void stopWorker() {
        //TBD
    }

}
