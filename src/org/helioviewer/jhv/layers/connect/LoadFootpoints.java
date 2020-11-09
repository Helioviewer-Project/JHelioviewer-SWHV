package org.helioviewer.jhv.layers.connect;

import java.io.BufferedReader;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
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

public class LoadFootpoints implements Callable<TimeMap<Position>> {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void submit(@Nonnull URI uri, PositionMapReceiver receiver) {
        EventQueueCallbackExecutor.pool.submit(new LoadFootpoints(uri), new Callback(receiver));
    }

    private final URI uri;

    private LoadFootpoints(URI _uri) {
        uri = _uri;
    }

    @Override
    public TimeMap<Position> call() throws Exception {
        TimeMap<Position> positionMap = new TimeMap<>();

        try (NetClient nc = NetClient.of(uri); BufferedReader br = new BufferedReader(nc.getReader())) {
            String line = br.readLine(); // skip first line
            while ((line = br.readLine()) != null) {
                String[] values = Regex.Comma.split(line);
                if (values.length > 8) {
                    try {
                        JHVTime time = new JHVTime(TimeUtils.parse(timeFormatter, values[6]));
                        double lon = Math.toRadians(Double.parseDouble(values[7]));
                        double lat = Math.toRadians(Double.parseDouble(values[8]));
                        positionMap.put(time, new Position(time, 1, lon, lat));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        positionMap.buildIndex();
        return positionMap;
    }

    private static class Callback implements FutureCallback<TimeMap<Position>> {

        private final PositionMapReceiver receiver;

        Callback(PositionMapReceiver _receiver) {
            receiver = _receiver;
        }

        @Override
        public void onSuccess(TimeMap<Position> result) {
            receiver.setMap(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
