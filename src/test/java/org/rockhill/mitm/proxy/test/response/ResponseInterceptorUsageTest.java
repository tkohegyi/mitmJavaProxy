package org.rockhill.mitm.proxy.test.response;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.rockhill.mitm.proxy.RequestInterceptor;
import org.rockhill.mitm.proxy.ResponseInterceptor;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpResponse;
import org.rockhill.mitm.proxy.test.support.ClientServerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * This test runs simple http request 1000 times.
 */
public class ResponseInterceptorUsageTest extends ClientServerBase {
    private static final String GET_REQUEST = "/anyUrl";
    private final Logger logger = LoggerFactory.getLogger(ResponseInterceptorUsageTest.class);
    private HttpGet request;

    @Override
    protected void setUp() throws Exception {
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        request = new HttpGet(GET_REQUEST);
        request.addHeader("A", "A"); //a header to start with
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
    }

    @Test
    public void responseInterceptorCallTest() throws Exception {
        setLastException(new Exception("TobeDeletedByInterceptor"));
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", SERVER_BACKEND, body);
            assertNull("Interceptor was not invoked!, test exception", getLastException());
        }
    }

    @Test
    public void responseInterceptorCallSecureTest() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertEquals("Body content was incorrect", SERVER_BACKEND, body);
            assertNull("Interceptor was not invoked!, test exception", getLastException());
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            logger.info("Response Interceptor called, status: {}", response.getStatus());
            setLastException(null);
        }
    }

}
