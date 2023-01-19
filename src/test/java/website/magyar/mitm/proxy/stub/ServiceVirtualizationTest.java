package website.magyar.mitm.proxy.stub;

import org.junit.jupiter.api.Test;
import website.magyar.mitm.proxy.ProxyServer;
import website.magyar.mitm.proxy.help.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is an example Service Virtualization - uses the proxy to redirect the request to an internal stub server.
 *
 * @author Tamas_Kohegyi
 */
public class ServiceVirtualizationTest extends AbstractComplexProxyTool {

    private final static boolean NEED_PROXY = true;

    @Override
    protected void setUp() {
        String stubUrl = "http://127.0.0.1:" + getStubServerPort() + "/stub";
        LOGGER.info("STUB URL used: {}", stubUrl);
        RequestInterceptorForStub requestInterceptorForStub = new RequestInterceptorForStub(stubUrl);
        getProxyServer().addRequestInterceptor(requestInterceptorForStub);
        getProxyServer().setCaptureBinaryContent(false);
        getProxyServer().setCaptureContent(false);
        ProxyServer.setResponseVolatile(true);
    }

    @Test
    public void testSimpleGetRequestToStub_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NEED_STUB_RESPONSE, NEED_PROXY, false, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSToStub_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, NEED_PROXY, false, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
    }

    @Test
    public void testSimplePostRequestToStub_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NEED_STUB_RESPONSE, NEED_PROXY, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
    }

    @Test
    public void testSimplePostRequestOverHTTPSToStub_Gzip() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, NEED_PROXY, ContentEncoding.GZIP);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
    }

    @Test
    public void testSimpleGetRequestToStub_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NEED_STUB_RESPONSE, NEED_PROXY, false, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSToStub_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, NEED_PROXY, false, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
    }

    @Test
    public void testSimplePostRequestToStub_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NEED_STUB_RESPONSE, true, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
    }

    @Test
    public void testSimplePostRequestOverHTTPSToStub_Brotli() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true, ContentEncoding.BROTLI);
        assertEquals(200, proxiedResponse.getStatusCode());
        assertEquals(STUB_BACKEND, proxiedResponse.getBody());
    }

}
