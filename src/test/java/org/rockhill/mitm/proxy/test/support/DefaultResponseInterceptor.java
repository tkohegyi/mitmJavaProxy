package org.rockhill.mitm.proxy.test.support;
import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpResponse;
import org.rockhill.mitm.proxy.ResponseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that is able to intercept and process every response going through the proxy, by implementing the ResponseInterceptor interface.
 * It logs every response that is intercepted.
 *
 * @Tamas_Kohegyi
 */
public class DefaultResponseInterceptor implements ResponseInterceptor {

    private final Logger logger = LoggerFactory.getLogger(DefaultResponseInterceptor.class);

    private AtomicInteger responseCount;

    public DefaultResponseInterceptor() {
        this.responseCount = new AtomicInteger(0);
    }

    public void process(final MitmJavaProxyHttpResponse response) {
        responseCount.incrementAndGet();
        logger.info("Response Interceptor Called, status: {}", response.getStatus());
    }

    public int getResponseCount() {
        return this.responseCount.get();
    }
}