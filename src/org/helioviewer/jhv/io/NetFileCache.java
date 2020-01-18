package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.JHVGlobals;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import okio.BufferedSink;
import okio.Okio;

public class NetFileCache {

    private static final LoadingCache<URI, URI> cache = CacheBuilder.newBuilder().softValues().
            build(new CacheLoader<>() {
                @Override
                public URI load(@Nonnull URI uri) throws IOException {
                    String scheme = uri.getScheme().toLowerCase();
                    if (scheme.equals("jpip") || scheme.equals("file"))
                        return uri;

                    File f = File.createTempFile("jhv", null, JHVGlobals.fileCacheDir);
                    try (NetClient nc = NetClient.of(uri, false, NetClient.NetCache.BYPASS); BufferedSink sink = Okio.buffer(Okio.sink(f))) {
                        sink.writeAll(nc.getSource());
                    }
                    return f.toURI();
                }
            });

    public static URI get(@Nonnull URI uri) throws IOException {
        try {
            return cache.get(uri);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

}
