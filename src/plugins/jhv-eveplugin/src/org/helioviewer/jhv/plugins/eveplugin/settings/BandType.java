package org.helioviewer.jhv.plugins.eveplugin.settings;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;

public class BandType {
    private String baseUrl;
    private BandGroup group;
    private String label;
    private String name;
    private String unitLabel;
    public HashMap<String, Double> warnLevels = new HashMap<String, Double>();
    private double min;
    private double max;
    private String scale = "";
    private boolean isLog = false;

    // private double multiplier = 1.0;

    public URL buildUrl(Interval<Date> interval) {
        final SimpleDateFormat eveAPIDateFormat = new SimpleDateFormat(EVEAPI.API_DATE_FORMAT);
        URL url = null;
        try {
            url = new URL(baseUrl + EVEAPI.API_URL_PARAMETER_STARTDATE + eveAPIDateFormat.format(interval.getStart()) + "&" + EVEAPI.API_URL_PARAMETER_ENDDATE + eveAPIDateFormat.format(interval.getEnd()) + "&" + EVEAPI.API_URL_PARAMETER_TYPE + this.getName() + "&" + EVEAPI.API_URL_PARAMETER_FORMAT + EVEAPI.API_URL_PARAMETER_FORMAT_VALUES.JSON);
        } catch (MalformedURLException e) {
            Log.error("Something is wrong with the EVEAPI url", e);
        }
        return url;
    }

    public String toStringAlt() {
        String str = "baseUrl:" + baseUrl;
        str += "\ngroup:" + group.toString();
        str += "\nlabel:" + label;
        str += "\nunitLabel:" + unitLabel;
        str += "\nwarnLevels: [";
        for (Map.Entry<String, Double> entry : warnLevels.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            str += key + ":";
            str += value.toString() + ",";
        }
        str += ']';
        return str;
    }

    @Override
    public String toString() {
        return label;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public BandGroup getGroup() {
        return group;
    }

    public void setGroup(BandGroup group) {
        this.group = group;
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
        this.scale = scale;
        isLog = scale.equals("logarithmic");
    }

    public String getScale() {
        return scale;
    }

    public boolean isLogScale() {
        return isLog;
    }

    /*
     * public double getMultiplier() { return multiplier; } public void
     * setMultiplier(double multiplier) { this.multiplier = multiplier; }
     */
}
