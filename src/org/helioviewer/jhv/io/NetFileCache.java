package org.helioviewer.jhv.io;

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

public class NetFileCache {

    private static final LoadingCache<URI, URI> cache = Caffeine.newBuilder().softValues().
            build(uri -> {
                String scheme = uri.getScheme().toLowerCase();
                if (scheme.equals("jpip") || scheme.equals("jpips") || scheme.equals("file"))
                    return uri;

                Path path = Files.createTempFile(JHVGlobals.fileCacheDir.toPath(), "jhv", null);
                try (NetClient nc = NetClient.of(uri, false, NetClient.NetCache.BYPASS); BufferedSink sink = Okio.buffer(Okio.sink(path))) {
                    sink.writeAll(nc.getSource());
                }
                return path.toUri();
            });

    public static URI get(@Nonnull URI uri) throws IOException {
        try {
            return cache.get(uri);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
