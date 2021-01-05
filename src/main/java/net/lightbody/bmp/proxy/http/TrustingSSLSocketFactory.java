package net.lightbody.bmp.proxy.http;

import org.apache.http.HttpHost;
import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.java_bandwidthlimiter.StreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class TrustingSSLSocketFactory extends SSLConnectionSocketFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrustingSSLSocketFactory.class);
    private static KeyStore keyStore;
    private static String keyStorePassword;

    static {
        try {
//            keyStorePassword = "vvilma";
//            String keyStorePath = "/sslSupport/mitmProxy_keystore.jks";
            keyStorePassword = "password";
            String keyStorePath = "/sslSupport/cybervillainsCA.jks";
//            keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
//            String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
            if (keyStorePath != null) {
                InputStream fis = TrustingSSLSocketFactory.class.getResourceAsStream(keyStorePath);
                keyStore = KeyStore.getInstance("jks");
                keyStore.load(fis, keyStorePassword.toCharArray());
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            // JKS file not found, continue with keyStore == null
            LOGGER.warn("JKS file not found continue without keyStore", e);
        }
    }

    private final StreamManager streamManager;
    private final int timeout;

    public TrustingSSLSocketFactory(final HostNameResolver nameResolver, final StreamManager streamManager, int timeout) throws KeyManagementException,
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        super(
                SSLContexts.custom()
                        .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                        .loadTrustMaterial(null, (cert, authType) -> true) //trust strategy is here
                        .build(),
                new AllowAllHostnameVerifier()
        );

        assert streamManager != null;
        this.streamManager = streamManager;
        this.timeout = timeout;
    }

    //just an helper function to wrap a normal sslSocket into a simulated one so we can do throttling
    private Socket createSimulatedSocket(final Socket socket) {
        SimulatedSocketFactory.configure(socket);
        return new SimulatedSSLSocket(socket, streamManager, timeout);
    }

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        Socket sslSocket = super.createSocket(context);
        return createSimulatedSocket(sslSocket);
    }

    @Override
    public Socket connectSocket(
            final int connectTimeout,
            final Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpContext context) throws IOException {
        Socket sslSocket = super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);

        if (sslSocket instanceof SimulatedSSLSocket) {
            return sslSocket;
        }
        return createSimulatedSocket(sslSocket);
    }

    @Override
    public Socket createLayeredSocket(
            final Socket socket,
            final String target,
            final int port,
            final HttpContext context) throws IOException {
        Socket sslSocket = super.createLayeredSocket(socket, target, port, context);

        if (sslSocket instanceof SimulatedSSLSocket) {
            return sslSocket;
        }
        return createSimulatedSocket(sslSocket);
    }
}
