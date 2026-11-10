package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

public class DataUri {

    private static final Tika tika = new Tika();

    private static Format detect(URI sourceUri, File file) throws IOException {
        String sourcePath = sourceUri.getPath();
        if (sourcePath != null) {
            sourcePath = sourcePath.toLowerCase();
            boolean gzip = sourcePath.endsWith(".gz");
            if (gzip)
                sourcePath = sourcePath.substring(0, sourcePath.length() - 3);
            if (gzip && (sourcePath.endsWith(".fits") || sourcePath.endsWith(".fit") || sourcePath.endsWith(".fts")))
                return Format.FITS;
            if (sourcePath.endsWith(".gltf") || sourcePath.endsWith(".glb"))
                return Format.GLTF;
        }

        return getFormat(tika.detect(file));
    }

    private static final Map<String, Format> map = Map.of(
            "application/x-jpp-stream", Format.JPIP,
            "image/jp2", Format.JP2,
            "image/jpx", Format.JPX,
            "application/fits", Format.FITS,
            "image/png", Format.PNG,
            "image/jpeg", Format.JPEG,
            "application/zip", Format.ZIP,
            "application/x-netcdf", Format.CDF,
            "text/csv", Format.CSV
    );

    private static Format getFormat(String spec) {
        Format f = map.get(spec);
        return f == null ? Format.UNKNOWN : f;
    }

    public enum Format {UNKNOWN, JPIP, JP2, JPX, FITS, PNG, JPEG, ZIP, GLTF, CDF, CSV}

    private final URI sourceUri;
    private final URI uri;
    private final Format format;
    private final File file;
    private final String baseName;

    DataUri(URI originalUri, URI cachedUri, File _file) throws IOException {
        sourceUri = originalUri;
        uri = cachedUri;
        file = _file;
        format = file == null ? Format.JPIP : detect(originalUri, file); // JPIP not backed by file
        baseName = FilenameUtils.getName(originalUri.toString());
    }

    public URI sourceUri() {
        return sourceUri;
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
