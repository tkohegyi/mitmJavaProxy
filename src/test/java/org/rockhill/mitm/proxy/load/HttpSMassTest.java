package org.rockhill.mitm.proxy.load;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.rockhill.mitm.proxy.RequestInterceptor;
import org.rockhill.mitm.proxy.ResponseInterceptor;
import org.rockhill.mitm.proxy.help.AnsweringServerBase;
import org.rockhill.mitm.proxy.help.TestUtils;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * This test runs simple http request 1000 times.
 */
public class HttpSMassTest extends AnsweringServerBase {
    private static final String GET_REQUEST = "/anyUrl";
    private static final int MAX_REQUEST = 1000;
    private final Logger logger = LoggerFactory.getLogger(HttpSMassTest.class);
    private HttpGet request;

    @Override
    protected void setUp() throws Exception {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        request = new HttpGet(GET_REQUEST);
        request.addHeader("A", "A"); //a header to start with
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected byte[] evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        return null;
    }

    @Test
    public void massHttpSRequestTest() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            for (int i = 0; i < MAX_REQUEST; i++) {
                Header h = request.getFirstHeader("A");
                request.removeHeader(h);
                request.addHeader("A", "R-" + i);
                HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
                int statusCode = response.getStatusLine().getStatusCode();
                EntityUtils.consume(response.getEntity());
                assertEquals("HTTPS Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
                assertNull(getLastException());
                logger.info("Request no:{} done successfully.", i);
            }
        }
    }

    class TestRequestInterceptor implements RequestInterceptor {

        @Override
        public void process(MitmJavaProxyHttpRequest request) {
            Header header;
            //header to be found
            header = request.getMethod().getFirstHeader("A");
            if (header == null) {
                Exception e = new Exception("Request header was not found");
                setLastException(e);
                logger.error("EXCEPTION at Request Interceptor", e);
            }
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            Header[] headers = response.getRequestHeaders();
            Header h = response.findHeader(headers, "A");
            if (h == null) {
                Exception e = new Exception("'A' was not found at response interceptor");
                setLastException(e);
                logger.error("EXCEPTION at Response Interceptor", e);
            }
            response.addHeader(new BasicHeader("B", response.getEntry().getMessageId()));
        }
    }
}
