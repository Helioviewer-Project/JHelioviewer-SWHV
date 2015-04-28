package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

public class PfssNewDataLoader implements Runnable {
    private final static ExecutorService pfssPool = Executors.newFixedThreadPool(5);
    private final Date start;
    private final Date end;

    public PfssNewDataLoader(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        if (start != null && end != null) {
            Calendar startCal = GregorianCalendar.getInstance();
            startCal.setTime(start);

            Calendar endCal = GregorianCalendar.getInstance();
            endCal.setTime(end);

            int startYear = startCal.get(Calendar.YEAR);
            int startMonth = startCal.get(Calendar.MONTH);

            int endYear = endCal.get(Calendar.YEAR);
            int endMonth = endCal.get(Calendar.MONTH);
            boolean run = true;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            do {
                URL data;
                try {
                    String m = (startMonth) < 9 ? "0" + (startMonth + 1) : (startMonth + 1) + "";
                    data = new URL(PfssSettings.baseUrl + startYear + "/" + m + "/list.txt");
                    BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));

                    String inputLine;
                    String[] splitted = null;
                    String url;
                    while ((inputLine = in.readLine()) != null) {
                        splitted = inputLine.split(" ");
                        url = splitted[1];

                        try {
                            Date dd = dateFormat.parse(splitted[0]);
                            Thread t = new Thread(new PfssDataLoader(url, dd.getTime()), "PFFSLoader");
                            pfssPool.submit(t);
                        } catch (ParseException e) {
                            Log.debug("Date could not be parsed from url " + url + "Exception was thrown : " + e);
                        }
                    }
                    in.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                }
                if (startYear == endYear && startMonth < endMonth) {
                    startMonth++;
                } else if (startYear < endYear) {
                    if (startMonth == 11) {
                        startMonth = 1;
                        startYear++;
                    } else {
                        startMonth++;
                    }
                }
            } while (startYear != endYear && startMonth != endMonth);

        }
    }
}
