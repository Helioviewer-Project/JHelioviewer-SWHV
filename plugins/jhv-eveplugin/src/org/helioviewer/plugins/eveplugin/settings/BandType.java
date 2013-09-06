package org.helioviewer.plugins.eveplugin.settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.net.MalformedURLException;
import java.net.URL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI;



public class BandType {
	private String baseUrl;
	private BandGroup group;
	private String label;
	private String unitLabel;
	public HashMap<String, Double> warnLevels = new HashMap<String, Double>();
	private double min;
	private double max;
	private double[] errorLevels;
	
	public URL buildUrl(Interval<Date> interval){
		final SimpleDateFormat eveAPIDateFormat = new SimpleDateFormat(EVEAPI.API_DATE_FORMAT);
		URL url = null;
        try {
			url = new URL(
			        this.baseUrl + 
			        EVEAPI.API_URL_PARAMETER_STARTDATE + eveAPIDateFormat.format(interval.getStart()) + "&" + 
			        EVEAPI.API_URL_PARAMETER_ENDDATE + eveAPIDateFormat.format(interval.getEnd()) + "&" + 
			        EVEAPI.API_URL_PARAMETER_TYPE + this.getLabel() + "&" +
			        EVEAPI.API_URL_PARAMETER_FORMAT + EVEAPI.API_URL_PARAMETER_FORMAT_VALUES.JSON);
		} catch (MalformedURLException e) {
			Log.error("Something is wrong with the EVEAPI url", e);
		}
        return url;
	}
	public String toStringAlt(){
		String str = "baseUrl:" + this.baseUrl;
		str += "\ngroup:" + this.group.toString();
		str += "\nlabel:" + this.label;
		str += "\nunitLabel:" + this.unitLabel;
		str += "\nwarnLevels: [";
		for (Map.Entry<String, Double> entry : warnLevels.entrySet()) {
		    String key = entry.getKey();
		    Object value = entry.getValue();
		    str += key + ":";
		    str += value.toString()+",";
		}
		str += ']';
		return str;
	}
	public String toString(){
		return this.label;
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
	public double[] getErrorLevels() {
		return errorLevels;
	}
	public void setErrorLevels(double[] errorlevels) {
		this.errorLevels = errorlevels;
	}
}
