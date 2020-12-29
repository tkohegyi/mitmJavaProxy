package org.rockhill.mitm.proxy;

import org.rockhill.mitm.proxy.http.MitmJavaProxyHttpRequest;

/**
 * Interface to be implemented in order to get access to requests of the Mitm Java Proxy.
 *
 * @author Tamas Kohegyi
 */
public interface RequestInterceptor {
    void process(MitmJavaProxyHttpRequest request);
}
