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
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import com.google.common.util.concurrent.FutureCallback;

public class LoadHCS implements Callable<List<Vec3>> {

    public static Void submit(@Nonnull URI uri, ReceiverVecList receiver) {
        EventQueueCallbackExecutor.pool.submit(new LoadHCS(uri), new Callback(receiver));
        return null;
    }

    private final URI uri;

    private LoadHCS(URI _uri) {
        uri = _uri;
    }

    @Override
    public List<Vec3> call() throws Exception {
        ArrayList<Vec3> vecList = new ArrayList<>();

        try (NetClient nc = NetClient.of(uri); BufferedReader br = new BufferedReader(nc.getReader())) {
            br.readLine(); // skip 1st line
            br.readLine(); // skip 2nd line
            br.readLine(); // skip 3rd line
            br.readLine(); // skip 4th line

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = Regex.MultiSpace.split(line);
                if (values.length > 4) {
                    try {
                        double lat = Math.toRadians(Double.parseDouble(values[3]));
                        double lon = Math.toRadians(Double.parseDouble(values[4]));
                        double x = Math.cos(lat) * Math.sin(lon);
                        double y = Math.sin(lat);
                        double z = Math.cos(lat) * Math.cos(lon);
                        vecList.add(new Vec3(x, y, z));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (vecList.isEmpty())
            return null;
        return vecList;
    }

    private static class Callback implements FutureCallback<List<Vec3>> {

        private final ReceiverVecList receiver;

        Callback(ReceiverVecList _receiver) {
            receiver = _receiver;
        }

        @Override
        public void onSuccess(List<Vec3> result) {
            receiver.setVecList(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
