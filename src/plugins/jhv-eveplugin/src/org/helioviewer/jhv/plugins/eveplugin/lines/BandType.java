package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.util.HashMap;

public class BandType {

    private String baseURL;
    private String label;
    private String name;
    private String unitLabel;
    public HashMap<String, Double> warnLevels = new HashMap<>();
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

    public void setBaseURL(String _baseURL) {
        baseURL = _baseURL;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String _label) {
        label = _label;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(String _unitLabel) {
        unitLabel = _unitLabel;
    }

    public HashMap<String, Double> getWarnLevels() {
        return warnLevels;
    }

    public void setWarnLevels(HashMap<String, Double> _warnLevels) {
        warnLevels = _warnLevels;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double _min) {
        min = _min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double _max) {
        max = _max;
    }

    public void setName(String _name) {
        name = _name;
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
