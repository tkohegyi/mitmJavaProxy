package website.magyar.mitm.proxy.dns;

import org.apache.http.HttpHost;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import website.magyar.mitm.proxy.ProxyServer;
import website.magyar.mitm.proxy.help.AbstractComplexProxyTool;
import website.magyar.mitm.proxy.help.ContentEncoding;
import website.magyar.mitm.proxy.help.DefaultRequestInterceptor;
import website.magyar.mitm.proxy.help.DefaultResponseInterceptor;
import website.magyar.mitm.proxy.help.ResponseInfo;

/**
 * Tests that DNS resolution works properly inside the proxy.
 *
 * @author Tamas_Kohegyi
 */
public class DNSUsageOfComplexProxyTest extends AbstractComplexProxyTool {

    @Override
    protected void setUp() {
        String stubUrl = "http://localhost:" + getStubServerPort() + "/stub";
        LOGGER.info("STUB URL used: {}", stubUrl);
        DefaultRequestInterceptor defaultRequestInterceptor = new DefaultRequestInterceptor(getRequestCount(), NEED_STUB_RESPONSE, stubUrl);
        DefaultResponseInterceptor defaultResponseInterceptor = new DefaultResponseInterceptor(getResponseCount());
        getProxyServer().addRequestInterceptor(defaultRequestInterceptor);
        getProxyServer().addResponseInterceptor(defaultResponseInterceptor);
        getProxyServer().setCaptureBinaryContent(false);
        getProxyServer().setCaptureContent(false);
        ProxyServer.setResponseVolatile(true);
        //update servers
        stubHost = new HttpHost("localhost", stubServerPort);
        webHost = new HttpHost("localhost", getWebServerPort());
        httpsWebHost = new HttpHost("localhost", httpsWebServerPort, "https");

    }

    @Test
    public void testSimpleGetRequestUsingDNSNoProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NO_NEED_STUB_RESPONSE, false, false, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(0, responseCount.get());
        Assertions.assertEquals(0, requestCount.get());
    }

    @Test
    public void testSimplePostRequestUsingDNSNoProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NO_NEED_STUB_RESPONSE, false, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(0, responseCount.get());
        Assertions.assertEquals(0, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestDNSAndProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NO_NEED_STUB_RESPONSE, true, false, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(1, responseCount.get());
        Assertions.assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSDNSAndProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NO_NEED_STUB_RESPONSE, true, false, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(1, responseCount.get());
        Assertions.assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestDNSAndProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NO_NEED_STUB_RESPONSE, true, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(1, responseCount.get());
        Assertions.assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestOverHTTPSDNSAndProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NO_NEED_STUB_RESPONSE, true, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(SERVER_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(1, responseCount.get());
        Assertions.assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestToStubDNSAndProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(webHost, NEED_STUB_RESPONSE, true, false, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(1, responseCount.get());
        Assertions.assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimpleGetRequestOverHTTPSToStubDNSAndProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpGetWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true, false, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(1, responseCount.get());
        Assertions.assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestToStubDNSAndProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(webHost, NEED_STUB_RESPONSE, true, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(1, responseCount.get());
        Assertions.assertEquals(1, requestCount.get());
    }

    @Test
    public void testSimplePostRequestOverHTTPSToStubDNSAndProxyInUse() throws Exception {
        ResponseInfo proxiedResponse = httpPostWithApacheClient(httpsWebHost, NEED_STUB_RESPONSE, true, ContentEncoding.ANY);
        Assertions.assertEquals(200, proxiedResponse.getStatusCode());
        Assertions.assertEquals(STUB_BACKEND, proxiedResponse.getBody());
        Assertions.assertEquals(1, responseCount.get());
        Assertions.assertEquals(1, requestCount.get());
    }

}
