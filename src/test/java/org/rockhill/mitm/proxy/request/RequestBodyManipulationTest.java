package org.rockhill.mitm.proxy.request;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.rockhill.mitm.proxy.RequestInterceptor;
import org.rockhill.mitm.proxy.help.AnsweringServerBase;
import org.rockhill.mitm.proxy.help.TestUtils;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * This test checks if the request body can be accessed and altered by the request interceptors.
 * Tests:
 * - No additional header - body untouched
 * - If header "A" added - reduce body size to 5 chars
 * - If header "B" added - duplicate the body text
 * - If header "C" added - replace it with a json message (changes content-type too)
 */
public class RequestBodyManipulationTest extends AnsweringServerBase {
    private static final String GET_REQUEST = "/anyUrl";
    private static final String REQ_STRING_BODY = "initial request body";
    private static final String REQ_JSON_BODY = "{ \"json\": \"simple text\" }";
    private final Logger logger = LoggerFactory.getLogger(RequestBodyManipulationTest.class);
    private HttpPost request;

    @Override
    protected void setUp() throws Exception {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        request = new HttpPost(GET_REQUEST);
        final StringEntity entity = new StringEntity(REQ_STRING_BODY, "UTF-8");
        entity.setChunked(true);
        request.setEntity(entity);
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected byte[] evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        //check if body properly arrived to server
        if (request.getHeader("A") != null) {
            // If header "A" added - reduce body size to 5 chars
            if (5 != request.getContentLength()) {
                setLastException(new Exception("Content length is not 5"));
            }
            if (!REQ_STRING_BODY.startsWith(bodyString)) {
                setLastException(new Exception("Body differs from expected string"));
            }
        } else {
            if (request.getHeader("B") != null) {
                // If header "B" added - duplicate the body text
                if (request.getContentLength() != REQ_STRING_BODY.length() * 2) {
                    setLastException(new Exception("Request content-length is not double sized"));
                }
                if (!(REQ_STRING_BODY + REQ_STRING_BODY).equals(bodyString)) {
                    setLastException(new Exception("Request body is not the double sized body"));
                }
            } else {
                if (request.getHeader("C") != null) {
                    // If header "C" added - replace it with a json message (changes content-type too)
                    if (request.getContentLength() != REQ_JSON_BODY.length()) {
                        setLastException(new Exception("Request content-length is not normal sized"));
                    }
                    if (!REQ_JSON_BODY.equals(bodyString)) {
                        setLastException(new Exception("Request body is not the json body"));
                    }
                    String contentType = request.getHeader("Content-Type");
                    if (!"application/json".equals(contentType)) {
                        setLastException(new Exception("Request content-type is not json"));
                    }
                } else {
                    // No additional header - body untouched
                    if (request.getContentLength() != REQ_STRING_BODY.length()) {
                        setLastException(new Exception("Request content-length is correct"));
                    }
                    if (!REQ_STRING_BODY.equals(bodyString)) {
                        setLastException(new Exception("Request body is not the expected body"));
                    }
                }
            }
        }
        return null;
    }

    @Test
    public void noRequestBodyChange() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
        }
    }

    @Test
    public void noRequestBodyChangeSecure() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTPS Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
        }
    }

    @Test
    public void reduceTo5Chars() throws Exception {
        request.addHeader("A", "A");
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
        }
    }

    @Test
    public void reduceTo5CharsSecure() throws Exception {
        request.addHeader("A", "A");
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTPS Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
        }
    }

    @Test
    public void doubleBodySize() throws Exception {
        request.addHeader("B", "B");
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
        }
    }

    @Test
    public void doubleBodySizeSecure() throws Exception {
        request.addHeader("B", "B");
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTPS Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
        }
    }

    @Test
    public void replaceWithJson() throws Exception {
        request.addHeader("C", "C");
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTP Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
        }
    }

    @Test
    public void replaceWithJsonSecure() throws Exception {
        request.addHeader("C", "C");
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort())) {
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            assertEquals("HTTPS Response Status code is:" + statusCode, 200, statusCode);
            assertNull(getLastException());
        }
    }

    class TestRequestInterceptor implements RequestInterceptor {

        @Override
        public void process(MitmJavaProxyHttpRequest request) {
            byte[] newBody = null;

            try {
                //get the body as string
                InputStream clonedInputStream = request.getPlayGround();
                clonedInputStream.mark(8192);
                String body = IOUtils.toString(clonedInputStream);
                clonedInputStream.reset();
                if (!REQ_STRING_BODY.equals(body)) {
                    setLastException(new Exception("Cannot find the expected body"));
                }

                //alter body - if 'A' header - to 5 char long
                if (request.getMethod().getFirstHeader("A") != null) {
                    newBody = body.substring(0, 5).getBytes(StandardCharsets.UTF_8);
                }

                //alter body - if 'B' header - double the body
                if (request.getMethod().getFirstHeader("B") != null) {
                    newBody = (body + body).getBytes(StandardCharsets.UTF_8);
                }

                //alter body - if 'C' header - use json request
                if (request.getMethod().getFirstHeader("C") != null) {
                    newBody = REQ_JSON_BODY.getBytes(StandardCharsets.UTF_8);
                    Header header = request.getMethod().getFirstHeader("Content-Type");
                    request.getMethod().removeHeader(header);
                    request.getMethod().addHeader("Content-Type", "application/json");
                }


            } catch (IOException e) {
                setLastException(e);
            }

            request.setBody(newBody);
        }
    }

}
