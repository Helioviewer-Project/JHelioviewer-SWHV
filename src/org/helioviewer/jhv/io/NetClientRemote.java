package org.helioviewer.jhv.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//import javax.annotation.Nonnull;

import org.helioviewer.jhv.JHVGlobals;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
//import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
//import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSource;

//import java.lang.invoke.MethodHandles;
//import java.util.logging.Level;
//import java.util.logging.Logger;

class NetClientRemote implements NetClient {

    // private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    private static final Dispatcher dispatcher;

    static {
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
        dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(8);
    }

    //private static final HttpLoggingInterceptor logging = new HttpLoggingInterceptor(Log::info).setLevel(HttpLoggingInterceptor.Level.HEADERS);

    private static final int cacheSize = 1024 * 1024 * 1024;
    private static final CacheControl noStore = new CacheControl.Builder().noStore().build();
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(JHVGlobals.getConnectTimeout(), TimeUnit.MILLISECONDS)
            .readTimeout(JHVGlobals.getReadTimeout(), TimeUnit.MILLISECONDS)
            .cache(new Cache(JHVGlobals.clientCacheDir, cacheSize))
            .dispatcher(dispatcher)
            //.addInterceptor(logging)
            //.addInterceptor(new LoggingInterceptor())
            .build();

    private final ResponseBody responseBody;
    private final boolean isSuccessful;

    NetClientRemote(URI uri, boolean allowError, NetCache cache) throws IOException {
        HttpUrl url = HttpUrl.get(uri);
        if (url == null)
            throw new IOException("Could not parse " + uri);

        Request.Builder builder = new Request.Builder().header("User-Agent", JHVGlobals.userAgent).url(url);
        if (cache == NetCache.NETWORK)
            builder.cacheControl(CacheControl.FORCE_NETWORK);
        else if (cache == NetCache.BYPASS)
            builder.cacheControl(noStore);
        Request request = builder.build();
        //System.out.println(">>> " + url);

        Response response = client.newBuilder().build() // avoid spurious connection leaked messages for LMSAL
                .newCall(request).execute();
        isSuccessful = response.isSuccessful();
        if (!allowError && !isSuccessful) {
            String msg = response.toString();
            response.close();
            throw new IOException(msg);
        }
        responseBody = response.body();

        //if (response.cacheResponse() != null)
        //    System.out.println(">>> cached response: " + url);
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    public InputStream getStream() {
        return responseBody.byteStream();
    }

    @Override
    public Reader getReader() {
        return responseBody.charStream();
    }

    @Override
    public BufferedSource getSource() {
        return responseBody.source();
    }

    @Override
    public long getContentLength() {
        return responseBody.contentLength();
    }

    @Override
    public void close() {
        if (responseBody != null) {
            responseBody.close();
        }
    }
/*
    private static class LoggingInterceptor implements Interceptor {
        @Nonnull
        @Override
        public Response intercept(@Nonnull Chain chain) throws IOException {
            long t1 = System.nanoTime();
            Request r1 = chain.request();
            LOGGER.log(Level.INFO, String.format("Sending request %s on %s%n%s", r1.url(), chain.connection(), r1.headers()));

            Response r2 = chain.proceed(r1);
            long t2 = System.nanoTime();
            LOGGER.log(Level.INFO, String.format("Received response for %s in %.1fms", r1.url(), (t2 - t1) / 1e6d));

            Response r3 = r2.networkResponse();
            if (r3 != null)
                LOGGER.log(Level.INFO, String.format("Network headers %s:\n%s", r1.url(), r3.headers()));

            return r2;
        }
    }
*/
}
