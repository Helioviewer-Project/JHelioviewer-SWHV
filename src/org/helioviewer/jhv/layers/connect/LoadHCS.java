package org.helioviewer.jhv.layers.connect;

import java.io.BufferedReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.layers.connect.ReceiverHCS.HCS;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import com.google.common.util.concurrent.FutureCallback;

public class LoadHCS implements Callable<HCS> {

    private static final double radius = 1.01;

    public static Void submit(@Nonnull URI uri, ReceiverHCS receiver) {
        EventQueueCallbackExecutor.pool.submit(new LoadHCS(uri), new Callback(receiver));
        return null;
    }

    private final URI uri;

    private LoadHCS(URI _uri) {
        uri = _uri;
    }

    @Override
    public HCS call() throws Exception {
        List<Vec3> hcsList = new ArrayList<>();

        try (NetClient nc = NetClient.of(uri); BufferedReader br = new BufferedReader(nc.getReader())) {
            int lineNo = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) // skip comment lines
                    continue;

                lineNo++;

                if (lineNo <= 1) // skip 1 line
                    continue;

                String[] values = Regex.MultiSpace.split(line);
                if (values.length > 4) {
                    try {
                        double lat = Math.toRadians(Double.parseDouble(values[3]));
                        double lon = Math.toRadians(Double.parseDouble(values[4]));
                        double x = Math.cos(lat) * Math.sin(lon);
                        double y = Math.sin(lat);
                        double z = Math.cos(lat) * Math.cos(lon);

                        hcsList.add(new Vec3(x, y, z));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        int size = hcsList.size();
        if (size == 0)
            return null;

        List<Vec3> ortho = new ArrayList<>(size);
        List<Vec3> scale = new ArrayList<>(size);
        hcsList.forEach(v -> {
            ortho.add(new Vec3(radius * v.x, radius * v.y, radius * v.z));
            scale.add(new Vec3(v.x, -v.y, v.z));
        });
        return new HCS(ortho, scale);
    }

    private static class Callback implements FutureCallback<HCS> {

        private final ReceiverHCS receiver;

        Callback(ReceiverHCS _receiver) {
            receiver = _receiver;
        }

        @Override
        public void onSuccess(HCS result) {
            receiver.setHCS(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
