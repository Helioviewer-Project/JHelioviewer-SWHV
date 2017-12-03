package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

//import java.util.logging.Level;
//import java.util.logging.Logger;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.log.Log;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import okio.Okio;

public class NetClient implements AutoCloseable {

    //static {
    //    Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    //}

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(JHVGlobals.getStdConnectTimeout(), TimeUnit.MILLISECONDS)
        .readTimeout(JHVGlobals.getStdReadTimeout(), TimeUnit.MILLISECONDS)
        //.addInterceptor(new LoggingInterceptor())
        .build();

    private final Response remote;
    private final BufferedSource local;

    public NetClient(String url) throws IOException {
        this(new URL(url), false);
    }

    public NetClient(URL url) throws IOException {
        this(url, false);
    }

    public NetClient(String url, boolean allowError) throws IOException {
        this(new URL(url), allowError);
    }

    public NetClient(URL url, boolean allowError) throws IOException {
        if ("file".equals(url.getProtocol())) {
            local = Okio.buffer(Okio.source(new File(url.getPath())));
            remote = null;
            return;
        }

        Request request = new Request.Builder().header("User-Agent", JHVGlobals.userAgent).url(url).build();
        remote = client.newCall(request).execute();
        if (!allowError && !remote.isSuccessful())
            throw new IOException(remote.toString());
        local = null;
    }

    public boolean isSuccessful() {
        return remote.isSuccessful();
    }

    public InputStream getStream() {
        return local == null ? remote.body().byteStream() : local.inputStream();
    }

    public Reader getReader() {
        return local == null ? remote.body().charStream() : new InputStreamReader(local.inputStream(), StandardCharsets.UTF_8);
    }

    public BufferedSource getSource() {
        return local == null ? remote.body().source() : local;
    }

    public long getContentLength() {
        return remote.body().contentLength();
    }

    @Override
    public void close() throws IOException {
        if (local != null)
            local.close();
        if (remote != null)
            remote.close();
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
