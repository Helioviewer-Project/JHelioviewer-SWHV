package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.EventQueue;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Regex;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;

import okio.BufferedSource;

public class PfssNewDataLoader extends JHVWorker<Void, Void> {

    private final long start;
    private final long end;

    public PfssNewDataLoader(long _start, long _end) {
        PfssPlugin.downloads++;
        start = _start;
        end = _end;
    }

    @Nullable
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

    @Override
    protected void done() {
        PfssPlugin.downloads--;
        PfssPlugin.getPfsscache().getNearestData(start); // preload first
    }

}
