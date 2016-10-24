package org.helioviewer.jhv.data.datatype.event;

public class SWEKParameter {

    private final String parameterName;
    private final String parameterDisplayName;
    private final SWEKParameterFilter parameterFilter;
    private final boolean defaultVisible;

    public SWEKParameter(String parameterName, String parameterDisplayName, SWEKParameterFilter parameterFilter, boolean defaultVisible) {
        this.parameterName = parameterName;
        this.parameterDisplayName = parameterDisplayName;
        this.parameterFilter = parameterFilter;
        this.defaultVisible = defaultVisible;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterDisplayName() {
        return parameterDisplayName;
    }

    public SWEKParameterFilter getParameterFilter() {
        return parameterFilter;
    }

    public boolean isDefaultVisible() {
        return defaultVisible;
    }

}
