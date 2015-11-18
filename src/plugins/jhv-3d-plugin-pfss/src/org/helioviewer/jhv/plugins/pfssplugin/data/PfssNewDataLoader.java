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
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.jhv.threads.CancelTask;

public class PfssNewDataLoader implements Runnable {

    private static int TIMEOUT_DOWNLOAD_SECONDS = 120;

    private final JHVDate start;
    private final JHVDate end;
    private final static SortedMap<Integer, ArrayList<Pair<String, Long>>> parsedCache = new TreeMap<Integer, ArrayList<Pair<String, Long>>>();

    public PfssNewDataLoader(JHVDate _start, JHVDate _end) {
        start = _start;
        end = _end;
    }

    @Override
    public void run() {
        if (start != null && end != null && start.milli <= end.milli) {
            final Calendar startCal = GregorianCalendar.getInstance();
            startCal.setTime(start.getDate());

            final Calendar endCal = GregorianCalendar.getInstance();
            endCal.setTime(new Date(end.milli + 31 * 24 * 60 * 60 * 1000));

            int startYear = startCal.get(Calendar.YEAR);
            int startMonth = startCal.get(Calendar.MONTH);

            final int endYear = endCal.get(Calendar.YEAR);
            final int endMonth = endCal.get(Calendar.MONTH);

            do {
                ArrayList<Pair<String, Long>> urls = null;

                try {
                    URL data;
                    Integer cacheKey = startYear * 10000 + startMonth;
                    synchronized (parsedCache) {
                        urls = parsedCache.get(cacheKey);
                    }
                    if (urls == null || urls.isEmpty()) {
                        urls = new ArrayList<Pair<String, Long>>();
                        String m = (startMonth) < 9 ? "0" + (startMonth + 1) : String.valueOf(startMonth + 1);
                        String url = PfssSettings.baseUrl + startYear + "/" + m + "/list.txt";
                        data = new URL(url);
                        BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream(), "UTF-8"));
                        String inputLine;
                        String[] splitted = null;
                        while ((inputLine = in.readLine()) != null) {
                            splitted = inputLine.split(" ");
                            url = splitted[1];
                            Date dd = TimeUtils.utcDateFormat.parse(splitted[0]);
                            urls.add(new Pair<String, Long>(url, dd.getTime()));
                        }
                        in.close();
                        synchronized (parsedCache) {
                            parsedCache.put(cacheKey, urls);
                        }
                    }
                } catch (MalformedURLException e) {
                    Log.warn("Could not read pfss entries : URL unavailable");
                } catch (IOException e) {
                    Log.warn("Could not read pfss entries");
                } catch (ParseException e) {
                    Log.warn("Could not parse date time during pfss loading");
                }
                for (Pair<String, Long> pair : urls) {
                    Long dd = pair.b;
                    String url = pair.a;
                    if (dd > start.milli - 24 * 60 * 60 * 1000 && dd < end.milli + 24 * 60 * 60 * 1000) {
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
            } while (startYear < endYear && (startYear >= endYear && startMonth <= endMonth));
        }
    }

}
