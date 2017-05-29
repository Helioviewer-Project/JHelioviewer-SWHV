package org.helioviewer.jhv.data.event;

import java.util.List;

public class SWEKSource {

    private final String name;
    private final List<SWEKParameter> generalParameters;

    private final SWEKParser parser;
    private final SWEKDownloader downloader;

    public SWEKSource(String _name, List<SWEKParameter> _generalParameters, SWEKParser _parser, SWEKDownloader _downloader) {
        name = _name.intern();
        generalParameters = _generalParameters;
        parser = _parser;
        downloader = _downloader;
    }

    public String getName() {
        return name;
    }

    public SWEKParser getParser() {
        return parser;
    }

    public SWEKDownloader getDownloader() {
        return downloader;
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
