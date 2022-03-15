package org.helioviewer.jhv.layers.connect;

import java.io.BufferedReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import com.google.common.util.concurrent.FutureCallback;

public class LoadConnectivity {

    public static class Connectivity {

        public final JHVTime time;
        public final OrthoScaleList SSW;
        public final OrthoScaleList FSW;
        public final OrthoScaleList M;

        Connectivity(JHVTime _time, List<Vec3> cartSSW, List<Vec3> cartFSW, List<Vec3> cartM) {
            time = _time;
            SSW = new OrthoScaleList(cartSSW);
            FSW = new OrthoScaleList(cartFSW);
            M = new OrthoScaleList(cartM);
        }

    }

    public interface Receiver {
        void setConnectivity(@Nullable Connectivity connectivity);
    }

    public static void submit(@Nonnull URI uri, Receiver receiver) {
        EventQueueCallbackExecutor.pool.submit(new ConnectivityLoad(uri), new Callback(receiver));
    }

    private record ConnectivityLoad(URI uri) implements Callable<Connectivity> {
        @Override
        public Connectivity call() throws Exception {
            JHVTime time = null;
            List<Vec3> SSW = new ArrayList<>();
            List<Vec3> FSW = new ArrayList<>();
            List<Vec3> M = new ArrayList<>();

            try (NetClient nc = NetClient.of(uri); BufferedReader br = new BufferedReader(nc.getReader())) {
                int lineNo = 0;
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#"))
                        continue;

                    lineNo++;

                    if (lineNo <= 2) // skip 2 lines
                        continue;
                    if (lineNo == 3) {
                        time = new JHVTime(TimeUtils.parse(TimeUtils.sqlTimeFormatter, line));
                        continue;
                    }
                    if (lineNo <= 5) // skip 2 lines
                        continue;

                    String[] values = Regex.MultiSpace.split(line);
                    if (values.length > 6) {
                        try {
                            double density = Double.parseDouble(values[3]);
                            if (density <= 1)
                                continue;

                            Vec3 v = ConnectUtils.toCartesian(values[6], values[5]);
                            switch (values[1]) {
                                case "SSW" -> SSW.add(v);
                                case "FSW" -> FSW.add(v);
                                case "M" -> M.add(v);
                            }
                        } catch (Exception e) {
                            Log.warn(e);
                        }
                    }
                }
            }

            if (time == null)
                return null;
            return new Connectivity(time, SSW, FSW, M);
        }
    }

    private record Callback(Receiver receiver) implements FutureCallback<Connectivity> {

        @Override
        public void onSuccess(Connectivity result) {
            receiver.setConnectivity(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred opening the remote file", t.getMessage());
        }

    }

}
