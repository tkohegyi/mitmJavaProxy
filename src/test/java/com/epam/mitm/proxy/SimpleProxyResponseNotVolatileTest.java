package com.epam.mitm.proxy;

import com.epam.mitm.proxy.help.AbstractSimpleProxyTool;
import com.epam.mitm.proxy.help.DefaultRequestInterceptor;
import com.epam.mitm.proxy.help.DefaultResponseInterceptor;
import com.epam.mitm.proxy.help.ResponseInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests just a single basic proxy running as a man in the middle, with defining response as not volatile.
 */
public class SimpleProxyResponseNotVolatileTest extends AbstractSimpleProxyTool {

    @Override
    protected void setUp() {
        DefaultRequestInterceptor defaultRequestInterceptor = new DefaultRequestInterceptor(requestCount);
        DefaultResponseInterceptor defaultResponseInterceptor = new DefaultResponseInterceptor(responseCount);
        proxyServer.addRequestInterceptor(defaultRequestInterceptor);
        proxyServer.addResponseInterceptor(defaultResponseInterceptor);
        proxyServer.setCaptureBinaryContent(false);
        proxyServer.setCaptureContent(false);
        ProxyServer.setResponseVolatile(false); //!!
        ProxyServer.setShouldKeepSslConnectionAlive(true);
    }

    @Test
    public void testSimpleGetRequestNoTimeout() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, GET_QUICK_RESPONSE, true, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        Thread.sleep(1000);
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSNoTimeout() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, GET_QUICK_RESPONSE, true, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        Thread.sleep(1000);
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

}
