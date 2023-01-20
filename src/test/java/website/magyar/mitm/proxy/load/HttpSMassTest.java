package website.magyar.mitm.proxy.load;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import website.magyar.mitm.proxy.RequestInterceptor;
import website.magyar.mitm.proxy.ResponseInterceptor;
import website.magyar.mitm.proxy.help.ClientServerBase;
import website.magyar.mitm.proxy.help.ProxyServerBase;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This test runs simple http request 1000 times.
 *
 * @author Tamas_Kohegyi
 */
public class HttpSMassTest extends ClientServerBase {
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
    protected int getProxyTimeout() {
        return ProxyServerBase.PROXY_LONG_TIMEOUT;
    }

    @Override
    protected void evaluateServerRequestResponse(HttpServletRequest request, HttpServletResponse response, String bodyString) {
    }

    @Test
    public void massHttpSRequestTest() throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            for (int i = 0; i < MAX_REQUEST; i++) {
                Header h = request.getFirstHeader("A");
                request.removeHeader(h);
                request.addHeader("A", "R-" + i);
                HttpResponse response = httpClient.execute(getSecureHost(), request); //request is here
                int statusCode = response.getStatusLine().getStatusCode();
                EntityUtils.consume(response.getEntity());
                Assertions.assertEquals(200, statusCode, "HTTPS Response Status code is:" + statusCode);
                Assertions.assertNull(getLastException());
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
            detectIssue(header == null, "Request header was not found");
        }
    }

    class TestResponseInterceptor implements ResponseInterceptor {

        @Override
        public void process(MitmJavaProxyHttpResponse response) {
            Header[] headers = response.getRequestHeaders();
            Header h = response.findHeader(headers, "A");
            detectIssue(h == null, "'A' was not found at response interceptor");
            response.addHeader(new BasicHeader("B", response.getEntry().getMessageId()));
        }
    }
}
