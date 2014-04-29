package org.helioviewer.gl3d.plugin.pfss.data.dataStructure;

/**
 * Bean for days and time
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssDayAndTime {
    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    private PfssDayAndTime next = null;
    private int dayAndTime;
    private int year;
    private int month;
    private String url = null;

    public PfssDayAndTime(int year, int month, int dayAndTime, String url) {
        this.year = year;
        this.month = month;
        this.dayAndTime = dayAndTime;
        this.url = url;
    }

    public void addNext(PfssDayAndTime next) {
        this.next = next;
    }

    public String getUrl() {
        return url;
    }

    public PfssDayAndTime getNext() {
        return next;
    }

    public int getDayAndTime() {
        return dayAndTime;
    }
}
