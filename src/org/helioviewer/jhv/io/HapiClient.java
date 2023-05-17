package org.helioviewer.jhv.io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import org.hapiserver.HapiClientCSVIterator;
import org.hapiserver.HapiRecord;
//import org.hapiserver.TimeUtil;
import com.google.common.util.concurrent.FutureCallback;

public class HapiClient {

    public static void submit() {
        String server = "https://jfaden.net/HapiServerDemo/hapi";
        String id = "Iowa+City+Conditions";
        String parameters = "Temperature,Humidity";
        String startTime = "2019-10-20T00:00";
        String endTime = "2019-10-24T05:00";

        EventQueueCallbackExecutor.pool.submit(new LoadHapi(server, id, parameters, startTime, endTime), new Callback(server));
    }

    private record LoadHapi(String server, String id, String parameters, String startTime,
                            String endTime) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            URI dataURI = new URI(server + "/data?id=" + id + "&parameters=" + parameters + "&time.min=" + startTime + "&time.max=" + endTime);
            try (NetClient nc = NetClient.of(dataURI); InputStream is = nc.getStream()) {
                URI infoURI = new URI(server + "/info?id=" + id + "&parameters=" + parameters);
                Iterator<HapiRecord> it = new HapiClientCSVIterator(JSONUtils.get(infoURI), new BufferedReader(new InputStreamReader(is)));
                while (it.hasNext()) {
                    HapiRecord rec = it.next();
                    System.out.printf("%s %9.3f %9.3f%n", rec.getIsoTime(0), rec.getDouble(1), rec.getDouble(2));
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
