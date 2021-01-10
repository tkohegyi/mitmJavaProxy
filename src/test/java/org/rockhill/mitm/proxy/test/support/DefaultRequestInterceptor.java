package org.rockhill.mitm.proxy.test.support;

import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;
import org.rockhill.mitm.proxy.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that is able to intercept and process every request going through the proxy, by implementing the RequestInterceptor interface.
 * It logs every request that is intercepted.
 *
 * @Tamas_Kohegyi
 */
public class DefaultRequestInterceptor implements RequestInterceptor {

    private final Logger logger = LoggerFactory.getLogger(DefaultRequestInterceptor.class);

    private AtomicInteger requestCount;
    private String stubRequestPattern;
    private URI stubUri;


    public DefaultRequestInterceptor() {
        this.requestCount = new AtomicInteger(0);
        this.stubRequestPattern = null;
        this.stubUri = null;
    }


    public void process(final MitmJavaProxyHttpRequest request) {
        requestCount.incrementAndGet();
/*        String uriPath = request.getRequest().getPathInfo();
        if (stubRequestPattern != null && stubUri != null && uriPath.contains(stubRequestPattern)) {
            //request.getRequest().getMethod().setURI(stubUri);
            logger.info("Request Interceptor Called - Redirect to STUB: {}", stubUri.toString());
        } else {
            logger.info("Request Interceptor Called - Request untouched.");
        }

 */
        logger.info("Request Interceptor Called - Request untouched.");
    }

    public int getRequestCount() {
        return requestCount.get();
    }

}