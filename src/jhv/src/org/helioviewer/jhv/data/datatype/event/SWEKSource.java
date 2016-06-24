package org.helioviewer.jhv.data.datatype.event;

import java.util.List;

public class SWEKSource {
    private final String sourceName;
    private final String providerName;
    private final List<SWEKParameter> generalParameters;

    public SWEKSource(String _sourceName, String _providerName, List<SWEKParameter> _generalParameters) {
        sourceName = _sourceName;
        providerName = _providerName;
        generalParameters = _generalParameters;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getProviderName() {
        return providerName;
    }

    public SWEKParameter getParameter(String name) {
        for (SWEKParameter parameter : generalParameters) {
            if (parameter.getParameterName().equalsIgnoreCase(name)) {
                return parameter;
            }
        }
        return null;
    }

}
