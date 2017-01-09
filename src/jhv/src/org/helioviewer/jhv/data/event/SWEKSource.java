package org.helioviewer.jhv.data.event;

import java.util.List;

public class SWEKSource {

    private final String sourceName;
    private final List<SWEKParameter> generalParameters;

    private final SWEKParser parser;
    private final SWEKDownloader downloader;

    public SWEKSource(String _sourceName, List<SWEKParameter> _generalParameters, SWEKParser _parser, SWEKDownloader _downloader) {
        sourceName = _sourceName;
        generalParameters = _generalParameters;
        parser = _parser;
        downloader = _downloader;
    }

    public String getSourceName() {
        return sourceName;
    }

    public SWEKParser getParser() {
        return parser;
    }

    public SWEKDownloader getDownloader() {
        return downloader;
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
