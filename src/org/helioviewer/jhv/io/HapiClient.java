package org.helioviewer.jhv.io;

import java.io.Reader;
import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
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

        EventQueueCallbackExecutor.pool.submit(new LoadHapi(server, id, parameters, startTime, endTime), new Callback(server));
    }

    private record LoadHapi(String server, String id, String parameters, String startTime,
                            String endTime) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            URI infoURI = new URI(server + "/info?id=" + id + "&parameters=" + parameters);
            JSONUtils.get(infoURI);

            URI dataURI = new URI(server + "/data?id=" + id + "&parameters=" + parameters + "&time.min=" + startTime + "&time.max=" + endTime);
            try (NetClient nc = NetClient.of(dataURI); Reader reader = nc.getReader()) {
                for (CSVRecord rec : CSVFormat.DEFAULT.parse(reader)) {
                    System.out.printf("%s %s %s %s%n", rec.get(0), rec.get(1), rec.get(2), rec.get(3));
                }
            }
            return null;
        }
    }

    private record Callback(String server) implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(server, t);
        }

    }

}
