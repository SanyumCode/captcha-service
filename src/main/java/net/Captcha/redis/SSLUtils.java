package net.Captcha.redis;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public final class SSLUtils {
    private SSLUtils() {
    }

    /**
     * Uses the JVM's configured default truststore.
     */
    public static SSLSocketFactory defaultSslSocketFactory() {
        return HttpsURLConnection.getDefaultSSLSocketFactory();
    }

    /**
     * Creates a socket factory that trusts the supplied CA certificate.
     */
    public static SSLSocketFactory createSslSocketFactoryWithCa(Path caCertPath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (InputStream input = Files.newInputStream(caCertPath)) {
            caCert = (X509Certificate) cf.generateCertificate(input);
        }

        // 创建 KeyStore 并放入 CA
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("caCert", caCert);

        // 创建 TrustManager，信任我们放进去的 CA
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
        return sslContext.getSocketFactory();
    }

}
