package org.rockhill.mitm.proxy.request;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.rockhill.mitm.proxy.RequestInterceptor;
import org.rockhill.mitm.proxy.ResponseInterceptor;
import org.rockhill.mitm.proxy.help.AnsweringServerBase;
import org.rockhill.mitm.proxy.help.TestUtils;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This test checks if the request body can be accessed and altered by the request interceptors.
 * Tests:
 * - No additional header - body untouched
 * - If header "A" added - reduce body size to 5 chars
 * - If header "B" added - duplicate the body text
 * - If header "C" added - replace it with a json message (changes content-type too)
 */
public class RequestBodyManipulationTest extends AnsweringServerBase {
    protected static final String GET_REQUEST = "/anyUrl";
    private final static String REQ_STRING_BODY = "initial request body";
    private final static String REQ_JSON_BODY = "{ \"json\": \"simple text\" }";
    private HttpPost request;

    @Override
    protected void setUp() throws Exception {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
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
            assertEquals(5, request.getContentLength());
            assertTrue(REQ_STRING_BODY.startsWith(bodyString));
        } else {
            if (request.getHeader("B") != null) {
                // If header "B" added - duplicate the body text
                assertEquals(request.getContentLength(), REQ_STRING_BODY.length() * 2);
                assertEquals((REQ_STRING_BODY + REQ_STRING_BODY), bodyString);
            } else {
                if (request.getHeader("C") != null) {
                    // If header "C" added - replace it with a json message (changes content-type too)
                    assertEquals(request.getContentLength(), REQ_JSON_BODY.length());
                    assertEquals(REQ_JSON_BODY, bodyString);
                    String contentType = request.getHeader("Content-Type");
                    assertEquals("application/json", contentType);
                } else {
                    // No additional header - body untouched
                    assertEquals(request.getContentLength(), REQ_STRING_BODY.length());
                    assertEquals(REQ_STRING_BODY, bodyString);
                }
            }
        }
        return null;
    }

    @Test
    public void noRequestBodyChange() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(httpHost, request); //request is here
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void noRequestBodyChangeSecure() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(secureHost, request); //request is here
        httpClient.close();
        assertEquals("HTTPS Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void reduceTo5Chars() throws Exception {
        request.addHeader("A", "A");
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(httpHost, request); //request is here
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void reduceTo5CharsSecure() throws Exception {
        request.addHeader("A", "A");
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(secureHost, request); //request is here
        httpClient.close();
        assertEquals("HTTPS Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void doubleBodySize() throws Exception {
        request.addHeader("B", "B");
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(httpHost, request); //request is here
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void doubleBodySizeSecure() throws Exception {
        request.addHeader("B", "B");
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(secureHost, request); //request is here
        httpClient.close();
        assertEquals("HTTPS Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void replaceWithJson() throws Exception {
        request.addHeader("C", "C");
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(httpHost, request); //request is here
        httpClient.close();
        assertEquals("HTTP Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void replaceWithJsonSecure() throws Exception {
        request.addHeader("C", "C");
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort());
        HttpResponse response = httpClient.execute(secureHost, request); //request is here
        httpClient.close();
        assertEquals("HTTPS Response Status code is:" + response.getStatusLine().getStatusCode(), 200, response.getStatusLine().getStatusCode());
        assertNull(getLastException());
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
                assertTrue("Cannot find the expected body", REQ_STRING_BODY.equals(body));

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

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
        }
    }
}
