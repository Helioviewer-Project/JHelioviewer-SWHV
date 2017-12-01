package org.helioviewer.jhv.plugins.pfss.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TreeMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;
import org.helioviewer.jhv.threads.CancelTask;
import org.helioviewer.jhv.time.TimeUtils;

import okio.BufferedSource;

public class PfssNewDataLoader implements Runnable {

    private static final int TIMEOUT_DOWNLOAD_SECONDS = 120;

    private final long start;
    private final long end;
    private static final TreeMap<Integer, ArrayList<Pair<String, Long>>> parsedCache = new TreeMap<>();

    public PfssNewDataLoader(long _start, long _end) {
        start = _start;
        end = _end;
    }

    @Override
    public void run() {
        Calendar cal = GregorianCalendar.getInstance();

        cal.setTimeInMillis(start);
        int startYear = cal.get(Calendar.YEAR);
        int startMonth = cal.get(Calendar.MONTH);

        cal.setTimeInMillis(end + 31 * TimeUtils.DAY_IN_MILLIS);
        int endYear = cal.get(Calendar.YEAR);
        int endMonth = cal.get(Calendar.MONTH);

        do {
            Integer cacheKey = startYear * 10000 + startMonth;
            ArrayList<Pair<String, Long>> urls;

            synchronized (parsedCache) {
                urls = parsedCache.get(cacheKey);
            }

            if (urls == null || urls.isEmpty()) {
                urls = new ArrayList<>();
                String m = startMonth < 9 ? "0" + (startMonth + 1) : Integer.toString(startMonth + 1);
                String url = PfssSettings.baseURL + startYear + '/' + m + "/list.txt";

                try (NetClient nc = new NetClient(url)) {
                    BufferedSource source = nc.getSource();
                    String inputLine;
                    while ((inputLine = source.readUtf8Line()) != null) {
                        String[] splitted = inputLine.split(" ");
                        urls.add(new Pair<>(splitted[1], TimeUtils.parse(splitted[0])));
                    }
                } catch (Exception e) {
                    Log.warn("Could not read PFSS entries: " + e);
                }
            }

            synchronized (parsedCache) {
                parsedCache.put(cacheKey, urls);
            }

            for (Pair<String, Long> pair : urls) {
                Long dd = pair.b;
                String url = pair.a;
                if (dd > start - TimeUtils.DAY_IN_MILLIS && dd < end + TimeUtils.DAY_IN_MILLIS) {
                    FutureTask<Void> dataLoaderTask = new FutureTask<>(new PfssDataLoader(url, dd), null);
                    PfssPlugin.pfssDataPool.execute(dataLoaderTask);
                    PfssPlugin.pfssReaperPool.schedule(new CancelTask(dataLoaderTask), TIMEOUT_DOWNLOAD_SECONDS, TimeUnit.SECONDS);
                }
            }

            if (startMonth == 11) {
                startMonth = 0;
                startYear++;
            } else {
                startMonth++;
            }
        } while (startYear < endYear || (startYear == endYear && startMonth <= endMonth));
    }

}
