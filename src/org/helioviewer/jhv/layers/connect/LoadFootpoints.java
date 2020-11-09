package org.helioviewer.jhv.layers.connect;

import java.io.BufferedReader;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionListReceiver;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import com.google.common.util.concurrent.FutureCallback;

public class LoadFootpoints implements Callable<List<Position>> {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void submit(@Nonnull URI uri, PositionListReceiver receiver) {
        EventQueueCallbackExecutor.pool.submit(new LoadFootpoints(uri), new Callback(receiver));
    }

    private final URI uri;

    private LoadFootpoints(URI _uri) {
        uri = _uri;
    }

    @Override
    public List<Position> call() throws Exception {
        ArrayList<Position> posList = new ArrayList<>();

        try (NetClient nc = NetClient.of(uri); BufferedReader br = new BufferedReader(nc.getReader())) {
            String line = br.readLine(); // skip first line
            while ((line = br.readLine()) != null) {
                String[] values = Regex.Comma.split(line);
                if (values.length > 8) {
                    try {
                        JHVTime time = new JHVTime(TimeUtils.parse(values[6], timeFormatter));
                        double lon = Math.toRadians(Double.parseDouble(values[7]));
                        double lat = Math.toRadians(Double.parseDouble(values[8]));
                        posList.add(new Position(time, 1, lon, lat));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return posList;
    }

    private static class Callback implements FutureCallback<List<Position>> {

        private final PositionListReceiver receiver;

        Callback(PositionListReceiver _receiver) {
            receiver = _receiver;
        }

        @Override
        public void onSuccess(List<Position> result) {
            receiver.setList(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
