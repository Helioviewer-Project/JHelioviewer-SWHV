package org.helioviewer.jhv.io;

import java.awt.EventQueue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import okio.BufferedSource;

public interface NetClient extends AutoCloseable {

    boolean isSuccessful();
    InputStream getStream();
    Reader getReader();
    BufferedSource getSource();
    long getContentLength();
    @Override void close() throws IOException;

    static NetClient of(String uri) throws IOException {
        try {
            return of(new URI(uri), false);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    static NetClient of(URI uri) throws IOException {
        return of(uri, false);
    }

    static NetClient of(String uri, boolean allowError) throws IOException {
        try {
            return of(new URI(uri), allowError);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    static NetClient of(URI uri, boolean allowError) throws IOException {
        if (EventQueue.isDispatchThread())
            throw new IOException("Don't do that");

        return "file".equals(uri.getScheme()) ? new NetClientLocal(uri) : new NetClientRemote(uri, allowError);
    }

}
