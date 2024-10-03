package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.JHVGlobals;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import okio.BufferedSink;
import okio.Okio;
import org.apache.tika.Tika;

public class NetFileCache {

    private static final LoadingCache<URI, DataUri> cache = Caffeine.newBuilder().softValues().
            build(uri -> {
                String scheme = uri.getScheme().toLowerCase();
                if ("jpip".equals(scheme) || "jpips".equals(scheme))
                    return new DataUri(uri, uri, null);
                if ("file".equals(scheme)) {
                    File file = new File(uri.getPath()); // for files with authority (//localhost) and Windows
                    return new DataUri(uri, uri, file);
                }

                Path path = Files.createTempFile(JHVGlobals.fileCacheDir.toPath(), "jhv", null);
                try (NetClient nc = NetClient.of(uri, false, NetClient.NetCache.BYPASS); BufferedSink sink = Okio.buffer(Okio.sink(path))) {
                    sink.writeAll(nc.getSource());
                }
                File file = path.toFile();
                return new DataUri(uri, path.toUri(), file);
            });

    public static DataUri get(@Nonnull URI uri) throws IOException {
        try {
            return cache.get(uri);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
