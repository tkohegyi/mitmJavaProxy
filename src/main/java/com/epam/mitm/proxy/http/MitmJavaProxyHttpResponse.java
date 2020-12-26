package com.epam.mitm.proxy.http;

import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.jetty.util.URI;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MitmJavaProxyHttpResponse {
    private final boolean responseVolatile;
    private final HarEntry entry;
    private final HttpRequestBase method;
    private final URI proxyRequestURI;
    private final HttpResponse response;
    private final boolean contentMatched;
    private final String errorMessage;
    private final String body;
    private final String contentType;
    private final String charSet;
    private final int status;
    private final OutputStream os;
    private ByteArrayOutputStream bos;

    public MitmJavaProxyHttpResponse(int status, HarEntry entry, HttpRequestBase method, URI proxyRequestURI, HttpResponse response,
                                     boolean contentMatched, String errorMessage,
                                     String body, String contentType, String charSet,
                                     ByteArrayOutputStream bos, OutputStream os, boolean responseVolatile) {
        this.entry = entry;
        this.method = method;
        this.proxyRequestURI = proxyRequestURI;
        this.response = response;
        this.contentMatched = contentMatched;
        this.errorMessage = errorMessage;
        this.body = body;
        this.contentType = contentType;
        this.charSet = charSet;
        this.status = status;
        this.bos = bos;
        this.os = os;
        this.responseVolatile = responseVolatile;
    }

    public boolean isContentMatched() {
        return contentMatched;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
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

    public void doAnswer() {
        //only if response is volatile and well prepared
        if (!isResponseVolatile() || bos == null || os == null) {
            return;
        }
        //from bos write to os
        byte[] answer = bos.toByteArray();
        try {
            os.write(answer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
        }
    }

    public byte[] getAnswer() {
        //only if response is volatile
        if (bos == null) {
            return null;
        }
        return bos.toByteArray();
    }

    public void setAnswer(byte[] bytes) throws IOException {
        //only if response is volatile and well prepared
        if (!isResponseVolatile() || bos == null) {
            return;
        }
        IOUtils.closeQuietly(bos);
        bos = new ByteArrayOutputStream(bytes.length);
        IOUtils.write(bytes, bos);
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
}
