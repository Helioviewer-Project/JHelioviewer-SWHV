package org.helioviewer.jhv.data.datatype.event;

public class JHVEventParameter {
    /** The name of the parameter */
    private String parameterName;

    /** How the parameter is displayed */
    private String parameterDisplayName;

    /** The value of the parameter */
    private String parameterValue;

    /**
     * Default constructor.
     * 
     */
    public JHVEventParameter() {
        parameterName = "";
        parameterDisplayName = "";
        parameterValue = "";
    }

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
        this.parameterName = parameterName;
        this.parameterDisplayName = parameterDisplayName;
        this.parameterValue = parameterValue;
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
     * Sets the parameter name.
     * 
     * @param parameterName
     *            the name of the parameter
     */
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
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
     * Sets the parameter display name.
     * 
     * @param parameterDisplayName
     *            the display name of the parameter
     */
    public void setParameterDisplayName(String parameterDisplayName) {
        this.parameterDisplayName = parameterDisplayName;
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
     * Sets the value of the parameter.
     * 
     * @param parameterValue
     *            the value of the parameter
     */
    public void setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
    }

}
