package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetStream implements AutoCloseable {

    private static final OkHttpClient client = new OkHttpClient();

    private Response response;

    public NetStream(String url) throws IOException {
        call(new Request.Builder().url(url).build());
    }

    public NetStream(URL url) throws IOException {
        call(new Request.Builder().url(url).build());
    }

    private void call(Request request) throws IOException {
        response = client.newCall(request).execute();
    }

    public InputStream getInput() {
        return response.body().byteStream();
    }

    public Reader getReader() {
        return response.body().charStream();
    }

    public long getContentLength() {
        return response.body().contentLength();
    }

    @Override
    public void close() {
        response.close();
    }

}
