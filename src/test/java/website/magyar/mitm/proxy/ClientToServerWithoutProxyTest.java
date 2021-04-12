package website.magyar.mitm.proxy;

import org.junit.Test;
import website.magyar.mitm.proxy.help.AbstractComplexProxyTool;
import website.magyar.mitm.proxy.help.DefaultRequestInterceptor;
import website.magyar.mitm.proxy.help.DefaultResponseInterceptor;
import website.magyar.mitm.proxy.help.ResponseInfo;

import static org.junit.Assert.assertEquals;

/**
 * Tests just a single basic proxy running as a man in the middle.
 */
public class ClientToServerWithoutProxyTest extends AbstractComplexProxyTool {

    @Override
    protected void setUp() {
        String stubUrl = "http://127.0.0.1:" + getStubServerPort() + "/stub";
        LOGGER.info("STUB URL used: {}", stubUrl);
        DefaultRequestInterceptor defaultRequestInterceptor = new DefaultRequestInterceptor(getRequestCount(), NEED_STUB_RESPONSE, stubUrl);
        DefaultResponseInterceptor defaultResponseInterceptor = new DefaultResponseInterceptor(getResponseCount());
        getProxyServer().addRequestInterceptor(defaultRequestInterceptor);
        getProxyServer().addResponseInterceptor(defaultResponseInterceptor);
        getProxyServer().setCaptureBinaryContent(true);
        getProxyServer().setCaptureContent(true);
    }

    @Test
    public void testSimpleGetRequest() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(getWebHost(), NO_NEED_STUB_RESPONSE, false, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(0, getResponseCount().get());
        assertEquals(0, getRequestCount().get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPS() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(getHttpsWebHost(), NO_NEED_STUB_RESPONSE, false, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(0, getResponseCount().get());
        assertEquals(0, getRequestCount().get());
    }

    @Test
    public void testSimplePostRequest() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(getWebHost(), NO_NEED_STUB_RESPONSE, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(0, getRequestCount().get());
        assertEquals(0, getResponseCount().get());
    }

    @Test
    public void testSimplePostRequestOverHTTPS() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(getHttpsWebHost(), NO_NEED_STUB_RESPONSE, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        assertEquals(0, getRequestCount().get());
        assertEquals(0, getResponseCount().get());
    }

    @Test
    public void testSimpleGetRequestToStub() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(getStubHost(), NEED_STUB_RESPONSE, false, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(0, getRequestCount().get());
        assertEquals(0, getResponseCount().get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSToStub() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(getStubHost(), NEED_STUB_RESPONSE, false, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(0, getRequestCount().get());
        assertEquals(0, getResponseCount().get());
    }

    @Test
    public void testSimplePostRequestToStub() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(getStubHost(), NEED_STUB_RESPONSE, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(0, getRequestCount().get());
        assertEquals(0, getResponseCount().get());
    }

    @Test
    public void testSimplePostRequestOverHTTPSToStub() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(getStubHost(), NEED_STUB_RESPONSE, false);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        assertEquals(0, getRequestCount().get());
        assertEquals(0, getResponseCount().get());
    }

}
