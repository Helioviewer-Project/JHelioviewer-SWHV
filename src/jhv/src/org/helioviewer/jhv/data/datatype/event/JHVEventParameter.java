package org.helioviewer.jhv.data.datatype.event;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.helioviewer.jhv.base.Regex;

public class JHVEventParameter {
    /** The name of the parameter */
    private final String parameterName;

    /** How the parameter is displayed */
    private final String parameterDisplayName;

    /** The value of the parameter */
    private final String parameterValue;

    private String parameterDisplayValue;

    /**
     * Creates a JHVEvent parameter with a parameter name, parameter display
     * name and parameter value.
     *
     * @param parameterName
     *            the parameter name
     * @param parameterDisplayName
     *            the parameter display name
     * @param parameterValue
     *            the parameter value
     */
    public JHVEventParameter(String parameterName, String parameterDisplayName, String parameterValue) {
        this.parameterName = parameterName.intern();
        this.parameterDisplayName = parameterDisplayName.intern();
        this.parameterValue = parameterValue.intern();
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

    /**
     * Gets the parameter display value
     */
    public String getDisplayParameterValue() {
        if (parameterDisplayValue == null)
            parameterDisplayValue = beautifyValue(parameterValue).intern();
        return parameterDisplayValue;
    }

    private String beautifyValue(String value) {
        if (Regex.FloatingPoint.matcher(value).matches() &&
            !Regex.Integer.matcher(value).matches()) {
            NumberFormat formatter = new DecimalFormat("0.###E0");
            String result = formatter.format(Double.parseDouble(value));
            if (result.contains("E0")) {
                return result.substring(0, result.length() - 2);
            } else {
                return result;
            }
        } else {
            return value;
        }
    }

    @Override
    public String toString() {
        return parameterValue;
    }

}
