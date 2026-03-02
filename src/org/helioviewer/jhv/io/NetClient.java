package org.helioviewer.jhv.io;

import java.awt.EventQueue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import okio.BufferedSource;

public interface NetClient extends AutoCloseable {

    boolean isSuccessful();

    default InputStream getStream() {
        return getSource().inputStream();
    }

    default Reader getReader() {
        return new InputStreamReader(getStream(), StandardCharsets.UTF_8);
    }

    BufferedSource getSource();

    long getContentLength();

    @Override
    void close() throws IOException;

    enum NetCache {
        CACHE, NETWORK, BYPASS
    }

    static NetClient of(URI uri) throws IOException {
        return of(uri, false, NetCache.CACHE);
    }

    static NetClient of(URI uri, boolean allowError) throws IOException {
        return of(uri, allowError, NetCache.CACHE);
    }

    static NetClient of(URI uri, boolean allowError, NetCache cache) throws IOException {
        if (EventQueue.isDispatchThread())
            throw new IOException("Don't do that");

        return "file".equals(uri.getScheme()) ? new NetClientLocal(uri) : new NetClientRemote(uri, allowError, cache);
    }

}
