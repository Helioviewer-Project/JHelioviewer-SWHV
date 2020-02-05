package org.helioviewer.jhv.events;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Strings;

public class SWEKSource {

    private final String name;
    private final List<SWEKParameter> generalParameters;
    private final SWEKHandler handler;

    public SWEKSource(String _name, List<SWEKParameter> _generalParameters, SWEKHandler _handler) {
        name = Strings.intern(_name);
        generalParameters = _generalParameters;
        handler = _handler;
    }

    public String getName() {
        return name;
    }

    public SWEKHandler getHandler() {
        return handler;
    }

    @Nullable
    public SWEKParameter getParameter(String _name) {
        for (SWEKParameter parameter : generalParameters) {
            if (parameter.getParameterName().equalsIgnoreCase(_name)) {
                return parameter;
            }
        }
        return null;
    }

}
