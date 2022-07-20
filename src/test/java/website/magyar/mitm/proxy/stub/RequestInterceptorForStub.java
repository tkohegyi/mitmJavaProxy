package website.magyar.mitm.proxy.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import website.magyar.mitm.proxy.RequestInterceptor;
import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class that is able to intercept and process every request going through the proxy, by implementing the RequestInterceptor interface.
 * It redirects all incoming message to the stub service - which acts as service virtualization.
 *
 * @author Tamas_Kohegyi
 */
public class RequestInterceptorForStub implements RequestInterceptor {

    private final Logger logger = LoggerFactory.getLogger(RequestInterceptorForStub.class);

    private URI stubUri;

    public RequestInterceptorForStub(String stubUrl) {
        try {
            this.stubUri = new URI(stubUrl);
        } catch (URISyntaxException e) {
            this.stubUri = null;
        }
    }

    public void process(final MitmJavaProxyHttpRequest request) {
        request.getMethod().setURI(stubUri);
        logger.info("Redirecting request:{} to stub service.", request.getMethod().getURI().getPath());
    }

}