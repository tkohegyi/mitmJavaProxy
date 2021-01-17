package org.rockhill.mitm.proxy.http;

import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.jetty.http.HttpFields;
import net.lightbody.bmp.proxy.jetty.http.HttpOutputStream;
import net.lightbody.bmp.proxy.jetty.util.URI;
import net.lightbody.bmp.proxy.util.ClonedOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.rockhill.mitm.proxy.header.HttpHeaderChange;
import org.rockhill.mitm.proxy.header.HttpHeaderToBeAdded;
import org.rockhill.mitm.proxy.header.HttpHeaderToBeRemoved;
import org.rockhill.mitm.proxy.header.HttpHeaderToBeUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MitmJavaProxyHttpResponse {
    private final Logger logger = LoggerFactory.getLogger(MitmJavaProxyHttpResponse.class);
    private final boolean responseVolatile;
    private final HarEntry entry;
    private final HttpRequestBase method;
    private final URI proxyRequestURI;
    private final HttpResponse response;
    private final String errorMessage;
    private final String body;
    private final String contentType;
    private final String charSet;
    private final int status;
    private final OutputStream os;
    private final Map<String, HttpHeaderChange> headerChanges = new HashMap<>();
    private ByteArrayOutputStream bos;

    public MitmJavaProxyHttpResponse(int status, HarEntry entry, HttpRequestBase method, URI proxyRequestURI, HttpResponse response,
                                     String errorMessage,
                                     String body, String contentType, String charSet,
                                     ByteArrayOutputStream bos, OutputStream os,
                                     final boolean responseVolatile) {
        this.entry = entry;
        this.method = method;
        this.proxyRequestURI = proxyRequestURI;
        this.response = response;
        this.errorMessage = errorMessage;
        this.body = body;
        this.contentType = contentType;
        this.charSet = charSet;
        this.status = status;
        this.bos = bos;
        this.os = os;
        this.responseVolatile = responseVolatile;
    }

    /**
     * Gets the response body as String. Only available in case Proxy is working in 'captureContent' mode and the content type is not binary.
     * @return with the response body, or null, if body is not available.
     */
    public String getBodyString() {
        return body;
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
        ByteArrayOutputStream copy = null;
        boolean enableWorkWithCopy = false;
        if (!responseVolatile && os != null && os instanceof ClonedOutputStream) {
            copy = ((ClonedOutputStream) os).getOutput();
            enableWorkWithCopy = true;
        }
        if (responseVolatile && bos != null) {
            enableWorkWithCopy = true;
            copy = bos;
        }
        if (enableWorkWithCopy) {
            result = copy.toByteArray();
        }
        return result;
    }

    /**
     * Updates the response body - in case it is volatile.
     *
     * @param newBody is the new body to be used as answer.
     */
    public void setBody(byte[] newBody) throws IOException {
        if (newBody != null && responseVolatile && bos != null) {
            String length = Integer.toString(newBody.length);
            //update os
            IOUtils.closeQuietly(bos);
            bos = new ByteArrayOutputStream(newBody.length);
            IOUtils.write(newBody, bos);
            //ensure that subsequent interceptors see this update too, including header update if necessary
            this.getRawResponse().setEntity(new ByteArrayEntity(newBody));
            Header header = this.getRawResponse().getFirstHeader(HttpFields.__ContentLength);
            if (header != null) {
                this.getRawResponse().removeHeader(header);
                this.updateHeader(header, length);
            }
        }
    }

    /**
     * Gets the response content-type.
     * @return with the content type string or null if not defined.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type of the response body.
     *
     * @param contentType is the type
     */
    public void setContentType(final String contentType) {
        Header header = findHeader(getRawResponse().getAllHeaders(), HttpFields.__ContentType);
        if (header != null) {
            updateHeader(header, contentType);
        } else {
            addHeader(new BasicHeader(HttpFields.__ContentType, contentType));
        }
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
        return charSet;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getHeader(String name) {
        Header header = response.getFirstHeader(name);
        if (header == null) {
            return null;
        }

        return header.getValue();
    }

    public Header[] getHeaders() {
        return response.getAllHeaders();
    }

    public Header[] getRequestHeaders() {
        return method.getAllHeaders();
    }

    public int getStatus() {
        return status;
    }

    public HttpResponse getRawResponse() {
        return response;
    }

    public HarEntry getEntry() {
        return entry;
    }

    /**
     * MitmJavaProxy internal call - support method to be used when a volatile response body is updated.
     * Never call it, otherwise the response will be messed up.
     *
     * @param response is the response inside HttpConnection - this nes to be updated
     */
    public void doAnswer(net.lightbody.bmp.proxy.jetty.http.HttpResponse response) {
        if (responseVolatile && !headerChanges.isEmpty()) {
            //update headers at req.proxyRequest._connection._response._header
            HttpFields httpFields = response.getHeader();

            for (Map.Entry<String, HttpHeaderChange> headerChangeEntry : headerChanges.entrySet()) {
                String key = headerChangeEntry.getKey();
                HttpHeaderChange httpHeaderChange = headerChangeEntry.getValue();
                String value = httpHeaderChange.getHeader().getValue();
                if (httpHeaderChange instanceof HttpHeaderToBeUpdated) { //update header
                    value = ((HttpHeaderToBeUpdated) httpHeaderChange).getNewValue();
                    httpFields.put(key, value);
                }
                if (httpHeaderChange instanceof HttpHeaderToBeAdded) { //add header
                    httpFields.add(key, value);
                }
                if (httpHeaderChange instanceof HttpHeaderToBeRemoved) { //remove header
                    httpFields.remove(key);
                }
            }
        }

        //prepare body update - only if response is volatile and well prepared
        if (!responseVolatile || bos == null || os == null) {
            return;
        }
        //from bos write to os
        byte[] answer = bos.toByteArray();
        try {
            ((HttpOutputStream) os).setContentLength(answer.length);
            response.setIntField(HttpFields.__ContentLength, answer.length); //this sets the length of to response
            os.write(answer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
        }
    }

    public boolean isResponseVolatile() {
        return responseVolatile;
    }

    public HttpRequestBase getMethod() {
        return method;
    }

    public URI getProxyRequestURI() {
        return proxyRequestURI;
    }

    /**
     * Remove an existing Http header.
     *
     * @param header is the Header of the HTTP header to be removed
     */
    public void removeHeader(final Header header) {
        boolean found = false;
        if (header != null) {
            String key = header.getName();
            Header[] headers = getHeaders();
            for (Header h : headers) {
                if (header.getName().equals(h.getName()) && header.getValue().equals(h.getValue())) {
                    found = true;
                    getRawResponse().removeHeader(h);
                    HttpHeaderToBeRemoved headerToBeRemoved = new HttpHeaderToBeRemoved(header);
                    headerChanges.put(key, headerToBeRemoved);
                }
            }
            if (!found) {
                logger.warn("Header with key: {} not found, remove header skipped.", key);
            }
        } else {
            logger.warn("removeHeader called with null");
        }
    }

    /**
     * Add a new HTTP header.
     *
     * @param header is the header to be added.
     */
    public void addHeader(final Header header) {
        if (header != null) {
            HttpResponse httpResponse = getRawResponse();
            if (httpResponse != null) {
                httpResponse.addHeader(header);
                HttpHeaderToBeAdded httpHeaderToBeAdded = new HttpHeaderToBeAdded(header);
                headerChanges.put(header.getName(), httpHeaderToBeAdded);
            } else {
                logger.warn("addHeader called without accessible response");
            }
        } else {
            logger.warn("addHeader called with null");
        }
    }

    /**
     * Update an existing Http header.
     *
     * @param header   is the existing header that need to be changed
     * @param newValue is the new value to be used for the header.
     */
    public void updateHeader(final Header header, final String newValue) {
        boolean found = false;
        if (header != null && newValue != null) {
            String key = header.getName();
            Header[] headers = getHeaders();
            for (Header h : headers) {
                if (header.getName().equals(h.getName()) && header.getValue().equals(h.getValue())) {
                    found = true;
                    getRawResponse().removeHeader(h);
                    getRawResponse().addHeader(key, newValue);
                    HttpHeaderToBeUpdated httpHeaderToBeUpdated = new HttpHeaderToBeUpdated(header, newValue);
                    headerChanges.put(key, httpHeaderToBeUpdated);
                }
            }
            if (!found) {
                Header h = new BasicHeader(header.getName(), newValue);
                addHeader(h);
            }
        } else {
            logger.warn("updateHeader called with null");
        }
    }

}
