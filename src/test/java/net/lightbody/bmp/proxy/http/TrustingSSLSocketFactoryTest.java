package net.lightbody.bmp.proxy.http;

import org.java_bandwidthlimiter.BandwidthLimiter;
import org.java_bandwidthlimiter.StreamManager;
import org.junit.Before;
import org.junit.Test;
import org.xbill.DNS.Cache;
import org.xbill.DNS.DClass;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import static org.junit.Assert.assertNotNull;

/**
 * Test class to test TrustingSSLSocketFactory class.
 */
public class TrustingSSLSocketFactoryTest {
    private TrustingSSLSocketFactory underTest;

    @Before
    public void setUp() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        StreamManager streamManager = new StreamManager(100 * BandwidthLimiter.OneMbps);
        BrowserMobHostNameResolver hostNameResolver = new BrowserMobHostNameResolver(new Cache(DClass.ANY));
        underTest = new TrustingSSLSocketFactory(hostNameResolver, streamManager, 60000);
    }

    @Test
    public void testSslSocketFactoryCreation() throws Exception {
        assertNotNull(underTest);
    }

}
