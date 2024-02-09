package website.magyar.mitm.proxy;

import org.apache.http.HttpHost;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import website.magyar.mitm.proxy.help.AbstractComplexProxyTool;
import website.magyar.mitm.proxy.help.ContentEncoding;
import website.magyar.mitm.proxy.help.DefaultRequestInterceptor;
import website.magyar.mitm.proxy.help.DefaultResponseInterceptor;
import website.magyar.mitm.proxy.help.ResponseInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests just a single basic proxy running as a man in the middle.
 *
 * @author Tamas_Kohegyi
 */
public class MitmComplexProxyWithExternalServerTest extends AbstractComplexProxyTool {

    @Override
    protected void setUp() {
        String stubUrl = "http://127.0.0.1:" + getStubServerPort() + "/stub";
        LOGGER.info("STUB URL used: {}", stubUrl);
        DefaultRequestInterceptor defaultRequestInterceptor = new DefaultRequestInterceptor(getRequestCount(), NEED_STUB_RESPONSE, stubUrl);
        DefaultResponseInterceptor defaultResponseInterceptor = new DefaultResponseInterceptor(getResponseCount());
        getProxyServer().addRequestInterceptor(defaultRequestInterceptor);
        getProxyServer().addResponseInterceptor(defaultResponseInterceptor);
        getProxyServer().setCaptureBinaryContent(false);
        getProxyServer().setCaptureContent(false);
        ProxyServer.setResponseVolatile(true);
    }

    @Test
    public void testSimpleLocalGetRequestOverHTTPSThroughProxy() throws Exception {
        String CALL = "/ok";
        HttpHost externalHost = new HttpHost("127.0.0.1", 8443, "https");
        try {
            httpGetWithApacheClient(externalHost, CALL, false, false, ContentEncoding.ANY);
        } catch (Exception e) {
            externalHost = null;
        }
        Assumptions.assumeTrue(externalHost != null);
        //do test if available
        ResponseInfo proxiedResponse = httpGetWithApacheClient(externalHost, CALL, true, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertTrue(proxiedResponse.getBody().contains("Wilma Test Server"));
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleLocalGetRequestOverHTTPSWithoutProxy() throws Exception {
        String CALL = "/ok";
        HttpHost externalHost = new HttpHost("127.0.0.1", 8443, "https");
        try {
            httpGetWithApacheClient(externalHost, CALL, false, false, ContentEncoding.ANY);
        } catch (Exception e) {
            externalHost = null;
        }
        Assumptions.assumeTrue(externalHost != null);
        //do test if available
        ResponseInfo proxiedResponse = httpGetWithApacheClient(externalHost, CALL, false, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertTrue(proxiedResponse.getBody().contains("Wilma Test Server"));
        assertEquals(0, responseCount.get());
        assertEquals(0, requestCount.get());
    }

    @Test
    public void testSimpleRemoteGetRequestOverHTTPSThroughProxy() throws Exception {
        //check if external test server is available
        String CALL = "/search?q=mitmJavaProxy";
        HttpHost externalHost = new HttpHost("www.google.com", 443, "https");
        try {
            httpGetWithApacheClient(externalHost, CALL, false, false, ContentEncoding.ANY);
        } catch (Exception e) {
            externalHost = null;
        }
        Assumptions.assumeTrue(externalHost != null);
        //do test if available
        ResponseInfo proxiedResponse = httpGetWithApacheClient(externalHost, CALL, true, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertTrue(requestCount.get() > 0);
        assertTrue(responseCount.get() > 0);
        assertEquals(requestCount.get(), responseCount.get());
    }

    @Test
    public void testSimpleRemoteGetRequestOverHTTPSWithoutProxy() throws Exception {
        //check if external test server is available
        String CALL = "/search?q=mitmJavaProxy";
        HttpHost externalHost = new HttpHost("www.google.com", 443, "https");
        try {
            httpGetWithApacheClient(externalHost, CALL, false, false, ContentEncoding.ANY);
        } catch (Exception e) {
            externalHost = null;
        }
        Assumptions.assumeTrue(externalHost != null);
        //do test if available
        ResponseInfo proxiedResponse = httpGetWithApacheClient(externalHost, CALL, false, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(0, responseCount.get());
        assertEquals(0, requestCount.get());
    }

    @Test
    public void testSimpleRemoteGetRequestOverHTTPSThroughProxy_Self() throws Exception {
        //check if external test server is available
        String CALL = "/";
        HttpHost externalHost = new HttpHost("mitmjavaproxy.magyar.website", 443, "https");
        try {
            httpGetWithApacheClient(externalHost, CALL, false, false, ContentEncoding.ANY);
        } catch (Exception e) {
            externalHost = null;
        }
        Assumptions.assumeTrue(externalHost != null);
        //do test if available
        ResponseInfo proxiedResponse = httpGetWithApacheClient(externalHost, CALL, true, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertTrue(requestCount.get() > 0);
        assertTrue(responseCount.get() > 0);
        assertEquals(requestCount.get(), responseCount.get());
    }

    @Test
    public void testSimpleRemoteGetRequestOverHTTPSThroughProxy_ReqRes() throws Exception {
        //check if external test server is available
        String CALL = "/api/users?page/2";
        HttpHost externalHost = new HttpHost("reqres.in", 443, "https");
        try {
            httpGetWithApacheClient(externalHost, CALL, false, false, ContentEncoding.ANY);
        } catch (Exception e) {
            externalHost = null;
        }
        Assumptions.assumeTrue(externalHost != null);
        //do test if available
        ResponseInfo proxiedResponse = httpGetWithApacheClient(externalHost, CALL, true, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertTrue(requestCount.get() > 0);
        assertTrue(responseCount.get() > 0);
        assertEquals(requestCount.get(), responseCount.get());
    }

}
