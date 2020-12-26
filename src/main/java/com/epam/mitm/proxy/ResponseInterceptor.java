package com.epam.mitm.proxy;

import com.epam.mitm.proxy.http.MitmJavaProxyHttpResponse;

public interface ResponseInterceptor {
    void process(MitmJavaProxyHttpResponse response);
}
