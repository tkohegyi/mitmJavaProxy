package net.lightbody.bmp.proxy.http;

import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
    private final static String DEFAULT_PASSWORD = "password";
    private final static String DEFAULT_KEYSTORE_PATH = "/sslSupport/cybervillainsCA.jks";
    private static KeyStore keyStore;
    private static String keyStorePassword;

    static {
        try {
        	
/* **** kept original hard-coded values here in case they are needed by original author for convenience
 *         	
            keyStorePassword = "vvilma";
            String keyStorePath = "/sslSupport/mitmProxy_keystore.jks";
            keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
            String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
       	    String keyStorePath = "/sslSupport/cybervillainsCA.jks";
*/       	            	
        	
        	// obtains the obfuscated password from system property, but has a default if the property is not present
        	// now only the encoded value is stored as an attribute, somewhat preventing exposure in case of memory dump
            keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword", DEFAULT_PASSWORD);
            
            
            //obtains the keyStorePath from system property, but has a default if property is not present
            String keyStorePath = System.getProperty("javax.net.ssl.keyStorePath", DEFAULT_KEYSTORE_PATH);;


            if (keyStorePath != null) {
                InputStream fis = TrustingSSLSocketFactory.class.getResourceAsStream(keyStorePath);
                keyStore = KeyStore.getInstance("jks");
                keyStore.load(fis, decodeKeyStorePassword(keyStorePassword).toCharArray());
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            // JKS file not found, continue with keyStore == null
            LOGGER.warn("JKS file not found continue without keyStore", e);
        }
    }

    private final int timeout;

    public TrustingSSLSocketFactory(int timeout) throws KeyManagementException,
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        super(
                SSLContexts.custom()
                        //.loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                		.loadKeyMaterial(keyStore, decodeKeyStorePassword(keyStorePassword).toCharArray())
                        .loadTrustMaterial(null, (cert, authType) -> true) //trust strategy is here
                        .build(),
                new NoopHostnameVerifier()
        );

        this.timeout = timeout;
    }

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        Socket sslSocket = super.createSocket(context);
        sslSocket.setSoTimeout(timeout);
        configureSocket(sslSocket);
        return sslSocket;
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
        return sslSocket;
    }

    @Override
    public Socket createLayeredSocket(
            final Socket socket,
            final String target,
            final int port,
            final HttpContext context) throws IOException {
        Socket sslSocket = super.createLayeredSocket(socket, target, port, context);
        configureSocket(sslSocket);
        return sslSocket;
    }

    private void configureSocket(Socket socket) {
        // Configure the socket to be Load Test Friendly!
        // If we don't set these, we can easily use up too many sockets, even when we're cleaning/closing the sockets
        // responsibly. The reason is that they will stick around in TIME_WAIT for some time (ie: 1-4 minutes) and once
        // they get to 64K (on Linux) or 16K (on Mac) we can't make any more requests. While those limits can be raised
        // with a configuration setting in the OS, we really don't need to change things globally. We just need to make
        // sure that when we close a socket it gets ditched right away and doesn't stick around in TIME_WAIT.
        //
        // This problem is most easily noticable/problematic for load tests that use a single transaction to issue
        // one HTTP request and then end the transaction, thereby shutting down the HTTP socket. This can easily create
        // 64K+ sockets in TIME_WAIT state, preventing any other requests from going out and producing a false-negative
        // "connection refused" error message.
        //
        // For further reading, check out HttpClient's FAQ on this subject:
        // http://wiki.apache.org/HttpComponents/FrequentlyAskedConnectionManagementQuestions
        try {
            socket.setReuseAddress(true);
            socket.setSoLinger(true, 0);
        } catch (Exception e) {
            //this is fine not to do anything here
        }
    }
    
    private static final String decodeKeyStorePassword(final String encodedKeyStorePassword) {
        // this requires that passwords set as a system property are Base64 encoded 
        // maybe a little better than having them in plain view
        // might want to upgrade this to use better encryption
    	
        
    	String passwordToReturn = encodedKeyStorePassword;
    	
    	//if it is not the default then decode it
        if(! DEFAULT_PASSWORD.equals(encodedKeyStorePassword)) {
        	Base64Encoder b64 = new Base64Encoder();
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	try {
        		b64.decode(encodedKeyStorePassword, baos);
        	}
        	catch(IOException io) {
        		throw new RuntimeException("Issue decoding keyStorePassword.  Please check that the value is set and properly encoded.", io);
        	}
        	passwordToReturn = new String(baos.toByteArray()).trim();
        }
        
        return passwordToReturn;
    }

}
