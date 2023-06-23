package website.magyar.mitm.proxy;

import website.magyar.mitm.proxy.http.MitmJavaProxyHttpRequest;

/**
 * Interface to be implemented in order to get access to requests of the Mitm Java Proxy.
 *
 * @author Tamas Kohegyi
 */
@FunctionalInterface
public interface RequestInterceptor {
    void process(MitmJavaProxyHttpRequest request);
}
