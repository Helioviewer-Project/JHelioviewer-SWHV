package org.helioviewer.jhv.data.event;

import java.util.List;

public class SWEKSource {

    private final String name;
    private final List<SWEKParameter> generalParameters;
    private final SWEKHandler handler;

    public SWEKSource(String _name, List<SWEKParameter> _generalParameters, SWEKHandler _handler) {
        name = _name.intern();
        generalParameters = _generalParameters;
        handler = _handler;
    }

    public String getName() {
        return name;
    }

    public SWEKHandler getHandler() {
        return handler;
    }

    public SWEKParameter getParameter(String _name) {
        for (SWEKParameter parameter : generalParameters) {
            if (parameter.getParameterName().equalsIgnoreCase(_name)) {
                return parameter;
            }
        }
        return null;
    }

}
