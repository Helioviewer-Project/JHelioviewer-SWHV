package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import okio.BufferedSource;
import okio.Okio;

class NetClientLocal implements NetClient {

    private final BufferedSource response;

    NetClientLocal(URI uri) throws IOException {
        try {
            response = Okio.buffer(Okio.source(new File(uri.getPath())));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isSuccessful() {
        return true;
    }

    @Override
    public InputStream getStream() {
        return response.inputStream();
    }

    @Override
    public Reader getReader() {
        return new InputStreamReader(response.inputStream(), StandardCharsets.UTF_8);
    }

    @Override
    public BufferedSource getSource() {
        return response;
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public void close() throws IOException {
        response.close();
    }

}
