package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.util.HashMap;

public class BandType {

    private String baseURL;
    private String label;
    private String name;
    private String unitLabel;
    public HashMap<String, Double> warnLevels = new HashMap<String, Double>();
    private double min;
    private double max;
    private boolean isLog = false;

    @Override
    public String toString() {
        return label;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(String unitLabel) {
        this.unitLabel = unitLabel;
    }

    public HashMap<String, Double> getWarnLevels() {
        return warnLevels;
    }

    public void setWarnLevels(HashMap<String, Double> warnLevels) {
        this.warnLevels = warnLevels;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setScale(String scale) {
        isLog = scale.equals("logarithmic");
    }

    public boolean isLogScale() {
        return isLog;
    }

}
