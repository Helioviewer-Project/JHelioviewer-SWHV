package org.helioviewer.jhv.event;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.helioviewer.jhv.base.Regex;

import org.apache.commons.validator.routines.UrlValidator;

public class EventParameter {

    private static final NumberFormat numFormatter = new DecimalFormat("0.###E0");

    // The name of the parameter
    private final String parameterName;
    // How the parameter is displayed
    private final String parameterDisplayName;
    // The value of the parameter
    private final String parameterValue;

    private String parameterDisplayValue;

    private String parameterSimpleDisplayValue;
    private Boolean url;

    public EventParameter(String _parameterName, String _parameterDisplayName, String _parameterValue) {
        parameterName = _parameterName.intern();
        parameterDisplayName = _parameterDisplayName.intern();
        parameterValue = _parameterValue.intern();
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterDisplayName() {
        return parameterDisplayName;
    }

    public String getParameterValue() {
        return parameterValue;
    }

    public String getSimpleDisplayParameterValue() {
        if (parameterSimpleDisplayValue == null)
            parameterSimpleDisplayValue = beautifyValue().intern();
        return parameterSimpleDisplayValue;
    }

    public String getDisplayParameterValue() {
        if (parameterDisplayValue == null) {
            parameterDisplayValue = getSimpleDisplayParameterValue();
            if (parameterName == "ar_noaanum")
                parameterDisplayValue = ("<a href=\"https://ui.adsabs.harvard.edu/#search/q=%22NOAA%20" +
                        parameterDisplayValue +
                        "%22&sort=date%20desc\">" +
                        parameterDisplayValue + "</a>").intern();
        }
        return parameterDisplayValue;
    }

    public boolean isUrl() {
        if (url == null)
            url = UrlValidator.getInstance().isValid(parameterValue);
        return url;
    }

    private String beautifyValue() {
        String value = parameterValue;
        if (Regex.FloatingPoint.matcher(value).matches() &&
                !Regex.Integer.matcher(value).matches()) {
            String result = numFormatter.format(Double.parseDouble(value));
            return result.contains("E0") ? result.substring(0, result.length() - 2) : result;
        } else {
            if (isUrl())
                return "<a href=\"" + value + "\">Open URL</a>";
            return value;
        }
    }

    @Override
    public String toString() {
        return parameterValue;
    }

}
