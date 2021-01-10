package org.rockhill.mitm.proxy.http;

import org.rockhill.mitm.jetty.server.Request;
import org.rockhill.mitm.util.idgenerator.TimeStampBasedIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MitmJavaProxyHttpRequest {
    public static final TimeStampBasedIdGenerator TIME_STAMP_BASED_ID_GENERATOR = new TimeStampBasedIdGenerator();
    protected static final Logger logger = LoggerFactory.getLogger(MitmJavaProxyHttpRequest.class);
    private final String messageId = TIME_STAMP_BASED_ID_GENERATOR.nextIdentifier();
    //arrived with constructor
    private final Request request;
    private boolean responseVolatile = false;

    public MitmJavaProxyHttpRequest(Request request) {
        this.request = request;
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

    public Request getRequest() {
        return request;
    }
}