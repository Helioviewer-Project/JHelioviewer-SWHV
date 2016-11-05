package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.jhv.threads.CancelTask;

public class PfssNewDataLoader implements Runnable {

    private static final int TIMEOUT_DOWNLOAD_SECONDS = 120;

    private final long start;
    private final long end;
    private final static SortedMap<Integer, ArrayList<Pair<String, Long>>> parsedCache = new TreeMap<Integer, ArrayList<Pair<String, Long>>>();

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
            ArrayList<Pair<String, Long>> urls = null;

            synchronized (parsedCache) {
                urls = parsedCache.get(cacheKey);
            }

            if (urls == null || urls.isEmpty()) {
                urls = new ArrayList<Pair<String, Long>>();
                String m = (startMonth) < 9 ? "0" + (startMonth + 1) : Integer.toString(startMonth + 1);
                String url = PfssSettings.baseURL + startYear + "/" + m + "/list.txt";

                try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream(), "UTF-8"))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        String[] splitted = inputLine.split(" ");
                        Date dd = TimeUtils.utcDateFormat.parse(splitted[0]);
                        urls.add(new Pair<String, Long>(splitted[1], dd.getTime()));
                    }
                } catch (MalformedURLException e) {
                    Log.warn("Could not read PFSS entries : URL unavailable");
                } catch (IOException e) {
                    Log.warn("Could not read PFSS entries");
                } catch (ParseException e) {
                    Log.warn("Could not parse date time during PFSS loading");
                }
            }

            synchronized (parsedCache) {
                parsedCache.put(cacheKey, urls);
            }

            for (Pair<String, Long> pair : urls) {
                Long dd = pair.b;
                String url = pair.a;
                if (dd > start - TimeUtils.DAY_IN_MILLIS && dd < end + TimeUtils.DAY_IN_MILLIS) {
                    FutureTask<Void> dataLoaderTask = new FutureTask<Void>(new PfssDataLoader(url, dd), null);
                    PfssPlugin.pfssDataPool.submit(dataLoaderTask);
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
