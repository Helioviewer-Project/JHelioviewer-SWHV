package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.URI;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

//import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.AppInfo;
import org.helioviewer.jhv.app.Log;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.HttpUrl;
//import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
//import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSource;

class NetClientRemote implements NetClient {

    static {
        Log.setLoggerLevel(OkHttpClient.class.getName(), Level.FINE);
    }

    //private static final HttpLoggingInterceptor logging = new HttpLoggingInterceptor(Log::info).setLevel(HttpLoggingInterceptor.Level.HEADERS);

    private static final int cacheSize = 1024 * 1024 * 1024;
    private static final CacheControl noStore = new CacheControl.Builder().noStore().build();
    /**
     * Try IPv4 before IPv6, and stop waiting a full minute on an address that will not answer.
     *
     * <p>Several of the archives publish an AAAA record for a host that a given network cannot
     * actually reach: umbra.nascom.nasa.gov resolves to both 198.118.248.134 and 2001:4d0:14:100::134,
     * and where the second has no route, a request that picks it spends the whole connect timeout
     * and then fails with "No route to host" rather than falling back. That made PUNCH loads fail
     * at random while the identical listing succeeded moments earlier, and left even the successes
     * taking eight to fourteen seconds for thirty kilobytes.
     *
     * <p>Ordering, not disabling: IPv6 is still there as a fallback for a network where it works,
     * and the shorter connect timeout means an address that is dead costs seconds instead of a
     * minute. The read timeout is untouched, since a slow archive is a different thing from an
     * unreachable one.
     */
    private static final Dns IPV4_FIRST = hostname -> Dns.SYSTEM.lookup(hostname).stream()
            .sorted(Comparator.comparingInt(a -> a instanceof Inet4Address ? 0 : 1))
            .toList();

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .dns(IPV4_FIRST)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .cache(new Cache(Directories.clientCacheDir, cacheSize))
            .proxyAuthenticator(Authenticator.JAVA_NET_AUTHENTICATOR)
            //.addInterceptor(logging)
            //.addInterceptor(new LoggingInterceptor())
            .build();

    private final ResponseBody responseBody;
    private final boolean isSuccessful;

    NetClientRemote(URI uri, boolean allowError, NetCache cache) throws IOException {
        this(uri, null, null, allowError, cache);
    }

    NetClientRemote(URI uri, String contentType, byte[] body, boolean allowError, NetCache cache) throws IOException {
        HttpUrl url = HttpUrl.get(uri);
        if (url == null)
            throw new IOException("Could not parse " + uri);

        Request.Builder builder = new Request.Builder().header("User-Agent", AppInfo.userAgent).url(url);
        if (cache == NetCache.NETWORK)
            builder.cacheControl(CacheControl.FORCE_NETWORK);
        else if (cache == NetCache.BYPASS)
            builder.cacheControl(noStore);
        if (body != null)
            builder.post(RequestBody.create(body, MediaType.get(contentType)));

        Request request = builder.build();
        Response response = client.newCall(request).execute();
        isSuccessful = response.isSuccessful();
        if (!allowError && !isSuccessful) {
            String msg = response.toString();
            response.close();
            throw new IOException(msg);
        }

        responseBody = response.body();
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
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
        responseBody.close();
    }
/*
    private static class LoggingInterceptor implements Interceptor {
        @Nonnull
        @Override
        public Response intercept(@Nonnull Chain chain) throws IOException {
            long t1 = System.nanoTime();
            Request r1 = chain.request();
            Log.info(String.format("Sending request %s on %s%n%s", r1.url(), chain.connection(), r1.headers()));

            Response r2 = chain.proceed(r1);
            long t2 = System.nanoTime();
            Log.info(String.format("Received response for %s in %.1fms", r1.url(), (t2 - t1) / 1e6d));

            Response r3 = r2.networkResponse();
            if (r3 != null)
                Log.info(String.format("Network headers %s:\n%s", r1.url(), r3.headers()));

            return r2;
        }
    }
*/
}
