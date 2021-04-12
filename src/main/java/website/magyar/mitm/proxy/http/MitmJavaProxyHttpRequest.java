package website.magyar.mitm.proxy.http;

import website.magyar.mitm.idgenerator.TimeStampBasedIdGenerator;
import net.lightbody.bmp.proxy.http.BrowserMobHttpClient;
import net.lightbody.bmp.proxy.http.RequestCallback;
import net.lightbody.bmp.proxy.jetty.http.HttpRequest;
import net.lightbody.bmp.proxy.util.Base64;
import net.lightbody.bmp.proxy.util.ClonedInputStream;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

public class MitmJavaProxyHttpRequest {
    public static final TimeStampBasedIdGenerator TIME_STAMP_BASED_ID_GENERATOR = new TimeStampBasedIdGenerator();
    protected static final Logger logger = LoggerFactory.getLogger(MitmJavaProxyHttpRequest.class);
    private final HttpRequestBase method;
    private final BrowserMobHttpClient client;
    private final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    private final boolean collectAdditionalInfo;
    private final HttpRequest proxyRequest;
    private final String messageId = TIME_STAMP_BASED_ID_GENERATOR.nextIdentifier();
    private int expectedStatusCode;
    private StringEntity stringEntity;
    private ByteArrayEntity byteArrayEntity;
    private InputStreamEntity inputStreamEntity;
    private MultipartEntityBuilder multipartEntityBuilder;
    private OutputStream outputStream;
    private RequestCallback requestCallback;
    private ByteArrayOutputStream copy;
    private String expectedLocation;
    private boolean multiPart;
    private InputStream playGround;
    private boolean responseVolatile = false;

    public MitmJavaProxyHttpRequest(final HttpRequestBase method, final BrowserMobHttpClient client, final int expectedStatusCode,
                                    final boolean collectAdditionalInfo, final HttpRequest proxyRequest) {
        this.method = method;
        this.client = client;
        this.expectedStatusCode = expectedStatusCode;
        this.collectAdditionalInfo = collectAdditionalInfo;
        this.proxyRequest = proxyRequest;
    }

    public String getExpectedLocation() {
        return expectedLocation;
    }

    public void setExpectedLocation(final String location) {
        expectedLocation = location;
    }

    public void addRequestHeader(final String key, final String value) {
        method.addHeader(key, value);
    }

    public void addRequestParameter(final String key, final String value) {
        nvps.add(new BasicNameValuePair(key, value));
    }

    public void setRequestBody(String body, String contentType, String charSet) {
        try {
            stringEntity = new StringEntity(body, ContentType.create(contentType, charSet));
        } catch (UnsupportedCharsetException e) {
            stringEntity = new StringEntity(body, ContentType.create(contentType, (String) null));
        }
    }

    public void setRequestBody(final String body) {
        setRequestBody(body, null, "UTF-8");
    }

    public void setRequestBodyAsBase64EncodedBytes(final String bodyBase64Encoded) {
        byteArrayEntity = new ByteArrayEntity(Base64.base64ToByteArray(bodyBase64Encoded));
    }

    public void setRequestInputStream(InputStream is, final long length) {
        if (collectAdditionalInfo) {
            ClonedInputStream cis = new ClonedInputStream(is);
            is = cis;
            copy = cis.getOutput();
        }

        inputStreamEntity = new InputStreamEntity(is, length);
    }

    public HttpRequestBase getMethod() {
        return method;
    }

    public HttpRequest getProxyRequest() {
        return proxyRequest;
    }

    public void makeMultiPart() {
        if (!multiPart) {
            multiPart = true;
            multipartEntityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        }
    }

    //MAIN METHOD
    public MitmJavaProxyHttpResponse execute() {
        // deal with PUT/POST requests
        if (method instanceof HttpEntityEnclosingRequestBase) {
            HttpEntityEnclosingRequestBase enclosingRequest = (HttpEntityEnclosingRequestBase) method;

            if (!nvps.isEmpty()) {
                try {
                    if (!multiPart) {
                        enclosingRequest.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                    } else {
                        for (NameValuePair nvp : nvps) {
                            multipartEntityBuilder.addPart(nvp.getName(), new StringBody(nvp.getValue()));
                        }
                        enclosingRequest.setEntity(multipartEntityBuilder.build());
                    }
                } catch (UnsupportedEncodingException e) {
                    logger.error("Could not find UTF-8 encoding, something is really wrong", e);
                }
            } else if (multipartEntityBuilder != null) {
                enclosingRequest.setEntity(multipartEntityBuilder.build());
            } else if (byteArrayEntity != null) {
                enclosingRequest.setEntity(byteArrayEntity);
            } else if (stringEntity != null) {
                enclosingRequest.setEntity(stringEntity);
            } else if (inputStreamEntity != null) {
                enclosingRequest.setEntity(inputStreamEntity);
            }
        }

        return client.execute(this);
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(final int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public RequestCallback getRequestCallback() {
        return requestCallback;
    }

    public void setRequestCallback(final RequestCallback requestCallback) {
        this.requestCallback = requestCallback;
    }

    public ByteArrayOutputStream getCopy() {
        return copy;
    }

    public InputStream getPlayGround() {
        return playGround;
    }

    public void setPlayGround(final InputStream playGround) {
        this.playGround = playGround;
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

    /**
     * Set the body of the request.
     *
     * @param body is the new content of the request. If null, then the original body is not changed.
     */
    public void setBody(byte[] body) {
        if (body != null) {
            if (this.getMethod() instanceof HttpEntityEnclosingRequestBase) {
                HttpEntityEnclosingRequestBase enclosingRequest = (HttpEntityEnclosingRequestBase) this.getMethod();
                enclosingRequest.setEntity(new ByteArrayEntity(body));
            }
        }
    }
}
