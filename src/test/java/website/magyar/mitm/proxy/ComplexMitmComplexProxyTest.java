package website.magyar.mitm.proxy;

import org.junit.jupiter.api.Test;
import website.magyar.mitm.proxy.help.AbstractComplexProxyTool;
import website.magyar.mitm.proxy.help.ContentEncoding;
import website.magyar.mitm.proxy.help.DefaultRequestInterceptor;
import website.magyar.mitm.proxy.help.DefaultResponseInterceptor;
import website.magyar.mitm.proxy.help.ResponseInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests just a single basic proxy running as a man in the middle.
 *
 * @author Tamas_Kohegyi
 */
public class ComplexMitmComplexProxyTest extends AbstractComplexProxyTool {

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
    public void testSimpleGetRequest() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NO_NEED_STUB_RESPONSE, true, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPS() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NO_NEED_STUB_RESPONSE, true, false, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPS_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NO_NEED_STUB_RESPONSE, true, false, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPS_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NO_NEED_STUB_RESPONSE, true, false, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequest() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NO_NEED_STUB_RESPONSE, true, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestOverHTTPS() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NO_NEED_STUB_RESPONSE, true, ContentEncoding.ANY);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestToStub_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NEED_STUB_RESPONSE, true, false, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSToStub_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true, false, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestToStub_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NEED_STUB_RESPONSE, true, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestOverHTTPSToStub_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestToStub_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NEED_STUB_RESPONSE, true, false, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSToStub_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true, false, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestToStub_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NEED_STUB_RESPONSE, true, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestOverHTTPSToStub_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(1, responseCount.get());
        assertEquals(1, requestCount.get());
    }

}
