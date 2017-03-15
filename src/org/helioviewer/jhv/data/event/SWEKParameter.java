package org.helioviewer.jhv.data.event;

public class SWEKParameter {

    private final String parameterName;
    private final String parameterDisplayName;
    private final SWEKParameterFilter parameterFilter;
    private final boolean defaultVisible;

    public SWEKParameter(String _parameterName, String _parameterDisplayName, SWEKParameterFilter _parameterFilter, boolean _defaultVisible) {
        parameterName = _parameterName;
        parameterDisplayName = _parameterDisplayName;
        parameterFilter = _parameterFilter;
        defaultVisible = _defaultVisible;
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
