package org.helioviewer.jhv.layers.connect;

import java.io.BufferedReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.thread.Task;

public class LoadHCS {

    public interface Receiver {
        void setHCS(List<Vec3> hcs);
    }

    public static void submit(@Nonnull URI uri, Receiver receiver) {
        Task.submit(uri.toString(), new HCS(uri), receiver::setHCS, "Error getting the data");
    }

    private record HCS(URI uri) implements Callable<List<Vec3>> {
        @Override
        public List<Vec3> call() throws Exception {
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
                            Log.warn(e);
                        }
                    }
                }
            }
            if (!hcsList.isEmpty()) // close line
                hcsList.add(hcsList.getFirst());
            return hcsList;
        }
    }

}
