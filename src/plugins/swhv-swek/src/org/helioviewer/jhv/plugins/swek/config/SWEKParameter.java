/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

/**
 * Describes a parameter of an event. A parameter has a source, a parameter name, a parameter display name,
 * a filter and a default visible indication.
 *
 * @author Bram Bourgoignie
 *
 */
public class SWEKParameter {
    /** The source from where this parameter comes*/
    private String source;

    /**The name of the parameter*/
    private String parameterName;

    /**The display name of the source*/
    private String parameterDisplayName;

    /**The filter of this parameter*/
    private SWEKParameterFilter parameterFilter;

    /** Is the parameter default visible*/
    private boolean defaultVisible;

    /**
     * Creates a SWEK parameter with empty source name, parameter name, parameter display name, parameter filter and
     * false default visibility.
     */
    public SWEKParameter(){
        this.source = "";
        this.parameterName = "";
        this.parameterDisplayName = "";
        this.parameterFilter = null;
        this.defaultVisible = false;
    }

    /**
     * Creates a SWEL parameter for a given source with the given parameter name, display name, filter and default visibility.
     *
     * @param source                    The source from where the parameter is coming
     * @param parameterName             The name of the parameter
     * @param parameterDisplayName      The display name of the parameter
     * @param parameterFilter           The filter on the parameter of null is there iss no filter
     * @param defaultVisible            True is the parameter is default visible, false if not
     */
    public SWEKParameter(String source, String parameterName, String parameterDisplayName, SWEKParameterFilter parameterFilter, boolean defaultVisible) {
        super();
        this.source = source;
        this.parameterName = parameterName;
        this.parameterDisplayName = parameterDisplayName;
        this.parameterFilter = parameterFilter;
        this.defaultVisible = defaultVisible;
    }

    /**
     * Gets the source of this SWEK parameter.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source for this parameter.
     *
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the name of this parameter.
     *
     * @return the parameterName
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Sets the name of this parameter
     *
     * @param parameterName the parameterName to set
     */
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    /**
     * Gets the display name of this parameter.
     *
     * @return the parameterDisplayName
     */
    public String getParameterDisplayName() {
        return parameterDisplayName;
    }

    /**
     * Sets the display name of this parameter.
     *
     * @param parameterDisplayName the parameterDisplayName to set
     */
    public void setParameterDisplayName(String parameterDisplayName) {
        this.parameterDisplayName = parameterDisplayName;
    }

    /**
     * Gets the filter of this parameter.
     *
     * @return the parameterFilter
     */
    public SWEKParameterFilter getParameterFilter() {
        return parameterFilter;
    }

    /**
     * Sets the filter of this parameter.
     *
     * @param parameterFilter the parameterFilter to set
     */
    public void setParameterFilter(SWEKParameterFilter parameterFilter) {
        this.parameterFilter = parameterFilter;
    }

    /**
     * Gets the default visibility.
     *
     * @return the defaultVisible   True if default visible, false if not default visible
     */
    public boolean isDefaultVisible() {
        return defaultVisible;
    }

    /**
     * Sets the default visibility.
     *
     * @param defaultVisible True if default visible, false if not.
     */
    public void setDefaultVisible(boolean defaultVisible) {
        this.defaultVisible = defaultVisible;
    }
}
