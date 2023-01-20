package website.magyar.mitm.proxy.response;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;
import website.magyar.mitm.proxy.ProxyServer;
import website.magyar.mitm.proxy.ResponseInterceptor;
import website.magyar.mitm.proxy.help.ClientServerBase;
import website.magyar.mitm.proxy.help.ContentEncoding;
import website.magyar.mitm.proxy.help.ProxyServerBase;
import website.magyar.mitm.proxy.help.TestUtils;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * This test checks if the response header can be accessed and altered by the response interceptors.
 * 3 header is in use/tested:
 * 'A'->'Aa' is to check header existence and accessibility
 * 'B'->'Bb' is altered to 'B'->'bBB' at request interceptor
 * 'C'->'Cc' is removed at request interceptor
 * + 1 header 'D'->'Dd' is added to response by response interceptor
 *
 * @author Tamas_Kohegyi
 */
public class ResponseHeaderManipulationTest extends ClientServerBase {
    private static final String GET_REQUEST = "/anyUrl";
    private final Logger logger = LoggerFactory.getLogger(ResponseHeaderManipulationTest.class);
    private HttpGet request;

    @Override
    protected void setUp() throws Exception {
        TestResponseInterceptor testResponseInterceptor = new TestResponseInterceptor();
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        ProxyServer.setResponseVolatile(true); //this is a must !!!
        request = new HttpGet(GET_REQUEST);
    }

    @Override
    protected int getProxyTimeout() {
        return ProxyServerBase.PROXY_LONG_TIMEOUT;
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
        //add test headers to the server response
        response.addHeader("A", "Aa"); //header to be found
        response.addHeader("B", "Bb"); //header to be altered
        response.addHeader("C", "Cc"); //header to be removed
    }

    @Test
    public void headerInterceptedAndAccessible() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort(), ContentEncoding.ANY)) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            assertEquals(200, response.getStatusLine().getStatusCode(), "HTTP Response Status code is:" + response.getStatusLine().getStatusCode());
            assertNull(getLastException());
            //check headers in response
            Header h;
            //'A' existence
            h = response.getFirstHeader("A");
            assertNotNull(h, "Cannot find 'A' header in response");
            //'B' changed
            h = response.getFirstHeader("B");
            assertEquals("bBB", h.getValue());
            //'C' must missing
            h = response.getFirstHeader("C");
            assertNull(h, "Deleted header 'C' found");
            //'D' (the new header) must exist
            h = response.getFirstHeader("D");
            assertNotNull(h, "Cannot find 'D' header in response");
        }
    }

    @Test
    public void headerInterceptedAndAccessibleSecure() throws Exception {
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort(), ContentEncoding.ANY)) {
            HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
            assertEquals(200, response.getStatusLine().getStatusCode(), "HTTPS Response Status code is:" + response.getStatusLine().getStatusCode());
            assertNull(getLastException());
            //check headers in response
            Header h;
            //'A' existence
            h = response.getFirstHeader("A");
            assertNotNull(h, "Cannot find 'A' header in response");
            //'B' changed
            h = response.getFirstHeader("B");
            assertEquals("bBB", h.getValue());
            //'C' must missing
            h = response.getFirstHeader("C");
            assertNull(h, "Deleted header 'C' found");
            //'D' (the new header) must exist
            h = response.getFirstHeader("D");
            assertNotNull(h, "Cannot find 'D' header in response");
        } catch (SSLException | IndexOutOfBoundsException e) {
            logger.error("Ups", e);
            throw e;
        }
    }

    @Test
    public void headerInterceptedAndAccessibleButResponseIsNotVolatile() throws Exception {
        ProxyServer.setResponseVolatile(false); //interceptor shall not influence the response !
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort(), ContentEncoding.ANY)) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            assertEquals(200, response.getStatusLine().getStatusCode(), "HTTP Response Status code is:" + response.getStatusLine().getStatusCode());
            assertNull(getLastException());
            //check headers in response
            Header h;
            //'A' existence
            h = response.getFirstHeader("A");
            assertNotNull(h, "Cannot find 'A' header in response");
            //'B' unchanged
            h = response.getFirstHeader("B");
            assertEquals("Bb", h.getValue());
            //'C' must present
            h = response.getFirstHeader("C");
            assertNotNull(h, "Header 'C' seems deleted");
            //'D' (the new header) must not exist
            h = response.getFirstHeader("D");
            assertNull(h, "Shall not find 'D' header in response");
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            boolean found;

            //A header - just check that we can find it, do not alter
            String headerString = response.getHeader("A");
            detectIssue(!"Aa".equals(headerString), "'A' was not found at interceptor");

            //B header - alter it
            found = false;
            Header[] headers = response.getHeaders();
            for (Header h : headers) {
                if ("B".equals(h.getName())) {
                    logger.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    found = true;
                    response.updateHeader(h, "bBB");
                }
            }
            detectIssue(!found, "'B' was not found at interceptor");

            //C header - remove it
            found = false;
            headers = response.getHeaders();
            for (Header h : headers) {
                if ("C".equals(h.getName())) {
                    logger.info("Header '{}' found with value '{}'", h.getName(), h.getValue());
                    found = true;
                    response.removeHeader(h);
                }
            }
            detectIssue(!found, "'C' was not found at interceptor");

            //D header - just add this brand new header
            response.addHeader(new BasicHeader("D", "Dd"));
        }
    }
}
