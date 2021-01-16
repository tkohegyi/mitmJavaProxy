package org.rockhill.mitm.proxy.http;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.eclipse.jetty.http.HttpURI;
import org.rockhill.mitm.jetty.server.Request;
import org.rockhill.mitm.proxy.header.HttpHeaderChange;
import org.rockhill.mitm.proxy.header.HttpHeaderToBeAdded;
import org.rockhill.mitm.proxy.header.HttpHeaderToBeRemoved;
import org.rockhill.mitm.proxy.header.HttpHeaderToBeUpdated;
import org.rockhill.mitm.util.idgenerator.TimeStampBasedIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MitmJavaProxyHttpRequest {
    public static final TimeStampBasedIdGenerator TIME_STAMP_BASED_ID_GENERATOR = new TimeStampBasedIdGenerator();
    protected static final Logger logger = LoggerFactory.getLogger(MitmJavaProxyHttpRequest.class);
    private final String messageId = TIME_STAMP_BASED_ID_GENERATOR.nextIdentifier();
    //arrived with constructor
    private final Request request;
    private boolean responseVolatile = false;
    private final Map<String, Header> headers;
    private final List<HttpHeaderChange> headerChanges;
    private byte[] newBody;
    private HttpURI newHttpURI;

    public MitmJavaProxyHttpRequest(Request request) {
        this.request = request;
        //get headers
        headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            Header header = new BasicHeader(name, value);
            headers.put(name, header);
        }
        //prepare header changes
        headerChanges = new ArrayList<>();
        //prepare body change
        newBody = null;
        //prepare URI change
        newHttpURI = null;
    }

    public void updateRequest() {
        //update headers
        if (!headerChanges.isEmpty()) {
            throw new UnsupportedOperationException("Request update feature is not implemented");
        }
        //update body (transfer byte[] to InputStream)
        if (newBody != null) {
            throw new UnsupportedOperationException("Request update feature is not implemented");
        }
        //update target URI
        if (newHttpURI != null) {
            request.setHttpURI(newHttpURI);
        }
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

    public InputStream getPlayGround() {
        try {
            return request.getInputStream();
        } catch (IOException e) {
            return  null; //it is unavailable for some reason
        }
    }

    /**
     * Use this carefully, when you are sure that the content is a valid string - otherwise it may ruin the request.
     * @return with the request body as String
     */
    public String getBody() {
        InputStream clonedInputStream = getPlayGround();
        if (clonedInputStream != null) {
            try {
                int available = clonedInputStream.available();
                if (available > 0) {
                    clonedInputStream.mark(available);                 //read all available bytes
                    String body = IOUtils.toString(clonedInputStream); //now String "body" will contain the request body
                    clonedInputStream.reset();
                    return body;
                }
            } catch (IOException e) {
                logger.warn("Request interceptor may cause trouble in request", e);
                return null;
            }
        }
        return null;
    }

    public Header[] getAllHeaders() {
        Header[] headerArray = null;
        if (headers.size() > 0) {
            headerArray = new Header[headers.size()];
            int i = 0;
            for (Header h: headers.values()) {
                headerArray[i] = h;
                i++;
            }
        }
        return headerArray;
    }

    public Header getHeader(final String name) {
        return headers.get(name);
    }

    public void addHeader(final Header header) {
        headerChanges.add(new HttpHeaderToBeAdded(header));
    }

    public void updateHeader(final Header header, final String newValue) {
        headerChanges.add(new HttpHeaderToBeUpdated(header, newValue));
    }

    public void removeHeader(final Header header) {
        headerChanges.add(new HttpHeaderToBeRemoved(header));
    }

    public void setPlayGround(final byte[] newBody) {
        if (newBody != null) {
            this.newBody = Arrays.copyOf(newBody, newBody.length);
        }
    }

    public HttpURI getUri() {
        return request.getHttpURI();
    }

    public void setURI(final URI uri) {
        newHttpURI = new HttpURI(uri);
    }
}