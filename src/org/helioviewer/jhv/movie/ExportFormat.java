package org.helioviewer.jhv.movie;

import java.util.List;

import org.helioviewer.jhv.app.Settings;

public enum ExportFormat {
    H264("H.264", ".mp4", List.of("-c:v", "libx264", "-profile:v", "high", "-crf", "23", "-preset", "fast",
            "-x264-params", "colorprim=bt709:transfer=bt709:colormatrix=bt709:fullrange=on")),
    H264HQ("H.264 better", ".mp4", List.of("-c:v", "libx264", "-profile:v", "high", "-crf", "17", "-preset", "medium",
            "-x264-params", "colorprim=bt709:transfer=bt709:colormatrix=bt709:fullrange=on")),
    H265("H.265", ".mp4", List.of("-c:v", "libx265", "-tag:v", "hvc1", "-crf", "23", "-preset", "fast",
            "-x265-params", "colorprim=bt709:transfer=bt709:colormatrix=bt709:range=full")),
    H265HQ("H.265 better", ".mp4", List.of("-c:v", "libx265", "-tag:v", "hvc1", "-crf", "17", "-preset", "medium",
            "-x265-params", "colorprim=bt709:transfer=bt709:colormatrix=bt709:range=full")),
    PNG("PNG series", "-%04d.png", List.of("-r", "1"));

    private final String name;
    final String extension;
    final List<String> settings;

    ExportFormat(String _name, String _extension, List<String> _settings) {
        name = _name;
        extension = _extension;
        settings = _settings;
    }

    public static ExportFormat fromSettings() {
        try {
            return valueOf(Settings.getProperty("video.format"));
        } catch (Exception ignore) {
            return H264;
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
