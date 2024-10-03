package org.helioviewer.jhv.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.json.JSONObject;

public class DataUri {

    private static final Tika tika = new Tika();

    private static Format detect(File f) throws IOException {
        if (f.getPath().toLowerCase().endsWith(".fits.gz")) // hack
            return Format.Image.FITS;
        else
            return getFormat(f, tika.detect(f));
    }

    private static final Map<String, Format> map = Map.of(
            "application/x-jpp-stream", Format.Image.JPIP,
            "image/jp2", Format.Image.JP2,
            "image/jpx", Format.Image.JPX,
            "application/fits", Format.Image.FITS,
            "image/png", Format.Image.PNG,
            "image/jpeg", Format.Image.JPEG,
            "application/zip", Format.Image.ZIP,
            "application/x-netcdf", Format.Timeline.CDF,
            "text/csv", Format.Timeline.CSV
    );

    private static Format getFormat(File f, String spec) {
        if ("application/json".equals(spec)) {
            try (FileReader reader = new FileReader(f)) {
                JSONObject jo = JSONUtils.get(reader);
                if (jo.has("org.helioviewer.jhv.state"))
                    return Format.Json.STATE;
                if (jo.has("org.helioviewer.jhv.request.image") || jo.has("org.helioviewer.jhv.request.timeline"))
                    return Format.Json.REQUEST;
                if ("SunJSON".equals(jo.optString("type")))
                    return Format.Json.SUNJSON;
            } catch (Exception ignored) {
            }
            return null;
        }

        Format fmt = map.get(spec);
        return fmt == null ? Format.Unknown.UNKNOWN : fmt;
    }

    public interface Format {
        enum Unknown implements Format {UNKNOWN}

        enum Image implements Format {JPIP, JP2, JPX, FITS, PNG, JPEG, ZIP}

        enum Timeline implements Format {CDF, CSV}

        enum Json implements Format {STATE, REQUEST, SUNJSON}
    }

    private final URI uri;
    private final Format format;
    private final File file;
    private final String baseName;

    DataUri(URI originalUri, URI cachedUri, File _file) throws IOException {
        uri = cachedUri;
        file = _file;
        format = file == null ? Format.Image.JPIP : detect(file); // JPIP not backed by file
        baseName = FilenameUtils.getName(originalUri.toString());
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
