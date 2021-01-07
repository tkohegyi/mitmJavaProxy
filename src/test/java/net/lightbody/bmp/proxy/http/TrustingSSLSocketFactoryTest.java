package net.lightbody.bmp.proxy.http;

import org.junit.Before;
import org.junit.Test;

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
        underTest = new TrustingSSLSocketFactory(60000);
    }

    @Test
    public void testSslSocketFactoryCreation() {
        assertNotNull(underTest);
    }

}
