package org.helioviewer.jhv.layers.connect;

import java.io.BufferedReader;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.PositionCartesian;
import org.helioviewer.jhv.astronomy.PositionMapReceiver;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.helioviewer.jhv.time.TimeUtils;

import com.google.common.util.concurrent.FutureCallback;

public class LoadFootpoint implements Callable<TimeMap<PositionCartesian>> {

    private static final DateTimeFormatter euroTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void submit(@Nonnull URI uri, PositionMapReceiver receiver) {
        EventQueueCallbackExecutor.pool.submit(new LoadFootpoint(uri), new Callback(receiver));
    }

    private final URI uri;

    private LoadFootpoint(URI _uri) {
        uri = _uri;
    }

    private static long parseTime(String s) {
        try {
            return TimeUtils.parse(TimeUtils.sqlTimeFormatter, s);
        } catch (Exception e) {
            return TimeUtils.parse(euroTimeFormatter, s);
        }
    }

    @Override
    public TimeMap<PositionCartesian> call() throws Exception {
        TimeMap<PositionCartesian> positionMap = new TimeMap<>();

        try (NetClient nc = NetClient.of(uri); BufferedReader br = new BufferedReader(nc.getReader())) {
            String line = br.readLine(); // skip first line
            while ((line = br.readLine()) != null) {
                String[] values = Regex.Comma.split(line);
                if (values.length > 8) {
                    try {
                        JHVTime time = new JHVTime(parseTime(values[6]));
                        double lon = Math.toRadians(Double.parseDouble(values[7]));
                        double lat = Math.toRadians(Double.parseDouble(values[8]));
                        double x = Math.cos(lat) * Math.sin(lon);
                        double y = Math.sin(lat);
                        double z = Math.cos(lat) * Math.cos(lon);

                        positionMap.put(time, new PositionCartesian(time, x, y, z));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (positionMap.isEmpty())
            return null;
        positionMap.buildIndex();
        return positionMap;
    }

    private static class Callback implements FutureCallback<TimeMap<PositionCartesian>> {

        private final PositionMapReceiver receiver;

        Callback(PositionMapReceiver _receiver) {
            receiver = _receiver;
        }

        @Override
        public void onSuccess(TimeMap<PositionCartesian> result) {
            receiver.setMap(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
