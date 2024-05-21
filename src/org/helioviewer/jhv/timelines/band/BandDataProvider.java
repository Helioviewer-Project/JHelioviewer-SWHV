package org.helioviewer.jhv.timelines.band;

import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.interval.Interval;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.util.concurrent.FutureCallback;

public class BandDataProvider {

    private static final ArrayListMultimap<Band, Future<List<Band.Data>>> workerMap = ArrayListMultimap.create();

    static void addDownloads(Band band, List<Interval> intervals) {
        BandType type = band.getBandType();
        if ("".equals(type.getBaseURL()))
            return;
        for (Interval interval : intervals) {
            Future<List<Band.Data>> worker = HapiReader.requestData(type.getDataset(), type.getParameter(), interval.start, interval.end);
            workerMap.put(band, worker);
        }
    }

    static void stopDownloads(Band band) {
        workerMap.get(band).forEach(worker -> worker.cancel(true));
        workerMap.removeAll(band);
    }

    static boolean isDownloadActive(Band band) {
        for (Future<?> worker : workerMap.get(band)) {
            if (!worker.isDone())
                return true;
        }
        return false;
    }

}
