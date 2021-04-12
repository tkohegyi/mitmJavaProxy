package website.magyar.mitm.proxy;

import org.junit.Test;
import website.magyar.mitm.proxy.help.AbstractSimpleProxyTool;
import website.magyar.mitm.proxy.help.DefaultRequestInterceptor;
import website.magyar.mitm.proxy.help.DefaultResponseInterceptor;
import website.magyar.mitm.proxy.help.ResponseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;

/**
 * Tests just a single basic proxy running as a man in the middle, with defining response as not volatile.
 */
public class SimpleProxyResponseNotVolatileTest extends AbstractSimpleProxyTool {
    private final Logger logger = LoggerFactory.getLogger(SimpleProxyResponseNotVolatileTest.class);

    @Override
    protected void setUp() {
        DefaultRequestInterceptor defaultRequestInterceptor = new DefaultRequestInterceptor(requestCount);
        DefaultResponseInterceptor defaultResponseInterceptor = new DefaultResponseInterceptor(responseCount);
        proxyServer.addRequestInterceptor(defaultRequestInterceptor);
        proxyServer.addResponseInterceptor(defaultResponseInterceptor);
        proxyServer.setCaptureBinaryContent(false);
        proxyServer.setCaptureContent(false);
        ProxyServer.setResponseVolatile(false); //!!
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
        try {
            ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, GET_QUICK_RESPONSE, true, false);
            assertEquals(200, proxiedResponse.getStatusCode());
            assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        } catch (SSLException | IndexOutOfBoundsException e) {
            logger.error("Ups", e);
            throw e;
        }
        Thread.sleep(1000);
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

}
