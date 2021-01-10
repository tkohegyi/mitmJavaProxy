package org.rockhill.mitm.proxy.http;

import org.rockhill.mitm.idgenerator.TimeStampBasedIdGenerator;
import org.rockhill.mitm.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class MitmJavaProxyHttpRequest {
    public static final TimeStampBasedIdGenerator TIME_STAMP_BASED_ID_GENERATOR = new TimeStampBasedIdGenerator();
    protected static final Logger logger = LoggerFactory.getLogger(MitmJavaProxyHttpRequest.class);
    private final String messageId = TIME_STAMP_BASED_ID_GENERATOR.nextIdentifier();
    private boolean responseVolatile = false;

    //arrived with constructor
    private final Servlet servlet;
    private final Request baseRequest;
    private final ServletRequest request;
    private final ServletResponse response;

    public MitmJavaProxyHttpRequest(Servlet servlet, Request baseRequest, ServletRequest request, ServletResponse response) {
        this.servlet = servlet;
        this.baseRequest = baseRequest;
        this.request = request;
        this.response = response;
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
        if (request instanceof HttpServletRequest) {
            return (HttpServletRequest) request;
        }
        return null;
    }
}