package org.helioviewer.jhv.base;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.helioviewer.jhv.Settings;

public class ProxySettings {

    public static final Proxy proxy;

    static {
        String[] httpVars = { "http.proxyHost", "http.proxyPort", "http.proxyUser", "http.proxyPassword" };
        String[] socksVars = { "socksProxyHost", "socksProxyPort", "java.net.socks.username", "java.net.socks.password" };

        Proxy _proxy = detectProxy(httpVars[0], httpVars[1], Proxy.Type.HTTP);
        if (_proxy == null)
            _proxy = detectProxy(socksVars[0], socksVars[1], Proxy.Type.SOCKS);
        if (_proxy == null)
            _proxy = Proxy.NO_PROXY;

        proxy = _proxy;

        if (proxy != Proxy.NO_PROXY) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String[] vars = proxy.type().equals(Proxy.Type.HTTP) ? httpVars : socksVars;
                    String host = System.getProperty(vars[0]);
                    if (getRequestingHost().equalsIgnoreCase(host)) {
                    // if (getRequestorType() == RequestorType.PROXY) {
                        String port = System.getProperty(vars[1]);
                        String user = System.getProperty(vars[2]);
                        String pass = System.getProperty(vars[3]);

                        if (user == null || pass == null) {
                            user = Settings.getSingletonInstance().getProperty("default.proxyUsername");
                            pass = Settings.getSingletonInstance().getProperty("default.proxyPassword");
                        }

                        if (user != null && pass != null && Integer.toString(getRequestingPort()).equals(port)) {
                            try {
                                return new PasswordAuthentication(user, new String(Base64.getDecoder().decode(pass), StandardCharsets.UTF_8).toCharArray());
                            } catch (Exception ignore) {
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    private static Proxy detectProxy(String host, String port, Proxy.Type type) {
        String proxyHost = System.getProperty(host);
        if (proxyHost != null) {
            try {
                int proxyPort = Integer.parseInt(System.getProperty(port));
                return new Proxy(type, InetSocketAddress.createUnresolved(proxyHost, proxyPort));
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public static void init() {}

}
