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

public class LoadHCS {

    public interface Receiver {
        void setHCS(OrthoScaleList hcs);
    }

    public static void submit(@Nonnull URI uri, Receiver receiver) {
        EventQueueCallbackExecutor.pool.submit(new HCS(uri), new Callback(receiver));
    }

    private record HCS(URI uri) implements Callable<OrthoScaleList> {
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
                            hcsList.add(ConnectUtils.toCartesian(values[4], values[3]));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return new OrthoScaleList(hcsList);
        }
    }

    private record Callback(Receiver receiver) implements FutureCallback<OrthoScaleList> {

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
