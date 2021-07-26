package website.magyar.mitm.proxy.dns;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import website.magyar.mitm.proxy.RequestInterceptor;
import website.magyar.mitm.proxy.ResponseInterceptor;
import website.magyar.mitm.proxy.help.HttpClientBase;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests that DNS resolution works properly inside the proxy.
 *
 * @author Tamas_Kohegyi
 */
public class ThirdPartyCallsForDNSUsageOfProxyTest extends HttpClientBase {
    private static final String GET_REQUEST = "/";
    private final Logger logger = LoggerFactory.getLogger(ThirdPartyCallsForDNSUsageOfProxyTest.class);

    @Override
    protected void setUp() throws Exception {
        TestRequestInterceptor defaultRequestInterceptor = new TestRequestInterceptor();
        TestResponseInterceptor defaultResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addRequestInterceptor(defaultRequestInterceptor);
        getProxyServer().addResponseInterceptor(defaultResponseInterceptor);
        getProxyServer().setCaptureBinaryContent(false);
        getProxyServer().setCaptureContent(false);
    }

    @Override
    protected void tearDown() throws Exception {
    }

    @Test
    public void testGoogleViaProxy() throws Exception {
        HttpHost host = new HttpHost("www.google.com", 443, "https");
        HttpGet request = new HttpGet(GET_REQUEST);
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpResponse response = httpClient.execute(host, request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            logger.debug("Body length: {}, first few data:{}...", body.length(), body.substring(0,30));
            assertNull(getLastException());
        }
    }

    @Test
    public void testGitHubViaProxy() throws Exception {
        HttpHost host = new HttpHost("raw.githubusercontent.com", 443, "https");
        HttpGet request = new HttpGet(GET_REQUEST + "tkohegyi/mitmJavaProxy/master/LICENSE.txt");
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpResponse response = httpClient.execute(host, request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            logger.debug("Body length: {}, first few data:{}...", body.length(), body.substring(0,30));
            assertNull(getLastException());
        }
    }

    class TestRequestInterceptor implements RequestInterceptor {

        @Override
        public void process(MitmJavaProxyHttpRequest request) {
            logger.info("Request interceptor invoked.");
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            logger.info("Response interceptor invoked.");
        }
    }

}
