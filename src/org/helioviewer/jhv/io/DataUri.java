package org.helioviewer.jhv.io;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public record DataUri(URI uri, Format format) {

    public enum Format {

        UNKNOWN, JPIP, JPEG2000, FITS, PNG, JPEG, ZIP;

        private static final Map<String, Format> map = Map.of(
                "application/x-jpp-stream", JPIP,
                "image/jp2", JPEG2000,
                "image/jpx", JPEG2000,
                "application/fits", FITS,
                "image/png", PNG,
                "image/jpeg", JPEG,
                "application/zip", ZIP
        );

        public static Format get(String spec) {
            Format f = map.get(spec);
            return f == null ? UNKNOWN : f;
        }

    }

    public File toFile() {
        return Path.of(uri).toFile();
    }

}
