package net.lightbody.bmp.proxy.http;

import com.epam.mitm.proxy.ProxyServer;
import com.epam.mitm.proxy.RequestInterceptor;
import com.epam.mitm.proxy.ResponseInterceptor;
import com.epam.mitm.proxy.http.MitmJavaProxyHttpRequest;
import com.epam.mitm.proxy.http.MitmJavaProxyHttpResponse;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarCookie;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarPostData;
import net.lightbody.bmp.core.har.HarPostDataParam;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import net.lightbody.bmp.core.har.HarTimings;
import net.lightbody.bmp.proxy.util.Base64;
import net.lightbody.bmp.proxy.util.CappedByteArrayOutputStream;
import net.lightbody.bmp.proxy.util.ClonedOutputStream;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.java_bandwidthlimiter.StreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Cache;
import org.xbill.DNS.DClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class BrowserMobHttpClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(BrowserMobHttpClient.class);
    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // MOB-216 don't buffer more than 1 MB
    private static final int BUFFER = 4096;
    private static final int MAX_REDIRECT = 10;

    private final List<RewriteRule> rewriteRules = new CopyOnWriteArrayList<RewriteRule>();
    private final List<RequestInterceptor> requestInterceptors = new CopyOnWriteArrayList<RequestInterceptor>();
    private final List<ResponseInterceptor> responseInterceptors = new CopyOnWriteArrayList<ResponseInterceptor>();
    private final HashMap<String, String> additionalHeaders = new LinkedHashMap<String, String>();
    private final AtomicBoolean allowNewRequests = new AtomicBoolean(true);
    // not using CopyOnWriteArray because we're WRITE heavy and it is for READ heavy operations
    // instead doing it the old fashioned way with a synchronized block
    private final Set<ActiveRequest> activeRequests = new HashSet<ActiveRequest>();
    private Har har;
    private String harPageRef;
    private boolean captureHeaders;
    private boolean captureContent; // if captureContent is set, default policy is to capture binary contents too
    private boolean captureBinaryContent = true;
    private final SimulatedSocketFactory socketFactory;
    private final TrustingSSLSocketFactory sslSocketFactory;
    private final PoolingHttpClientConnectionManager httpClientConnMgr;
    private final HttpClient httpClient;
    private int requestTimeout;
    private BrowserMobHostNameResolver hostNameResolver;
    private boolean decompress = true;
    private WildcardMatchingCredentialsProvider credsProvider;
    private boolean shutdown = false;

    private boolean followRedirects = true;
    private AtomicInteger requestCounter;

    public BrowserMobHttpClient(final StreamManager streamManager, final AtomicInteger requestCounter, final int requestTimeOut) {
        this.requestCounter = requestCounter;
        this.requestTimeout = requestTimeOut;
        hostNameResolver = new BrowserMobHostNameResolver(new Cache(DClass.ANY));

        socketFactory = new SimulatedSocketFactory(hostNameResolver, streamManager, requestTimeout);
        try {
            sslSocketFactory = new TrustingSSLSocketFactory(hostNameResolver, streamManager, requestTimeout);
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", socketFactory)
                .register("https", sslSocketFactory)
                .build();

        httpClientConnMgr = new PoolingHttpClientConnectionManager(registry) {
            public ConnectionRequest requestConnection(
                    final HttpRoute route,
                    final Object state) {
                final ConnectionRequest wrapped = super.requestConnection(route, state);
                return new ConnectionRequest() {
                    @Override
                    public boolean cancel() {
                        return wrapped.cancel();
                    }

                    @Override
                    public HttpClientConnection get(
                            final long timeout,
                            final TimeUnit timeUnit) throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
                        Date start = new Date();
                        try {
                            return wrapped.get(timeout, timeUnit);
                        } finally {
                            RequestInfo.get().blocked(start, new Date());
                        }
                    }
                };
            }
        };

        // MOB-338: 30 total connections and 6 connections per host matches the behavior in Firefox 3
        httpClientConnMgr.setMaxTotal(30);
        httpClientConnMgr.setDefaultMaxPerRoute(6);
        credsProvider = new WildcardMatchingCredentialsProvider();

        httpClient = HttpClientBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setConnectionManager(httpClientConnMgr)
                .setRequestExecutor(new SimulatedRequestExecutor())
                .setDefaultCredentialsProvider(credsProvider)
                .addInterceptorFirst(new PreemptiveAuth())
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                .setDefaultCookieStore(new BlankCookieStore())
                .setDefaultCookieSpecRegistry(
                        RegistryBuilder.<CookieSpecProvider>create().register("easy", new CookieSpecProvider() {
                                    @Override
                                    public CookieSpec create(HttpContext context) {
                                        return new BrowserCompatSpec() {
                                            @Override
                                            public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
                                                // easy!
                                            }
                                        };
                                    }
                                }
                        ).build())
                .disableRedirectHandling() //setRedirectStrategy(new LaxRedirectStrategy())
                .setConnectionTimeToLive(requestTimeOut, TimeUnit.MILLISECONDS)
                .build();


        HttpClientInterrupter.watch(this);
    }

    public void remapHost(final String source, final String target) {
        hostNameResolver.remap(source, target);
    }

    public void addRequestInterceptor(final RequestInterceptor interceptor) {
        requestInterceptors.add(interceptor);
    }

    public void addResponseInterceptor(final ResponseInterceptor interceptor) {
        responseInterceptors.add(interceptor);
    }

    public MitmJavaProxyHttpRequest newPost(final String url, final net.lightbody.bmp.proxy.jetty.http.HttpRequest proxyRequest) {
        try {
            URI uri = makeUri(url);
            return new MitmJavaProxyHttpRequest(new HttpPost(uri), this, -1, captureContent, proxyRequest);
        } catch (URISyntaxException e) {
            throw reportBadURI(url, "POST");
        }
    }

    public MitmJavaProxyHttpRequest newGet(final String url, final net.lightbody.bmp.proxy.jetty.http.HttpRequest proxyRequest) {
        try {
            URI uri = makeUri(url);
            return new MitmJavaProxyHttpRequest(new HttpGet(uri), this, -1, captureContent, proxyRequest);
        } catch (URISyntaxException e) {
            throw reportBadURI(url, "GET");
        }
    }

    public MitmJavaProxyHttpRequest newPut(final String url, final net.lightbody.bmp.proxy.jetty.http.HttpRequest proxyRequest) {
        try {
            URI uri = makeUri(url);
            return new MitmJavaProxyHttpRequest(new HttpPut(uri), this, -1, captureContent, proxyRequest);
        } catch (Exception e) {
            throw reportBadURI(url, "PUT");
        }
    }

    public MitmJavaProxyHttpRequest newDelete(final String url, final net.lightbody.bmp.proxy.jetty.http.HttpRequest proxyRequest) {
        try {
            URI uri = makeUri(url);
            return new MitmJavaProxyHttpRequest(new HttpDelete(uri), this, -1, captureContent, proxyRequest);
        } catch (URISyntaxException e) {
            throw reportBadURI(url, "DELETE");
        }
    }

    public MitmJavaProxyHttpRequest newOptions(final String url, final net.lightbody.bmp.proxy.jetty.http.HttpRequest proxyRequest) {
        try {
            URI uri = makeUri(url);
            return new MitmJavaProxyHttpRequest(new HttpOptions(uri), this, -1, captureContent, proxyRequest);
        } catch (URISyntaxException e) {
            throw reportBadURI(url, "OPTIONS");
        }
    }

    public MitmJavaProxyHttpRequest newHead(final String url, final net.lightbody.bmp.proxy.jetty.http.HttpRequest proxyRequest) {
        try {
            URI uri = makeUri(url);
            return new MitmJavaProxyHttpRequest(new HttpHead(uri), this, -1, captureContent, proxyRequest);
        } catch (URISyntaxException e) {
            throw reportBadURI(url, "HEAD");
        }
    }

    private URI makeUri(String url) throws URISyntaxException {
        // MOB-120: check for | character and change to correctly escaped %7C
        url = url.replace(" ", "%20");
        url = url.replace(">", "%3C");
        url = url.replace("<", "%3E");
        url = url.replace("#", "%23");
        url = url.replace("{", "%7B");
        url = url.replace("}", "%7D");
        url = url.replace("|", "%7C");
        url = url.replace("\\", "%5C");
        url = url.replace("^", "%5E");
        url = url.replace("~", "%7E");
        url = url.replace("[", "%5B");
        url = url.replace("]", "%5D");
        url = url.replace("`", "%60");
        url = url.replace("\"", "%22");

        URI uri = new URI(url);

        // are we using the default ports for http/https? if so, let's rewrite the URI to make sure the :80 or :443
        // is NOT included in the string form the URI. The reason we do this is that in HttpClient 4.0 the Host header
        // would include a value such as "yahoo.com:80" rather than "yahoo.com". Not sure why this happens but we don't
        // want it to, and rewriting the URI solves it
        if ((uri.getPort() == 80 && "http".equals(uri.getScheme())) || (uri.getPort() == 443 && "https".equals(uri.getScheme()))) {
            // we rewrite the URL with a StringBuilder (vs passing in the components of the URI) because if we were
            // to pass in these components using the URI's 7-arg constructor query parameters get double escaped (bad!)
            StringBuilder sb = new StringBuilder(uri.getScheme()).append("://");
            if (uri.getRawUserInfo() != null) {
                sb.append(uri.getRawUserInfo()).append("@");
            }
            sb.append(uri.getHost());
            if (uri.getRawPath() != null) {
                sb.append(uri.getRawPath());
            }
            if (uri.getRawQuery() != null) {
                sb.append("?").append(uri.getRawQuery());
            }
            if (uri.getRawFragment() != null) {
                sb.append("#").append(uri.getRawFragment());
            }

            uri = new URI(sb.toString());
        }
        return uri;
    }

    private RuntimeException reportBadURI(final String url, final String method) {
        if (har != null && harPageRef != null) {
            HarEntry entry = new HarEntry(harPageRef, MitmJavaProxyHttpRequest.TIME_STAMP_BASED_ID_GENERATOR.nextIdentifier());
            entry.setTime(0);
            entry.setRequest(new HarRequest(method, url, "HTTP/1.1"));
            entry.setResponse(new HarResponse(-998, "Bad URI", "HTTP/1.1"));
            entry.setTimings(new HarTimings());
            har.getLog().addEntry(entry);
        }

        throw new BadURIException("Bad URI requested: " + url);
    }

    public void checkTimeout() {
        synchronized (activeRequests) {
            for (ActiveRequest activeRequest : activeRequests) {
                activeRequest.checkTimeout();
            }
        }
    }

    //MAIN METHOD TO HANDLE A REQUEST AND PREPARE A RESULT
    public MitmJavaProxyHttpResponse execute(final MitmJavaProxyHttpRequest req) {
        if (!allowNewRequests.get()) {
            throw new RuntimeException("No more requests allowed");
        }

        try {
            boolean isResponseVolatile = ProxyServer.getResponseVolatile(); //this is the base of response volatility
            req.setResponseVolatile(isResponseVolatile);

            requestCounter.incrementAndGet();
            for (RequestInterceptor interceptor : requestInterceptors) {
                interceptor.process(req);
            }

            // Response volatility might be overwritten in request interceptors, but not later, so from now it is fixed:
            isResponseVolatile = req.getResponseVolatile();

            MitmJavaProxyHttpResponse response = execute(req, 1, isResponseVolatile);

            for (ResponseInterceptor interceptor : responseInterceptors) {
                interceptor.process(response);
            }

            if (isResponseVolatile) {
                response.doAnswer();
            }
            return response;
        } finally {
            requestCounter.decrementAndGet();
        }
    }

    //
    //If we were making cake, this would be the filling :)
    //Sending the prepared - maybe altered - request to the server, and getting back the result
    //
    private MitmJavaProxyHttpResponse execute(final MitmJavaProxyHttpRequest req, int depth, boolean isResponseVolatile) {
        if (depth >= MAX_REDIRECT) {
            throw new IllegalStateException("Max number of redirects (" + MAX_REDIRECT + ") reached");
        }

        RequestCallback callback = req.getRequestCallback();

        HttpRequestBase method = req.getMethod();
        String url = method.getURI().toString();

        // process any rewrite requests
        boolean rewrote = false;
        String newUrl = url;
        for (RewriteRule rule : rewriteRules) {
            Matcher matcher = rule.match.matcher(newUrl);
            newUrl = matcher.replaceAll(rule.replace);
            rewrote = true;
        }

        if (rewrote) {
            try {
                method.setURI(new URI(newUrl));
                url = newUrl;
            } catch (URISyntaxException e) {
                LOGGER.warn("Could not rewrite url to {}", newUrl);
            }
        }

        if (!additionalHeaders.isEmpty()) {
            // Set the additional headers
            for (Map.Entry<String, String> entry : additionalHeaders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                method.removeHeaders(key);
                method.addHeader(key, value);
            }
        }

        String charSet = "UTF-8";
        String responseBody = null;

        InputStream is = null;
        int statusCode = -998;
        long bytes = 0;
        boolean gzipping = false;
        boolean deflating = false;
        boolean contentMatched = true;
        OutputStream os = req.getOutputStream();
        if (os == null) {
            os = new CappedByteArrayOutputStream(MAX_BUFFER_SIZE);
        }

        // link the object up now, before we make the request, so that if we get cut off (ie: favicon.ico request and browser shuts down)
        // we still have the attempt associated, even if we never got a response
        HarEntry entry = new HarEntry(harPageRef, req.getWilmaMessageId());

        // clear out any connection-related information so that it's not stale from previous use of this thread.
        RequestInfo.clear(url, entry);

        entry.setRequest(new HarRequest(method.getMethod(), url, method.getProtocolVersion().getProtocol()));
        entry.setResponse(new HarResponse(-999, "NO RESPONSE", method.getProtocolVersion().getProtocol()));
        if (har != null && harPageRef != null) {
            har.getLog().addEntry(entry);
        }

        String errorMessage = null;
        HttpResponse response = null;

        BasicHttpContext ctx = new BasicHttpContext();

        ActiveRequest activeRequest = new ActiveRequest(method, ctx, entry.getStartedDateTime());
        synchronized (activeRequests) {
            activeRequests.add(activeRequest);
        }

        StatusLine statusLine = null;
        ByteArrayOutputStream bos = null;

        try {
            // set the User-Agent if it's not already set
            if (method.getHeaders("User-Agent").length == 0) {
                method.addHeader("User-Agent", "MITM-JavaProxy V/1.0");
            }

            response = httpClient.execute(method, ctx);

            statusLine = response.getStatusLine();
            statusCode = statusLine.getStatusCode();

            if (callback != null) {
                callback.handleStatusLine(statusLine);
                callback.handleHeaders(response.getAllHeaders());
            }

            if (response.getEntity() != null) {
                is = response.getEntity().getContent();
            }

            // check for null (resp 204 can cause HttpClient to return null, which is what Google does with http://clients1.google.com/generate_204)
            if (is != null) {
                Header contentEncodingHeader = response.getFirstHeader("Content-Encoding");
                if (contentEncodingHeader != null) {
                    if ("gzip".equalsIgnoreCase(contentEncodingHeader.getValue())) {
                        gzipping = true;
                    } else if ("deflate".equalsIgnoreCase(contentEncodingHeader.getValue())) {
                        deflating = true;
                    }
                }

                // deal with GZIP content!
                if (decompress && response.getEntity().getContentLength() != 0) { //getContentLength<0 if unknown
                    if (gzipping) {
                        is = new GZIPInputStream(is);
                    } else if (deflating) {  //RAW deflate only
                        // WARN : if system is using zlib<=1.1.4 the stream must be append with a dummy byte
                        // that is not required for zlib>1.1.4 (not mentioned on current Inflater javadoc)
                        is = new InflaterInputStream(is, new Inflater(true));
                    }
                }

                if (isResponseVolatile) {
                    //response content is volatile
                    bytes = is.available();
                    bos = new ByteArrayOutputStream();
                    org.apache.commons.io.IOUtils.copy(is, bos);
                } else {
                    //response content is not volatile
                    if (captureContent) {
                        os = new ClonedOutputStream(os);
                    }
                    bytes = copyWithStatsDynamic(is, os); //if copied to os, then response gone back
                }
            }
        } catch (Exception e) {
            errorMessage = e.toString();
            if (callback != null) {
                if (activeRequest.wasTimeout) {
                    e = new ConnectTimeoutException();
                }
                callback.reportError(e);
            }

            // only log it if we're not shutdown (otherwise, errors that happen during a shutdown can likely be ignored)
            if (!shutdown) {
                LOGGER.info("{} when requesting {}", errorMessage, url);
            }
        } finally {
            // the request is done, get it out of here
            synchronized (activeRequests) {
                activeRequests.remove(activeRequest);
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // this is OK to ignore
                }
            }
        }

        // record the response as ended
        RequestInfo.get().finish();

        // set the start time and other timings
        entry.setStartedDateTime(RequestInfo.get().getStart());
        entry.setTimings(RequestInfo.get().getTimings());
        entry.setServerIPAddress(RequestInfo.get().getResolvedAddress());
        entry.setTime(RequestInfo.get().getTotalTime());

        entry.getResponse().setBodySize(bytes);
        entry.getResponse().getContent().setSize(bytes);
        entry.getResponse().setStatus(statusCode);
        if (statusLine != null) {
            entry.getResponse().setStatusText(statusLine.getReasonPhrase());
        }

        boolean urlEncoded = false;
        if (captureHeaders || captureContent) {
            for (Header header : method.getAllHeaders()) {
                if (header.getValue() != null && header.getValue().startsWith(URLEncodedUtils.CONTENT_TYPE)) {
                    urlEncoded = true;
                }

                entry.getRequest().getHeaders().add(new HarNameValuePair(header.getName(), header.getValue()));
            }

            if (response != null) {
                for (Header header : response.getAllHeaders()) {
                    entry.getResponse().getHeaders().add(new HarNameValuePair(header.getName(), header.getValue()));
                }
            }
        }

        if (captureContent) {
            // can we understand the POST data at all?
            if (method instanceof HttpEntityEnclosingRequestBase && req.getCopy() != null) {
                HttpEntityEnclosingRequestBase enclosingReq = (HttpEntityEnclosingRequestBase) method;
                HttpEntity entity = enclosingReq.getEntity();

                HarPostData data = new HarPostData();
                data.setMimeType(req.getMethod().getFirstHeader("Content-Type").getValue());
                entry.getRequest().setPostData(data);

                if (urlEncoded || URLEncodedUtils.isEncoded(entity)) {
                    try {
                        final String content = new String(req.getCopy().toByteArray(), "UTF-8");
                        if (content != null && content.length() > 0) {
                            List<NameValuePair> result = new ArrayList<NameValuePair>();
                            URLEncodedUtils.parse(result, new Scanner(content), null);

                            ArrayList<HarPostDataParam> params = new ArrayList<HarPostDataParam>();
                            data.setParams(params);

                            for (NameValuePair pair : result) {
                                params.add(new HarPostDataParam(pair.getName(), pair.getValue()));
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.info("Unexpected problem when parsing input copy", e);
                    }
                } else {
                    // not URL encoded, so let's grab the body of the POST and capture that
                    data.setText(new String(req.getCopy().toByteArray()));
                }
            }
        }

        //capture request cookies
        javax.servlet.http.Cookie[] cookies = req.getProxyRequest().getCookies();
        for (javax.servlet.http.Cookie cookie : cookies) {
            HarCookie hc = new HarCookie();
            hc.setName(cookie.getName());
            hc.setValue(cookie.getValue());
            entry.getRequest().getCookies().add(hc);
        }

        String contentType = null;

        if (response != null) {
            Header contentTypeHdr = response.getFirstHeader("Content-Type");
            if (contentTypeHdr != null) {
                contentType = contentTypeHdr.getValue();
                entry.getResponse().getContent().setMimeType(contentType);

                ByteArrayOutputStream copy = null;
                boolean enableWorkWithCopy = false;
                if (!isResponseVolatile && captureContent && os != null && os instanceof ClonedOutputStream) {
                    copy = ((ClonedOutputStream) os).getOutput();
                    enableWorkWithCopy = true;
                }
                if (isResponseVolatile && captureContent && bos != null) {
                    enableWorkWithCopy = true;
                    copy = bos;
                }
                if (captureContent && enableWorkWithCopy) {
                    if (entry.getResponse().getBodySize() != 0 && (gzipping || deflating)) {
                        // ok, we need to decompress it before we can put it in the har file
                        try {
                            InputStream temp = null;
                            if (gzipping) {
                                temp = new GZIPInputStream(new ByteArrayInputStream(copy.toByteArray()));
                            } else if (deflating) {
                                //RAW deflate only
                                // WARN : if system is using zlib<=1.1.4 the stream must be append with a dummy byte
                                // that is not required for zlib>1.1.4 (not mentioned on current Inflater javadoc)
                                temp = new InflaterInputStream(new ByteArrayInputStream(copy.toByteArray()), new Inflater(true));
                            }
                            copy = new ByteArrayOutputStream();
                            IOUtils.copy(temp, copy);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (contentType != null && (contentType.startsWith("text/") || contentType.startsWith("application/x-javascript"))
                            || contentType.startsWith("application/javascript") || contentType.startsWith("application/json")
                            || contentType.startsWith("application/xml") || contentType.startsWith("application/xhtml+xml")
                            || contentType.startsWith("application/soap+xml")) {
                        entry.getResponse().getContent().setText(new String(copy.toByteArray()));
                    } else if (captureBinaryContent) {
                        entry.getResponse().getContent().setText(Base64.byteArrayToBase64(copy.toByteArray()));
                    }
                }

                NameValuePair nvp = contentTypeHdr.getElements()[0].getParameterByName("charset");

                if (nvp != null) {
                    charSet = nvp.getValue();
                }
            }
        }

        if (contentType != null) {
            entry.getResponse().getContent().setMimeType(contentType);
        }

        // checking to see if the client is being redirected
        boolean isRedirect = false;

        String location = null;
        if (response != null && statusCode >= 300 && statusCode < 400 && statusCode != 304) {
            isRedirect = true;

            // pulling the header for the redirect
            Header locationHeader = response.getLastHeader("location");
            if (locationHeader != null) {
                location = locationHeader.getValue();
            } else if (followRedirects) {
                throw new RuntimeException("Invalid redirect - missing location header");
            }
        }

        //
        // Response validation - they only work if we're not following redirects
        //

        int expectedStatusCode = req.getExpectedStatusCode();

        // if we didn't mock out the actual response code and the expected code isn't what we saw, we have a problem
        if (expectedStatusCode > -1) {
            if (followRedirects) {
                throw new RuntimeException("Response validation cannot be used while following redirects");
            }
            if (expectedStatusCode != statusCode) {
                if (isRedirect) {
                    throw new RuntimeException("Expected status code of " + expectedStatusCode + " but saw " + statusCode + " redirecting to: "
                            + location);
                } else {
                    throw new RuntimeException("Expected status code of " + expectedStatusCode + " but saw " + statusCode);
                }
            }
        }

        // Location header check:
        if (isRedirect && (req.getExpectedLocation() != null)) {
            if (followRedirects) {
                throw new RuntimeException("Response validation cannot be used while following redirects");
            }

            if (location.compareTo(req.getExpectedLocation()) != 0) {
                throw new RuntimeException("Expected a redirect to  " + req.getExpectedLocation() + " but saw " + location);
            }
        }

        // end of validation logic

        // basic tail recursion for redirect handling
        if (isRedirect && followRedirects) {
            // updating location:
            try {
                URI redirectUri = new URI(location);
                URI newUri = method.getURI().resolve(redirectUri);
                method.setURI(newUri);

                return execute(req, ++depth, isResponseVolatile);
            } catch (URISyntaxException e) {
                LOGGER.warn("Could not parse URL", e);
            }
        }

        return new MitmJavaProxyHttpResponse(statusCode, entry, method, req.getProxyRequest().getURI(), response, contentMatched, errorMessage,
                entry.getResponse().getContent().getText(), contentType, charSet, bos, os, isResponseVolatile);
    }

    private long copyWithStatsDynamic(InputStream is, OutputStream os) throws IOException {
        long bytesCopied = 0;
        byte[] buffer = new byte[BUFFER];
        int length;

        try {
            // read the first byte
            int firstByte = is.read();

            if (firstByte == -1) {
                return 0;
            }

            os.write(firstByte);
            bytesCopied++;

            do {
                length = is.read(buffer, 0, BUFFER);
                if (length != -1) {
                    bytesCopied += length;
                    os.write(buffer, 0, length);
                    os.flush();
                }
            } while (length != -1);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // ok to ignore
            }

            try {
                os.close();
            } catch (IOException e) {
                // ok to ignore
            }
        }

        return bytesCopied;
    }

    public void shutdown() {
        shutdown = true;
        abortActiveRequests();
        rewriteRules.clear();
        credsProvider.clear();
        httpClientConnMgr.shutdown();
        HttpClientInterrupter.release(this);
    }

    public void abortActiveRequests() {
        allowNewRequests.set(true);

        synchronized (activeRequests) {
            for (ActiveRequest activeRequest : activeRequests) {
                activeRequest.abort();
            }
            activeRequests.clear();
        }
    }

    public void setHarPageRef(final String harPageRef) {
        this.harPageRef = harPageRef;
    }

    public void setRequestTimeout(final int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(final boolean followRedirects) {
        this.followRedirects = followRedirects;

    }

    public void rewriteUrl(final String match, final String replace) {
        rewriteRules.add(new RewriteRule(match, replace));
    }

    public void addHeader(final String name, final String value) {
        additionalHeaders.put(name, value);
    }

    public String remappedHost(final String host) {
        return hostNameResolver.remapping(host);
    }

    public List<String> originalHosts(final String host) {
        return hostNameResolver.original(host);
    }

    public Har getHar() {
        return har;
    }

    public void setHar(final Har har) {
        this.har = har;
    }

    public void setCaptureHeaders(final boolean captureHeaders) {
        this.captureHeaders = captureHeaders;
    }

    public void setCaptureContent(final boolean captureContent) {
        this.captureContent = captureContent;
    }

    public void setCaptureBinaryContent(final boolean captureBinaryContent) {
        this.captureBinaryContent = captureBinaryContent;
    }

    public void setHttpProxy(final String httpProxy) {
        String host = httpProxy.split(":")[0];
        Integer port = Integer.parseInt(httpProxy.split(":")[1]);
        HttpHost proxy = new HttpHost(host, port);
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    public void clearDNSCache() {
        hostNameResolver.clearCache();
    }

    public void setDNSCacheTimeout(final int timeout) {
        hostNameResolver.setCacheTimeout(timeout);
    }

    public void prepareForBrowser() {
        // Clear cookies, let the browser handle them
        //httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
        decompress = false;
        setFollowRedirects(false);
    }

    static class PreemptiveAuth implements HttpRequestInterceptor {
        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds != null) {
                        authState.setAuthScheme(authScheme);
                        authState.setCredentials(creds);
                    }
                }
            }
        }
    }

    class ActiveRequest {
        HttpRequestBase request;
        BasicHttpContext ctx;
        Date start;
        boolean wasTimeout;

        ActiveRequest(final HttpRequestBase request, final BasicHttpContext ctx, final Date start) {
            this.request = request;
            this.ctx = ctx;
            this.start = start;
            this.wasTimeout = false;
        }

        void checkTimeout() {
            if (requestTimeout != -1) {
                if (request != null && start != null && new Date(System.currentTimeMillis() - requestTimeout).after(start)) {
                    LOGGER.info("Aborting request to {} after it failed to complete in {} ms", request.getURI().toString(), requestTimeout);
                    wasTimeout = true;
                    abort();
                }
            }
        }

        public void abort() {
            request.abort();

            // try to close the connection? is this necessary? unclear based on preliminary debugging of HttpClient, but
            // it doesn't seem to hurt to try
            HttpConnection conn = (HttpConnection) ctx.getAttribute("http.connection");
            if (conn != null) {
                try {
                    conn.close();
                } catch (IOException e) {
                    // this is fine, we're shutting it down anyway
                }
            }
        }
    }

    private class RewriteRule {
        private final Pattern match;
        private final String replace;

        private RewriteRule(final String match, final String replace) {
            this.match = Pattern.compile(match);
            this.replace = replace;
        }
    }

}
