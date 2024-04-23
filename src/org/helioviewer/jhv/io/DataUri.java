package org.helioviewer.jhv.io;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public class DataUri {

    public enum Format {

        UNKNOWN, JPIP, JP2, JPX, FITS, PNG, JPEG, ZIP;

        private static final Map<String, Format> map = Map.of(
                "application/x-jpp-stream", JPIP,
                "image/jp2", JP2,
                "image/jpx", JPX,
                "application/fits", FITS,
                "image/png", PNG,
                "image/jpeg", JPEG,
                "application/zip", ZIP
        );

        static Format get(String spec) {
            Format f = map.get(spec);
            return f == null ? UNKNOWN : f;
        }

    }

    private final URI uri;
    private final Format format;
    private final File file;
    private final String baseName;

    DataUri(URI cachedUri, URI originalUri, Format _format) {
        uri = cachedUri;
        format = _format;
        file = format == Format.JPIP ? null : Path.of(uri).toFile();
        baseName = originalUri.getPath().split(".+?/(?=[^/]+$)")[1];
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
