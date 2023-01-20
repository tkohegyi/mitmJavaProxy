package website.magyar.mitm.proxy.response;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import website.magyar.mitm.proxy.ProxyServer;
import website.magyar.mitm.proxy.ResponseInterceptor;
import website.magyar.mitm.proxy.help.AbstractSimpleProxyTool;
import website.magyar.mitm.proxy.help.ClientServerBase;
import website.magyar.mitm.proxy.help.ContentEncoding;
import website.magyar.mitm.proxy.help.TestUtils;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests just a single basic proxy running as a man in the middle, but target server is not responding, therefore PROXY has a timeout.
 * Now we try to alter the message of the proxy itself.
 *
 * @author Tamas_Kohegyi
 */
public class ProxyTimeoutMessageResponseManipulationTest extends ClientServerBase {

    private static final String REQ_JSON_BODY = "{ \"json\": \"simple text\" }";
    private HttpGet request;

    @Override
    protected void setUp() {
        //NOTE that proxy has 5 sec timeout
        ProxyTimeoutMessageResponseManipulationTest.TestResponseInterceptor testResponseInterceptor = new ProxyTimeoutMessageResponseManipulationTest.TestResponseInterceptor();
        getProxyServer().addResponseInterceptor(testResponseInterceptor);
        getProxyServer().setCaptureBinaryContent(false);
        getProxyServer().setCaptureContent(false);
        ProxyServer.setResponseVolatile(true);
        request = new HttpGet(AbstractSimpleProxyTool.GET_SLOW_RESPONSE);
    }

    @Override
    protected int getProxyTimeout() {
        return AbstractSimpleProxyTool.PROXY_SHORT_TIMEOUT;
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
    }

    @Test
    public void testSimpleGetRequestWithTimeout() throws Exception {
        //here we don1t touch the response, just test the normal Proxy timeout message
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort(), ContentEncoding.ANY)) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            String body = EntityUtils.toString(response.getEntity());
            assertEquals(504, response.getStatusLine().getStatusCode(), "HTTP Response Status code is:" + response.getStatusLine().getStatusCode());
            assertEquals("Gateway Timeout", response.getStatusLine().getReasonPhrase(), "HTTP Response Reason Phrase is:" + response.getStatusLine().getReasonPhrase());
            assertTrue(body.startsWith("PROXY: Connection timed out!"));
        }
    }

    @Test
    public void requestWithTimeoutWithAlteredResponse() throws Exception {
        //NOTE: There is no way to manipulate the response, if there is NO response...
        request.addHeader("A", "A");
        try (CloseableHttpClient httpClient = TestUtils.buildHttpClient(true, getProxyPort(), ContentEncoding.ANY)) {
            HttpResponse response = httpClient.execute(getHttpHost(), request); //request is here
            String body = EntityUtils.toString(response.getEntity());
            assertEquals(504, response.getStatusLine().getStatusCode(), "HTTP Response Status code is:" + response.getStatusLine().getStatusCode());
            assertEquals("Gateway Timeout", response.getStatusLine().getReasonPhrase(), "HTTP Response Reason Phrase is:" + response.getStatusLine().getReasonPhrase());
            assertTrue(body.startsWith("PROXY: Connection timed out!"));
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {

            byte[] newBody = null;
            Header[] requestHeaders = response.getRequestHeaders();

            //alter body - if 'A' header - use json request
            if (response.findHeader(requestHeaders, "A") != null) {
                //THERE IS NO WAY TO MANIPULATE THE ANSWER, SINCE THERE IS NO ANSWER
                // newBody = REQ_JSON_BODY.getBytes(StandardCharsets.UTF_8);
                // response.setContentType("application/json"); -> this causes NPE, response is null
                // response.setStatus(200);
                // response.setReasonPhrase("OK");
            }

            try {
                response.setBody(newBody);
            } catch (IOException e) {
                //just swallow it
            }
        }
    }

}
