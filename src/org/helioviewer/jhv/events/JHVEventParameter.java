package org.helioviewer.jhv.events;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.helioviewer.jhv.base.Regex;

import org.apache.commons.validator.routines.UrlValidator;

public class JHVEventParameter {

    private static final NumberFormat numFormatter = new DecimalFormat("0.###E0");

    // The name of the parameter
    private final String parameterName;
    // How the parameter is displayed
    private final String parameterDisplayName;
    // The value of the parameter
    private final String parameterValue;

    private String parameterDisplayValue;

    private String parameterSimpleDisplayValue;

    /**
     * Creates a JHVEvent parameter with a parameter name, parameter display
     * name and parameter value.
     *
     * @param _parameterName        the parameter name
     * @param _parameterDisplayName the parameter display name
     * @param _parameterValue       the parameter value
     */
    public JHVEventParameter(String _parameterName, String _parameterDisplayName, String _parameterValue) {
        parameterName = _parameterName.intern();
        parameterDisplayName = _parameterDisplayName.intern();
        parameterValue = _parameterValue.intern();
    }

    /**
     * Gets the parameter name.
     *
     * @return the name of the parameter
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Gets the parameter display name.
     *
     * @return the parameter display name
     */
    public String getParameterDisplayName() {
        return parameterDisplayName;
    }

    /**
     * Gets the value of the parameter.
     *
     * @return the value of the parameter
     */
    public String getParameterValue() {
        return parameterValue;
    }

    public String getSimpleDisplayParameterValue() {
        if (parameterSimpleDisplayValue == null)
            parameterSimpleDisplayValue = beautifyValue(parameterValue).intern();
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

    private static String beautifyValue(String value) {
        if (Regex.FloatingPoint.matcher(value).matches() &&
                !Regex.Integer.matcher(value).matches()) {
            String result = numFormatter.format(Double.parseDouble(value));
            return result.contains("E0") ? result.substring(0, result.length() - 2) : result;
        } else {
            if (UrlValidator.getInstance().isValid(value))
                return "<a href=\"" + value + "\">Open URL</a>";
            return value;
        }
    }

    @Override
    public String toString() {
        return parameterValue;
    }

}
