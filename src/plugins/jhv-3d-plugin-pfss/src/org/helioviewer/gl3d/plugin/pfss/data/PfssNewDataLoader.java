package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.base.Pair;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

public class PfssNewDataLoader implements Runnable {
    private final static ExecutorService pfssPool = Executors.newFixedThreadPool(5);
    private final Date start;
    private final Date end;
    //Integer is year*1000 + month; to be synchronized across Threads! Prohibited use outside this class.
    private final static SortedMap<Integer, ArrayList<Pair<String, Long>>> parsedCache = new TreeMap<Integer, ArrayList<Pair<String, Long>>>();

    public PfssNewDataLoader(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        if (start != null && end != null && start.before(end)) {
            final Calendar startCal = GregorianCalendar.getInstance();
            startCal.setTime(start);

            final Calendar endCal = GregorianCalendar.getInstance();
            endCal.setTime(new Date(end.getTime() + 31 * 24 * 60 * 60 * 1000));

            int startYear = startCal.get(Calendar.YEAR);
            int startMonth = startCal.get(Calendar.MONTH);

            final int endYear = endCal.get(Calendar.YEAR);
            final int endMonth = endCal.get(Calendar.MONTH);

            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

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
                        String m = (startMonth) < 9 ? "0" + (startMonth + 1) : (startMonth + 1) + "";
                        String url = PfssSettings.baseUrl + startYear + "/" + m + "/list.txt";
                        data = new URL(url);
                        BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
                        String inputLine;
                        String[] splitted = null;
                        while ((inputLine = in.readLine()) != null) {
                            splitted = inputLine.split(" ");
                            url = splitted[1];
                            Date dd = dateFormat.parse(splitted[0]);
                            urls.add(new Pair(url, dd.getTime()));
                        }
                        in.close();
                        synchronized (parsedCache) {
                            parsedCache.put(cacheKey, urls);
                        }
                    }
                } catch (MalformedURLException e) {
                    errorState = true;
                    Log.warn("Could not read pfss entries : URL unavailable");
                } catch (IOException e) {
                    errorState = true;
                    Log.warn("Could not read pfss entries");
                } catch (ParseException e) {
                    errorState = true;
                    Log.warn("Could not parse date time during pfss loading");
                }
                for (Pair<String, Long> pair : urls) {
                    Long dd = pair.b;
                    String url = pair.a;
                    if (dd > start.getTime() - 24 * 60 * 60 * 1000 && dd < end.getTime() + 24 * 60 * 60 * 1000) {
                        Thread t = new Thread(new PfssDataLoader(url, dd), "PFFSLoader");
                        pfssPool.submit(t);
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
