package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.Date;

import org.helioviewer.base.math.Interval;

public class RadioData {
    private byte[][] data;
    private FrequencyInterval frequencyInterval;
    private Interval<Date> timeInterval;
    private double freqPerPixel;
    private double timePerPixel;

    public RadioData(byte[][] data, FrequencyInterval frequencyInterval, Interval<Date> timeInterval, double freqPerPixel, double timePerPixel) {
        super();
        this.data = data;
        this.frequencyInterval = frequencyInterval;
        this.timeInterval = timeInterval;
        this.freqPerPixel = freqPerPixel;
        this.timePerPixel = timePerPixel;
    }

    public byte[][] getData() {
        return data;
    }

    public void setData(byte[][] data) {
        this.data = data;
    }

    public FrequencyInterval getFrequencyInterval() {
        return frequencyInterval;
    }

    public void setFrequencyInterval(FrequencyInterval frequencyInterval) {
        this.frequencyInterval = frequencyInterval;
    }

    public Interval<Date> getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(Interval<Date> timeInterval) {
        this.timeInterval = timeInterval;
    }

    public double getFreqPerPixel() {
        return freqPerPixel;
    }

    public void setFreqPerPixel(double freqPerPixel) {
        this.freqPerPixel = freqPerPixel;
    }

    public double getTimePerPixel() {
        return timePerPixel;
    }

    public void setTimePerPixel(double timePerPixel) {
        this.timePerPixel = timePerPixel;
    }
}
