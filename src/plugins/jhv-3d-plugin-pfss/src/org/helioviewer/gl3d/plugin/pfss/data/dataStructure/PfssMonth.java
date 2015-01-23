package org.helioviewer.gl3d.plugin.pfss.data.dataStructure;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bean for Month
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssMonth {
    private final int month;
    private final CopyOnWriteArrayList<PfssDayAndTime> dayAndTimes;

    public PfssMonth(int month) {
        this.month = month;
        this.dayAndTimes = new CopyOnWriteArrayList<PfssDayAndTime>();
    }

    public PfssDayAndTime addDayAndTime(int year, int month, int dayAndTime, String url) {
        PfssDayAndTime tmp = new PfssDayAndTime(year, month, dayAndTime, url);
        dayAndTimes.add(tmp);
        return tmp;
    }

    public PfssDayAndTime findData(int dayAndTime) {

        PfssDayAndTime last = null;
        PfssDayAndTime entry = null;
        for (PfssDayAndTime data : dayAndTimes) {
            if (last == null)
                last = data;
            if (dayAndTime <= data.getDayAndTime()) {
                entry = data;
                break;
            }
        }
        if (entry == null && last != null)
            return last.getNext();
        return entry;
    }

    @Override
    public String toString() {
        return month + "";
    }
}
