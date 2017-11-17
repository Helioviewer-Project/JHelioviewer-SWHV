package org.helioviewer.jhv.data.event;

import java.util.List;

public class SWEKSource {

    private final String name;
    private final List<SWEKParameter> generalParameters;

    private final SWEKParser parser;
    private final SWEKHandler handler;

    public SWEKSource(String _name, List<SWEKParameter> _generalParameters, SWEKParser _parser, SWEKHandler _handler) {
        name = _name.intern();
        generalParameters = _generalParameters;
        parser = _parser;
        handler = _handler;
    }

    public String getName() {
        return name;
    }

    public SWEKParser getParser() {
        return parser;
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
