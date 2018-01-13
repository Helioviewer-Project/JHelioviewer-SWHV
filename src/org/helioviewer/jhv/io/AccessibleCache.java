package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.JHVGlobals;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.BaseEncoding;
import okio.BufferedSink;
import okio.Okio;

public class AccessibleCache {

    private static final LoadingCache<URI, URI> cache = CacheBuilder.newBuilder().maximumSize(10000).
        build(new CacheLoader<URI, URI>() {
            @Override
            public URI load(URI uri) throws IOException {
                String out = BaseEncoding.base64Url().encode(uri.toString().getBytes(StandardCharsets.UTF_8));
                File f = new File(JHVGlobals.JP2CacheDir, out);
                try (NetClient nc = NetClient.of(uri, false, true); BufferedSink sink = Okio.buffer(Okio.sink(f))) {
                    sink.writeAll(nc.getSource());
                }
                return f.toURI();
            }
    });

    public static URI get(URI uri) throws IOException {
        try {
            return cache.get(uri);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

}
