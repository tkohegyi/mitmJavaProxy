package org.rockhill.mitm.proxy;

import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpResponse;

/**
 * Interface to be implemented in order to get access to responses of the Mitm Java Proxy.
 *
 * @author Tamas Kohegyi
 */
public interface ResponseInterceptor {
    void process(MitmJavaProxyHttpResponse response);
}
