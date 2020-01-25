package org.helioviewer.jhv.export;

import java.util.List;

public enum VideoFormat {

    H264("H.264 Good", ".mp4", List.of("-c:v", "libx264", "-profile:v", "high", "-level", "4.2", "-crf", "23", "-preset", "fast")),
    H264HQ("H.264 Better", ".mp4", List.of("-c:v", "libx264", "-profile:v", "high", "-level", "4.2", "-crf", "17", "-preset", "medium")),
    H265("H.265 Good", ".mp4", List.of("-c:v", "libx265", "-tag:v", "hvc1", "-crf", "28", "-preset", "fast")),
    H265HQ("H.265 Better", ".mp4", List.of("-c:v", "libx265", "-tag:v", "hvc1", "-crf", "22", "-preset", "medium")),
    PNG("PNG series", "-%04d.png", List.of("-r", "1"));

    private final String name;
    final String extension;
    final List<String> settings;

    VideoFormat(String _name, String _extension, List<String> _settings) {
        name = _name;
        extension = _extension;
        settings = _settings;
    }

    @Override
    public String toString() {
        return name;
    }

}
