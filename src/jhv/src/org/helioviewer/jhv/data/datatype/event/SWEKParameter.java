package org.helioviewer.jhv.data.datatype.event;

/**
 * Describes a parameter of an event. A parameter has a source, a parameter name, a parameter display name,
 * a filter and a default visible indication.
 *
 * @author Bram Bourgoignie
 *
 */
public class SWEKParameter {

    /** The source from where this parameter comes*/
    private final String source;

    /**The name of the parameter*/
    private final String parameterName;

    /**The display name of the source*/
    private final String parameterDisplayName;

    /**The filter of this parameter*/
    private final SWEKParameterFilter parameterFilter;

    /** Is the parameter default visible*/
    private final boolean defaultVisible;

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
     * Gets the name of this parameter.
     *
     * @return the parameterName
     */
    public String getParameterName() {
        return parameterName;
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
     * Gets the filter of this parameter.
     *
     * @return the parameterFilter
     */
    public SWEKParameterFilter getParameterFilter() {
        return parameterFilter;
    }

    /**
     * Gets the default visibility.
     *
     * @return the defaultVisible   True if default visible, false if not default visible
     */
    public boolean isDefaultVisible() {
        return defaultVisible;
    }

}
