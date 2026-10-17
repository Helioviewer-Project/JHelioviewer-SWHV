package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.annotation.Nullable;
import javax.net.SocketFactory;

import org.helioviewer.jhv.app.Settings;

import okhttp3.Challenge;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class ProxySettings {

    // OkHttp uses this for DIRECT and HTTP routes. A plain Socket would consult
    // SOCKS settings again, overriding the route already chosen by the selector.
    private static final SocketFactory directSockets = new SocketFactory() {
        @Override
        public Socket createSocket() {
            return new Socket(Proxy.NO_PROXY);
        }

        @Override
        public Socket createSocket(String host, int port) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(InetAddress host, int port) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress local, int localPort) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort) {
            throw new UnsupportedOperationException();
        }
    };

    public static void init() {
        // JPIP's HTTP CONNECT uses Java's tunneling authentication. Permit Basic
        // by default for a configured HTTP proxy, while honoring explicit JVM options.
        if (System.getProperty("jdk.http.auth.tunneling.disabledSchemes") == null)
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        Authenticator.setDefault(new Authenticator() {
            @Nullable
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // SOCKS5 uses the legacy Authenticator call, which reports SERVER.
                // Otherwise only answer proxy challenges, never origin-server ones.
                boolean socks = "SOCKS5".equalsIgnoreCase(getRequestingProtocol());
                if (!socks && getRequestorType() != RequestorType.PROXY)
                    return null;
                String user = System.getProperty(socks ? "java.net.socks.username" : "http.proxyUser");
                String password = System.getProperty(socks ? "java.net.socks.password" : "http.proxyPassword");
                if (user == null || password == null) {
                    user = Settings.getProperty("proxy.username");
                    password = getPassword();
                }
                return user == null || password == null ? null : new PasswordAuthentication(user, password.toCharArray());
            }
        });
    }

    static OkHttpClient.Builder configure(OkHttpClient.Builder builder) {
        return builder.socketFactory(directSockets).proxyAuthenticator(ProxySettings::authenticateProxy);
    }

    public static Socket connectJpipSocket(URI destination, int connectTimeout, int readTimeout) throws IOException {
        ProxySelector selector = ProxySelector.getDefault();
        if (selector == null)
            return connect(Proxy.NO_PROXY, destination, connectTimeout, readTimeout);
        IOException failure = null;
        for (Proxy proxy : selector.select(destination)) {
            try {
                return connect(proxy, destination, connectTimeout, readTimeout);
            } catch (IOException e) {
                failure = e;
                if (proxy.type() != Proxy.Type.DIRECT)
                    selector.connectFailed(destination, proxy.address(), e);
            }
        }
        if (failure != null)
            throw failure;
        throw new IOException("No connection route for " + destination);
    }

    private static Socket connect(Proxy proxy, URI destination, int connectTimeout, int readTimeout) throws IOException {
        Socket socket = new Socket(proxy);
        try {
            socket.setReceiveBufferSize(Math.max(262144 * 8, 2 * socket.getReceiveBufferSize()));
            socket.setTrafficClass(0x10);
            // HTTP CONNECT needs the protocol's read timeout during connect.
            socket.setSoTimeout(readTimeout);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(destination.getHost(), destination.getPort()), connectTimeout);
            return socket;
        } finally {
            if (!socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    @Nullable
    private static Request authenticateProxy(@Nullable Route route, Response response) {
        if (route == null || !(route.proxy().address() instanceof InetSocketAddress address)
                || response.request().header("Proxy-Authorization") != null)
            return null;

        HttpUrl url = response.request().url();
        for (Challenge challenge : response.challenges()) {
            if (!"Basic".equalsIgnoreCase(challenge.scheme()))
                continue;
            // The selector supplies unresolved proxy addresses. OkHttp's built-in
            // authenticator fails when their getAddress() returns null.
            PasswordAuthentication credentials = Authenticator.requestPasswordAuthentication(
                    address.getHostString(), address.getAddress(), address.getPort(), url.scheme(),
                    challenge.realm(), challenge.scheme(), url.url(), Authenticator.RequestorType.PROXY);
            if (credentials != null)
                return response.request().newBuilder().header("Proxy-Authorization",
                        Credentials.basic(credentials.getUserName(), new String(credentials.getPassword()), challenge.charset())).build();
        }
        return null;
    }

    @Nullable
    public static String getPassword() {
        String password = Settings.getProperty("proxy.password");
        if (password == null)
            return null;
        try {
            return new String(Base64.getDecoder().decode(password), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    public static void setPassword(String password) {
        Settings.setProperty("proxy.password", Base64.getEncoder().withoutPadding().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
    }

}
