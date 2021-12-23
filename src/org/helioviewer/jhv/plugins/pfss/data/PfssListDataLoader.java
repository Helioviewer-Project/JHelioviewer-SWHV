package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.EventQueue;
import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;

import okio.BufferedSource;

import com.google.common.util.concurrent.FutureCallback;

public class PfssListDataLoader {

    public static void submit(long start, long end) {
        EventQueueCallbackExecutor.pool.submit(new ListDataLoader(start, end), new Callback(start));
        PfssPlugin.downloads++;
    }

    private record ListDataLoader(long start, long end) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            Calendar cal = Calendar.getInstance();

            cal.setTimeInMillis(start);
            int startYear = cal.get(Calendar.YEAR);
            int startMonth = cal.get(Calendar.MONTH);

            cal.setTimeInMillis(end + 31 * TimeUtils.DAY_IN_MILLIS);
            int endYear = cal.get(Calendar.YEAR);
            int endMonth = cal.get(Calendar.MONTH);

            do {
                String m = startMonth < 9 ? "0" + (startMonth + 1) : Integer.toString(startMonth + 1);
                URI listUri = new URI(PfssSettings.BASE_URL + startYear + '/' + m + "/list.txt");
                HashMap<Long, URI> uris = new HashMap<>();

                // may come from http cache
                try (NetClient nc = NetClient.of(listUri); BufferedSource source = nc.getSource()) {
                    String line;
                    while ((line = source.readUtf8Line()) != null) {
                        String[] splitted = Regex.Space.split(line);
                        if (splitted.length != 2)
                            throw new Exception("Invalid line: " + line);
                        uris.put(TimeUtils.parse(splitted[0]), new URI(PfssSettings.BASE_URL + splitted[1]));
                    }
                } catch (Exception e) { // continue in case of list error
                    Log.warn("PFSS list error", e);
                }
                EventQueue.invokeLater(() -> PfssPlugin.getPfsscache().put(uris));

                if (startMonth == 11) {
                    startMonth = 0;
                    startYear++;
                } else {
                    startMonth++;
                }
            } while (startYear < endYear || (startYear == endYear && startMonth <= endMonth));
            return null;
        }

    }

    private record Callback(long start) implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
            PfssPlugin.downloads--;
            PfssPlugin.getPfsscache().getNearestData(start); // preload first
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            PfssPlugin.downloads--;
            Log.error(t);
        }

    }

}
