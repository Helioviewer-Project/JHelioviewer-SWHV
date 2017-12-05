package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
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

class NetClientRemote implements NetClient {

    //static {
    //    Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    //}

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(JHVGlobals.getStdConnectTimeout(), TimeUnit.MILLISECONDS)
        .readTimeout(JHVGlobals.getStdReadTimeout(), TimeUnit.MILLISECONDS)
        //.addInterceptor(new LoggingInterceptor())
        .build();

    private final Response response;

    NetClientRemote(URL url, boolean allowError) throws IOException {
        Request request = new Request.Builder().header("User-Agent", JHVGlobals.userAgent).url(url).build();
        response = client.newCall(request).execute();
        if (!allowError && !response.isSuccessful()) {
            response.close();
            throw new IOException(response.toString());
        }
    }

    @Override
    public boolean isSuccessful() {
        return response.isSuccessful();
    }

    @Override
    public InputStream getStream() {
        return response.body().byteStream();
    }

    @Override
    public Reader getReader() {
        return response.body().charStream();
    }

    @Override
    public BufferedSource getSource() {
        return response.body().source();
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
