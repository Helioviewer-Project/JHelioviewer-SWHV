package org.helioviewer.jhv.layers.connect;

import java.io.BufferedReader;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMap;
import org.helioviewer.jhv.time.TimeUtils;

import com.google.common.util.concurrent.FutureCallback;

public class LoadFootpoint {

    public interface Receiver {
        void setPositionMap(@Nullable TimeMap<Position.Cartesian> positionMap);
    }

    private static final DateTimeFormatter euroTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void submit(@Nonnull URI uri, Receiver receiver) {
        EventQueueCallbackExecutor.pool.submit(new Footpoint(uri), new Callback(receiver));
    }

    private static long parseTime(String s) {
        try {
            return TimeUtils.parse(TimeUtils.sqlTimeFormatter, s);
        } catch (Exception e) {
            return TimeUtils.parse(euroTimeFormatter, s);
        }
    }

    private record Footpoint(URI uri) implements Callable<TimeMap<Position.Cartesian>> {
        @Override
        public TimeMap<Position.Cartesian> call() throws Exception {
            TimeMap<Position.Cartesian> positionMap = new TimeMap<>();

            try (NetClient nc = NetClient.of(uri); BufferedReader br = new BufferedReader(nc.getReader())) {
                br.readLine(); // skip 1st line

                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = Regex.Comma.split(line);
                    if (values.length > 8) {
                        try {
                            JHVTime time = new JHVTime(parseTime(values[6]));
                            positionMap.put(time, ConnectUtils.toCartesian(time.milli, values[7], values[8]));
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
    }

    private record Callback(Receiver receiver) implements FutureCallback<TimeMap<Position.Cartesian>> {

        @Override
        public void onSuccess(TimeMap<Position.Cartesian> result) {
            receiver.setPositionMap(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
