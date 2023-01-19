package website.magyar.mitm.proxy.request;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import website.magyar.mitm.proxy.RequestInterceptor;
import website.magyar.mitm.proxy.ResponseInterceptor;
import website.magyar.mitm.proxy.help.ClientServerBase;
import website.magyar.mitm.proxy.help.ContentEncoding;
import website.magyar.mitm.proxy.help.TestUtils;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test checks if the request header can be accessed and altered by the request interceptors.
 * 3 header is in use/tested:
 * 'A'->'A' is to check header existence and accessibility
 * 'B'->'B' is altered to 'B'->'BB' at request interceptor
 * 'C'->'C' is removed at request interceptor
 * 'D'->'D' this header is added as new header at request interceptor
 *
 * @author Tamas_Kohegyi
 */
public class RequestHeaderManipulationTest extends ClientServerBase {
    protected static final String GET_REQUEST = "/anyUrl";
    private final Logger logger = LoggerFactory.getLogger(RequestHeaderManipulationTest.class);
    private HttpGet request;

    @Override
    protected void setUp() throws Exception {
        TestRequestInterceptor testRequestInterceptor = new TestRequestInterceptor();
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addRequestInterceptor(testRequestInterceptor);
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        request = new HttpGet(GET_REQUEST);
        request.addHeader("A", "A"); //header to be found
        request.addHeader("B", "B"); //header to be altered
        request.addHeader("C", "C"); //header to be removed
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        String headerValue;
        //check request header existence
        headerValue = request.getHeader("A");
        assertEquals("A", headerValue);
        //check altered header value
        headerValue = request.getHeader("B");
        assertEquals("BB", headerValue);
        //check deletion of header
        headerValue = request.getHeader("C");
        assertNull(headerValue);
        //check new header
        headerValue = request.getHeader("D");
        assertEquals("D", headerValue);
    }

    @Test
    public void headerInterceptedAndAccessible() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort(), ContentEncoding.ANY);
        HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
        httpClient.close();
        assertEquals(200, response.getStatusLine().getStatusCode(), "HTTP Response Status code is:" + response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    @Test
    public void headerInterceptedAndAccessibleSecure() throws Exception {
        CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort(), ContentEncoding.ANY);
        HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
        httpClient.close();
        assertEquals(200, response.getStatusLine().getStatusCode(), "HTTPS Response Status code is:" + response.getStatusLine().getStatusCode());
        assertNull(getLastException());
    }

    class TestRequestInterceptor implements RequestInterceptor {

        @Override
        public void process(MitmJavaProxyHttpRequest request) {
            Header header;
            //header to be found
            header = request.getMethod().getFirstHeader("A");
            assertEquals(header.getValue(), "A");
            //header to be altered
            header = request.getMethod().getFirstHeader("B");
            assertEquals(header.getValue(), "B");
            request.getMethod().removeHeader(header);
            request.getMethod().addHeader("B", "BB");
            //header to be removed
            header = request.getMethod().getFirstHeader("C");
            assertEquals(header.getValue(), "C");
            request.getMethod().removeHeader(header);
            //add brand new header
            request.getMethod().addHeader("D", "D");
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            Header[] headers = response.getRequestHeaders();
            boolean found;
            //header to be found
            found = false;
            for (Header h : headers) {
                if ("A".equals(h.getName())) {
                    logger.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    if ("A".equals(h.getValue())) {
                        found = true;
                    }
                }
            }
            assertTrue(found, "Cannot find request header 'A'->'A'");

            //header to be found as altered
            found = false;
            for (Header h : headers) {
                if ("B".equals(h.getName())) {
                    logger.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    if ("BB".equals(h.getValue())) {
                        found = true;
                    }
                }
            }
            assertTrue(found, "Cannot find altered request header 'B'->'BB'");

            //header not to be found
            found = false;
            for (Header h : headers) {
                if ("C".equals(h.getName())) {
                    logger.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    found = true;
                }
            }
            assertFalse(found, "Shall not find deleted request header 'C'");
        }
    }
}
