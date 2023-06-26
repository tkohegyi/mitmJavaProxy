package website.magyar.mitm.proxy;

import website.magyar.mitm.proxy.http.MitmJavaProxyHttpResponse;

/**
 * Interface to be implemented in order to get access to responses of the Mitm Java Proxy.
 *
 * @author Tamas Kohegyi
 */
@FunctionalInterface
public interface ResponseInterceptor {
    void process(MitmJavaProxyHttpResponse response);
}
