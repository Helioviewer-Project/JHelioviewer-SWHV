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
import org.helioviewer.jhv.layers.connect.ReceiverConnectivity.Connectivity;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import com.google.common.util.concurrent.FutureCallback;

public class LoadConnectivity implements Callable<Connectivity> {

    public static Void submit(@Nonnull URI uri, ReceiverConnectivity receiver) {
        EventQueueCallbackExecutor.pool.submit(new LoadConnectivity(uri), new Callback(receiver));
        return null;
    }

    private static final double AMPLIFY = 4;
    private static final double[] sswColor = new double[]{164 / 255., 48 / 255., 42 / 255.};
    private static final double[] fswColor = new double[]{74 / 255., 136 / 255., 92 / 255.};
    private static final double[] mColor = new double[]{240 / 255., 145 / 255., 53 / 255.};

    private static byte[] alphaColor(double[] color, double alpha) {
        return new byte[]{(byte) (color[0] * alpha), (byte) (color[1] * alpha), (byte) (color[2] * alpha), (byte) alpha};
    }

    private final URI uri;

    private LoadConnectivity(URI _uri) {
        uri = _uri;
    }

    @Override
    public Connectivity call() throws Exception {
        JHVTime time = null;
        List<OrthoScale> SSW = new ArrayList<>();
        List<OrthoScale> FSW = new ArrayList<>();
        List<OrthoScale> M = new ArrayList<>();

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
                        double density = 2.55 * MathUtils.clip(AMPLIFY * Double.parseDouble(values[3]), 0, 100);
                        Vec3 v = ConnectUtils.toCartesian(values[6], values[5]);
                        switch (values[1]) {
                            case "SSW":
                                SSW.add(new OrthoScale(v, alphaColor(sswColor, density)));
                                break;
                            case "FSW":
                                FSW.add(new OrthoScale(v, alphaColor(fswColor, density)));
                                break;
                            case "M":
                                M.add(new OrthoScale(v, alphaColor(mColor, density)));
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return time == null ? null : new Connectivity(time, SSW, FSW, M);
    }

    private static class Callback implements FutureCallback<Connectivity> {

        private final ReceiverConnectivity receiver;

        Callback(ReceiverConnectivity _receiver) {
            receiver = _receiver;
        }

        @Override
        public void onSuccess(Connectivity result) {
            receiver.setConnectivity(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
