package com.epam.mitm.proxy;

import com.epam.mitm.proxy.http.MitmJavaProxyHttpRequest;

public interface RequestInterceptor {
    void process(MitmJavaProxyHttpRequest request);
}
