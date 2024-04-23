package org.helioviewer.jhv.io;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public class DataUri {

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

    private final URI uri;
    private final Format format;
    private final File file;
    private final String baseName;

    public DataUri(URI _uri, Format _format) {
        uri = _uri;
        format = _format;
        file = format == Format.JPIP ? null : Path.of(uri).toFile();

        String path = uri.getPath();
        baseName = path.substring(Math.max(0, path.lastIndexOf('/')));
    }

    public URI uri() {
        return uri;
    }

    public Format format() {
        return format;
    }

    public File file() {
        return file;
    }

    public String baseName() {
        return baseName;
    }

    @Override
    public String toString() {
        return uri.toString();
    }

}
