package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.log.Log;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;

public class NetClient implements AutoCloseable {

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(JHVGlobals.getStdConnectTimeout(), TimeUnit.MILLISECONDS)
        .readTimeout(JHVGlobals.getStdReadTimeout(), TimeUnit.MILLISECONDS)
        //.addInterceptor(new LoggingInterceptor())
        .build();

    private final Request.Builder requestBuilder = new Request.Builder()
        .header("User-Agent", JHVGlobals.userAgent);

    private Response response;

    public NetClient(String url) throws IOException {
        this(new URL(url));
    }

    public NetClient(URL url) throws IOException {
        Request request = requestBuilder.url(url).build();
        response = client.newCall(request).execute();
        if (!response.isSuccessful())
            throw new IOException(response.toString());
    }

    public InputStream getStream() {
        return response.body().byteStream();
    }

    public Reader getReader() {
        return response.body().charStream();
    }

    public BufferedSource getSource() {
        return response.body().source();
    }

    public long getContentLength() {
        return response.body().contentLength();
    }

    @Override
    public void close() {
        response.close();
    }

    private static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            long t1 = System.nanoTime();
            Request r1 = chain.request();
            Log.info(String.format("Sending request %s on %s%n%s", r1.url(), chain.connection(), r1.headers()));

            Response r2 = chain.proceed(r1);
            long t2 = System.nanoTime();
            Log.info(String.format("Received response for %s in %.1fms%n%s", r1.url(), (t2 - t1) / 1e6d, r2.networkResponse().headers()));

            return r2;
        }
    }

}
