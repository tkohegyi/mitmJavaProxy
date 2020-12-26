package net.lightbody.bmp.proxy.http;

public interface RequestInterceptor {
    void process(MitmJavaProxyHttpRequest request);
}
