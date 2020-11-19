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

public class LoadHCS implements Callable<OrthoScaleList> {

    public static Void submit(@Nonnull URI uri, ReceiverHCS receiver) {
        EventQueueCallbackExecutor.pool.submit(new LoadHCS(uri), new Callback(receiver));
        return null;
    }

    private final URI uri;

    private LoadHCS(URI _uri) {
        uri = _uri;
    }

    @Override
    public OrthoScaleList call() throws Exception {
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

        return new OrthoScaleList(hcsList);
    }

    private static class Callback implements FutureCallback<OrthoScaleList> {

        private final ReceiverHCS receiver;

        Callback(ReceiverHCS _receiver) {
            receiver = _receiver;
        }

        @Override
        public void onSuccess(OrthoScaleList result) {
            receiver.setHCS(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
