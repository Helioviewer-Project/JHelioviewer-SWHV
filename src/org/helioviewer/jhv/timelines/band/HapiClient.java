package org.helioviewer.jhv.timelines.band;

import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import com.google.common.util.concurrent.FutureCallback;

public class HapiClient {

    public static void submit() {
        String server = "https://cdaweb.gsfc.nasa.gov/hapi";
        String id = "SOLO_L2_MAG-RTN-NORMAL-1-MINUTE";
        String parameters = "B_RTN";
        String startTime = "2022-01-01T00:00:00";
        String endTime = "2022-01-02T00:00:00";

        EventQueueCallbackExecutor.pool.submit(new LoadHapi(server, id, parameters, startTime, endTime), new Callback(server, new HapiReceiver()));
    }

    private record LoadHapi(String server, String id, String parameters, String startTime,
                            String endTime) implements Callable<DatesValues> {
        @Override
        public DatesValues call() throws Exception {
            URI infoURI = new URI(server + "/info?id=" + id + "&parameters=" + parameters);
            JSONUtils.get(infoURI);

            ArrayList<Long> dates = new ArrayList<>();
            ArrayList<float[]> values = new ArrayList<>();

            URI dataURI = new URI(server + "/data?id=" + id + "&parameters=" + parameters + "&time.min=" + startTime + "&time.max=" + endTime);
            try (NetClient nc = NetClient.of(dataURI); Reader reader = nc.getReader()) {
                for (CSVRecord rec : CSVFormat.DEFAULT.parse(reader)) {
                    long milli = TimeUtils.optParse(rec.get(0), Long.MIN_VALUE);
                    if (milli > TimeUtils.MINIMAL_TIME.milli && milli < TimeUtils.MAXIMAL_TIME.milli) {
                        dates.add(milli);
                        values.add(new float[]{Float.parseFloat(rec.get(1)), Float.parseFloat(rec.get(2)), Float.parseFloat(rec.get(3))});
                    }
                }
            }
            return new DatesValues(dates.stream().mapToLong(i -> i).toArray(), values.toArray(float[][]::new));
        }
    }

    interface Receiver {
        void setHapiResponse(DatesValues dvs);
    }

    private record Callback(String server, Receiver receiver) implements FutureCallback<DatesValues> {

        @Override
        public void onSuccess(DatesValues dvs) {
            receiver.setHapiResponse(dvs);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(server, t);
        }

    }

    private static class HapiReceiver implements Receiver {
        @Override
        public void setHapiResponse(DatesValues dvs) {
            long[] dates = dvs.dates();
            float[][] values = dvs.values();
            for (int i = 0; i < dates.length; i++) {
                System.out.printf("%s %s %s %s%n", TimeUtils.format(dates[i]), values[i][0], values[i][1], values[i][2]);
            }
        }
    }

}
