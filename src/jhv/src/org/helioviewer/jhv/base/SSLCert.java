package org.helioviewer.jhv.base;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

class SSLCert {

    static void addRootCA(String cert) throws Exception {
        Certificate ca = CertificateFactory.getInstance("X.509").generateCertificate(FileUtils.getResourceInputStream("/cert/" + cert));

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry(Integer.toString(1), ca);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);

        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
    }

}
