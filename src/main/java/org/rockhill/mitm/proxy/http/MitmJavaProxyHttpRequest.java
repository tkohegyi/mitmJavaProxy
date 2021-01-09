package org.rockhill.mitm.proxy.http;

import org.rockhill.mitm.idgenerator.TimeStampBasedIdGenerator;
import org.rockhill.mitm.jetty.proxy.ConnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentMap;

public class MitmJavaProxyHttpRequest {
    public static final TimeStampBasedIdGenerator TIME_STAMP_BASED_ID_GENERATOR = new TimeStampBasedIdGenerator();
    protected static final Logger logger = LoggerFactory.getLogger(MitmJavaProxyHttpRequest.class);
    private final String messageId = TIME_STAMP_BASED_ID_GENERATOR.nextIdentifier();
    private boolean responseVolatile = false;

    //arrived with constructor
    private final HttpServletRequest request;
    private final ConcurrentMap<String, Object> context;
    private final ConnectHandler.UpstreamConnection upstreamConnection;

    public MitmJavaProxyHttpRequest(HttpServletRequest request, ConcurrentMap<String, Object> context, ConnectHandler.UpstreamConnection upstreamConnection) {
        this.request = request;
        this.context = context;
        this.upstreamConnection = upstreamConnection;
    }

    public String getMessageId() {
        return messageId;
    }

    public boolean getResponseVolatile() {
        return responseVolatile;
    }

    public void setResponseVolatile(final boolean responseVolatile) {
        this.responseVolatile = responseVolatile;
    }

    public HttpServletRequest getRequest() {
        return request;
    }
}