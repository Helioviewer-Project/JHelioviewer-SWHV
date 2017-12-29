package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.EventQueue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TreeMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;
import org.helioviewer.jhv.threads.CancelTask;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;

import okio.BufferedSource;

public class PfssNewDataLoader extends JHVWorker<Void, Void> {

    private final long start;
    private final long end;
    private static final TreeMap<Integer, ArrayList<Pair<Long, String>>> parsedCache = new TreeMap<>();

    public PfssNewDataLoader(long _start, long _end) {
        PfssPlugin.downloads++;
        start = _start;
        end = _end;
    }

    @Override
    protected Void backgroundWork() {
        Calendar cal = GregorianCalendar.getInstance();

        cal.setTimeInMillis(start);
        int startYear = cal.get(Calendar.YEAR);
        int startMonth = cal.get(Calendar.MONTH);

        cal.setTimeInMillis(end + 31 * TimeUtils.DAY_IN_MILLIS);
        int endYear = cal.get(Calendar.YEAR);
        int endMonth = cal.get(Calendar.MONTH);

        do {
            Integer cacheKey = startYear * 10000 + startMonth;
            ArrayList<Pair<Long, String>> urls;

            synchronized (parsedCache) {
                urls = parsedCache.get(cacheKey);
            }

            if (urls == null || urls.isEmpty()) {
                urls = new ArrayList<>();
                String m = startMonth < 9 ? "0" + (startMonth + 1) : Integer.toString(startMonth + 1);
                String url = PfssSettings.baseURL + startYear + '/' + m + "/list.txt";

                try (NetClient nc = NetClient.of(url)) {
                    BufferedSource source = nc.getSource();
                    String line;
                    while ((line = source.readUtf8Line()) != null) {
                        String[] splitted = Regex.Space.split(line);
                        if (splitted.length != 2)
                            throw new Exception("Invalid line: " + line);
                        urls.add(new Pair<>(TimeUtils.parse(splitted[0]), splitted[1]));
                    }
                } catch (Exception e) {
                    Log.warn("Could not read PFSS entries: " + e);
                }

                synchronized (parsedCache) {
                    parsedCache.put(cacheKey, urls);
                }
            }

            ArrayList<Pair<Long, String>> furls = urls;
            EventQueue.invokeLater(() -> {
                for (Pair<Long, String> pair : furls) {
                    long time = pair.a;
                    if (time > start - TimeUtils.DAY_IN_MILLIS && time < end + TimeUtils.DAY_IN_MILLIS && PfssPlugin.getPfsscache().getData(time) == null) {
                        FutureTask<Void> dataLoaderTask = new FutureTask<>(new PfssDataLoader(time, PfssSettings.baseURL + pair.b), null);
                        PfssPlugin.pfssDataPool.execute(dataLoaderTask);
                        PfssPlugin.pfssReaperPool.schedule(new CancelTask(dataLoaderTask), PfssSettings.TIMEOUT_DOWNLOAD, TimeUnit.SECONDS);
                    }
                }
            });

            if (startMonth == 11) {
                startMonth = 0;
                startYear++;
            } else {
                startMonth++;
            }
        } while (startYear < endYear || (startYear == endYear && startMonth <= endMonth));
        return null;
    }

    @Override
    protected void done() {
        PfssPlugin.downloads--;
    }

}
