package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import okio.BufferedSource;

public interface NetClient extends AutoCloseable {

    boolean isSuccessful();
    InputStream getStream();
    Reader getReader();
    BufferedSource getSource();
    long getContentLength();
    void close() throws IOException;

    static NetClient of(String url) throws IOException {
        return of(new URL(url), false);
    }

    static NetClient of(URL url) throws IOException {
        return of(url, false);
    }

    static NetClient of(String url, boolean allowError) throws IOException {
        return of(new URL(url), allowError);
    }

    static NetClient of(URL url, boolean allowError) throws IOException {
        return "file".equals(url.getProtocol()) ? new NetClientLocal(url) : new NetClientRemote(url, allowError);
    }

}
