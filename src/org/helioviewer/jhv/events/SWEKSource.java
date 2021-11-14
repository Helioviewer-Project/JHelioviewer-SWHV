package org.helioviewer.jhv.events;

import java.util.List;

import javax.annotation.Nullable;

public record SWEKSource(String name, List<SWEKParameter> generalParameters, SWEKHandler handler) {

    @Nullable
    public SWEKParameter getParameter(String _name) {
        for (SWEKParameter parameter : generalParameters) {
            if (parameter.name().equalsIgnoreCase(_name)) {
                return parameter;
            }
        }
        return null;
    }

}
