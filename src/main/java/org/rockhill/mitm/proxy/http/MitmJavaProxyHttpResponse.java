package org.rockhill.mitm.proxy.http;

import org.apache.http.Header;
import org.rockhill.mitm.jetty.server.Request;
import org.rockhill.mitm.jetty.server.Response;
import org.rockhill.mitm.proxy.header.HttpHeaderChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MitmJavaProxyHttpResponse {
    private final Logger logger = LoggerFactory.getLogger(MitmJavaProxyHttpResponse.class);
    private final Map<String, HttpHeaderChange> headerChanges = new HashMap<>();

    private final MitmJavaProxyHttpRequest mitmJavaProxyHttpRequest;
    private final Servlet servlet;
    private final Request baseRequest;
    private final ServletRequest request;
    private final ServletResponse response;

    public MitmJavaProxyHttpResponse(MitmJavaProxyHttpRequest mitmJavaProxyHttpRequest, Servlet servlet, Request baseRequest, ServletRequest request, ServletResponse response) {
        this.mitmJavaProxyHttpRequest = mitmJavaProxyHttpRequest;
        this.servlet = servlet;
        this.baseRequest = baseRequest;
        this.request = request;
        this.response = response;
    }


    /**
     * Gets the response body as String. Only available in case Proxy is working in 'captureContent' mode and the content type is not binary.
     * @return with the response body, or null, if body is not available.
     */
    public String getBodyString() {
        return null;
    }

    /**
     * Gets the response body as byte array. Returns null if body is not available.
     * Returns with raw bytes, so unzipping/deflating etc is up to you.
     * See getBodyString() too.
     *
     * @return with the response body content
     */
    public byte[] getBodyBytes() {
        byte[] result = null;
        return result;
    }

    /**
     * Updates the response body - in case it is volatile.
     *
     * @param newBody is the new body to be used as answer.
     */
    public void setBody(byte[] newBody) throws IOException {
    }

    /**
     * Gets the response content-type.
     * @return with the content type string or null if not defined.
     */
    public String getContentType() {
        return response.getContentType();
    }

    /**
     * Sets the content type of the response body.
     *
     * @param contentType is the type
     */
    public void setContentType(final String contentType) {
    }

    /**
     * Support method to search for a specific header in header array.
     * @param headers is the header array
     * @param key is the specific header to be searched for
     * @return with the header found, or null if not found
     */
    public Header findHeader(Header[] headers, String key) {
        Header header = null;
        for (Header h : headers) {
            if (h.getName().equals(key)) {
                header = h;
                break;
            }
        }
        return header;
    }

    public String getCharSet() {
        return response.getCharacterEncoding();
    }

    public String getHeader(String name) {
        return null;
    }

    public Header[] getHeaders() {
        return null;
    }

    public Header[] getRequestHeaders() {
        return null;
    }

    public int getStatus() {
        if (response instanceof Response) {
            return ((Response)response).getStatus();
        }
        return -1;
    }

    public boolean isResponseVolatile() {
        return false;
    }

    /**
     * Remove an existing Http header.
     *
     * @param header is the Header of the HTTP header to be removed
     */
    public void removeHeader(final Header header) {
        boolean found = false;
    }

    /**
     * Add a new HTTP header.
     *
     * @param header is the header to be added.
     */
    public void addHeader(final Header header) {
    }

    /**
     * Update an existing Http header.
     *
     * @param header   is the existing header that need to be changed
     * @param newValue is the new value to be used for the header.
     */
    public void updateHeader(final Header header, final String newValue) {
        boolean found = false;
        String key = header.getName();
        Header[] headers = getHeaders();
    }

}