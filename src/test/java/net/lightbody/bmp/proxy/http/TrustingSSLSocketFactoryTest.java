package net.lightbody.bmp.proxy.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class to test TrustingSSLSocketFactory class.
 */
public class TrustingSSLSocketFactoryTest {
    private TrustingSSLSocketFactory underTest;

    @BeforeEach
    public void setUp() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        underTest = new TrustingSSLSocketFactory(60000);
    }

    @Test
    public void testSslSocketFactoryCreation() {
        assertNotNull(underTest);
    }

}
