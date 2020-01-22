package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.EventQueue;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;

import okio.BufferedSource;

import com.google.common.util.concurrent.FutureCallback;

public class PfssNewDataLoader implements Callable<Void> {

    public static Future<Void> submit(long start, long end) {
        return EventQueueCallbackExecutor.pool.submit(new PfssNewDataLoader(start, end), new Callback(start));
    }

    private final long start;
    private final long end;

    private PfssNewDataLoader(long _start, long _end) {
        PfssPlugin.downloads++;
        start = _start;
        end = _end;
    }

    @Override
    public Void call() {
        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(start);
        int startYear = cal.get(Calendar.YEAR);
        int startMonth = cal.get(Calendar.MONTH);

        cal.setTimeInMillis(end + 31 * TimeUtils.DAY_IN_MILLIS);
        int endYear = cal.get(Calendar.YEAR);
        int endMonth = cal.get(Calendar.MONTH);

        do {
            String m = startMonth < 9 ? "0" + (startMonth + 1) : Integer.toString(startMonth + 1);
            String url = PfssSettings.baseURL + startYear + '/' + m + "/list.txt";
            HashMap<Long, String> urls = new HashMap<>();

            // may come from http cache
            try (NetClient nc = NetClient.of(url)) {
                BufferedSource source = nc.getSource();
                String line;
                while ((line = source.readUtf8Line()) != null) {
                    String[] splitted = Regex.Space.split(line);
                    if (splitted.length != 2)
                        throw new Exception("Invalid line: " + line);
                    urls.put(TimeUtils.parse(splitted[0]), PfssSettings.baseURL + splitted[1]);
                }
            } catch (Exception e) {
                Log.warn("PFSS list error: " + e);
            }
            EventQueue.invokeLater(() -> PfssPlugin.getPfsscache().put(urls));

            if (startMonth == 11) {
                startMonth = 0;
                startYear++;
            } else {
                startMonth++;
            }
        } while (startYear < endYear || (startYear == endYear && startMonth <= endMonth));
        return null;
    }

    private static class Callback implements FutureCallback<Void> {

        private final long start;

        Callback(long _start) {
            start = _start;
        }

        @Override
        public void onSuccess(Void result) {
            PfssPlugin.downloads--;
            PfssPlugin.getPfsscache().getNearestData(start); // preload first
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            PfssPlugin.downloads--;
            Log.error("PfssNewDataLoader", t);
        }

    }

}
