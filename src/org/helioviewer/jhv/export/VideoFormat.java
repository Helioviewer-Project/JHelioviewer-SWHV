package org.helioviewer.jhv.export;

import java.util.List;

public enum VideoFormat {

    H264("H.264 Good", List.of("-c:v", "libx264", "-profile:v", "high", "-level", "4.2", "-crf", "23", "-preset", "fast")),
    H264HQ("H.264 Better", List.of("-c:v", "libx264", "-profile:v", "high", "-level", "4.2", "-crf", "17", "-preset", "medium")),
    H265("H.265 Good", List.of("-c:v", "libx265", "-tag:v", "hvc1", "-crf", "28", "-preset", "fast")),
    H265HQ("H.265 Better", List.of("-c:v", "libx265", "-tag:v", "hvc1", "-crf", "22", "-preset", "medium"));

    private final String name;
    final List<String> settings;

    VideoFormat(String _name, List<String> _settings) {
        name = _name;
        settings = _settings;
    }

    @Override
    public String toString() {
        return name;
    }

}
