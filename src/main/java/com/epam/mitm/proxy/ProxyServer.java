package com.epam.mitm.proxy;
/*==========================================================================
Copyright since 2013, EPAM Systems
===========================================================================*/

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarNameVersion;
import net.lightbody.bmp.core.har.HarPage;
import net.lightbody.bmp.core.util.ThreadUtils;
import net.lightbody.bmp.proxy.BrowserMobProxyHandler;
import net.lightbody.bmp.proxy.http.BrowserMobHttpClient;
import net.lightbody.bmp.proxy.http.RequestInterceptor;
import net.lightbody.bmp.proxy.http.ResponseInterceptor;
import net.lightbody.bmp.proxy.jetty.http.HttpContext;
import net.lightbody.bmp.proxy.jetty.http.HttpListener;
import net.lightbody.bmp.proxy.jetty.http.SocketListener;
import net.lightbody.bmp.proxy.jetty.jetty.BmpServer;
import net.lightbody.bmp.proxy.jetty.util.InetAddrPort;
import org.java_bandwidthlimiter.BandwidthLimiter;
import org.java_bandwidthlimiter.StreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyServer {
    protected static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    private static final HarNameVersion CREATOR = new HarNameVersion("Wilma Proxy", "2.2.1");
    public static int PROXY_TIMEOUT = 240000; //4 minutes, by default will be set during ProxyServer.start()
    private static Boolean responseVolatile = Boolean.FALSE;  //general default approach is that the response is not volatile
    private static Boolean shouldKeepSslConnectionAlive = Boolean.FALSE; //set it to true if such (e.g. .net) clients we have
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private BmpServer bmpServer;
    private int port = -1;
    private BrowserMobHttpClient client;
    private StreamManager streamManager;
    private HarPage currentPage;
    private BrowserMobProxyHandler handler;
    private int pageCount = 1;

    public ProxyServer() {
    }

    public ProxyServer(final int port) {
        this.port = port;
    }

    public static Boolean getResponseVolatile() {
        return responseVolatile;
    }

    public static void setResponseVolatile(Boolean responseVolatile) {
        ProxyServer.responseVolatile = responseVolatile;
    }

    public static Boolean getShouldKeepSslConnectionAlive() {
        return shouldKeepSslConnectionAlive;
    }

    public static void setShouldKeepSslConnectionAlive(Boolean shouldKeepSslConnectionAlive) {
        ProxyServer.shouldKeepSslConnectionAlive = shouldKeepSslConnectionAlive;
    }

    public void start(final int requestTimeOut) throws Exception {
        if (port == -1) {
            throw new IllegalStateException("Must set port before starting");
        }

        PROXY_TIMEOUT = requestTimeOut;
        //create a stream manager that will be capped to 100 Megabits
        //remember that by default it is disabled!
        streamManager = new StreamManager(100 * BandwidthLimiter.OneMbps);

        bmpServer = new BmpServer();
        HttpListener listener = new SocketListener(new InetAddrPort(getPort()));
        bmpServer.addListener(listener);
        HttpContext context = new HttpContext();
        context.setContextPath("/");
        bmpServer.addContext(context);

        handler = new BrowserMobProxyHandler();
        handler.setJettyServer(bmpServer);
        handler.setShutdownLock(new Object());
        client = new BrowserMobHttpClient(streamManager, requestCounter, requestTimeOut);
        client.prepareForBrowser();
        handler.setHttpClient(client);

        context.addHandler(handler);

        bmpServer.start();

        setPort(listener.getPort());
    }

    public void cleanup() {
        handler.cleanup();
    }

    public void stop() throws Exception {
        cleanup();
        client.shutdown();
        bmpServer.stop();
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    /* public void setRetryCount(final int count) {
        client.setRetryCount(count);
    } */

    public Har getHar() {
        // Wait up to 5 seconds for all active requests to cease before returning the HAR.
        // This helps with race conditions but won't cause deadlocks should a request hang
        // or error out in an unexpected way (which of course would be a bug!)
        boolean success = ThreadUtils.waitFor(new ThreadUtils.WaitCondition() {
            @Override
            public boolean checkCondition(final long elapsedTimeInMs) {
                return requestCounter.get() == 0;
            }
        }, TimeUnit.SECONDS, 5);

        if (!success) {
            logger.warn("Waited 5 seconds for requests to cease before returning HAR; giving up!");
        }

        return client.getHar();
    }

    public Har newHar(final String initialPageRef) {
        pageCount = 1;

        Har oldHar = getHar();

        Har har = new Har(new HarLog(CREATOR));
        client.setHar(har);
        newPage(initialPageRef);

        return oldHar;
    }

    public void newPage(String pageRef) {
        if (pageRef == null) {
            pageRef = "Page " + pageCount;
        }

        client.setHarPageRef(pageRef);
        currentPage = new HarPage(pageRef);
        client.getHar().getLog().addPage(currentPage);

        pageCount++;
    }

    public void endPage() {
        if (currentPage == null) {
            return;
        }

        currentPage.getPageTimings().setOnLoad(new Date().getTime() - currentPage.getStartedDateTime().getTime());
        client.setHarPageRef(null);
        currentPage = null;
    }

    public void remapHost(final String source, final String target) {
        client.remapHost(source, target);
    }

    public void addRequestInterceptor(final RequestInterceptor interceptor) {
        client.addRequestInterceptor(interceptor);
    }

    public void addResponseInterceptor(final ResponseInterceptor interceptor) {
        client.addResponseInterceptor(interceptor);
    }

    public StreamManager getStreamManager() {
        return streamManager;
    }

    public void setRequestTimeout(final int requestTimeout) {
        client.setRequestTimeout(requestTimeout);
    }

    public void setSocketOperationTimeout(final int readTimeout) {
        client.setSocketOperationTimeout(readTimeout);
    }

    public void setConnectionTimeout(final int connectionTimeout) {
        client.setConnectionTimeout(connectionTimeout);
    }

    public void rewriteUrl(final String match, final String replace) {
        client.rewriteUrl(match, replace);
    }

    public void blacklistRequests(final String pattern, final int responseCode) {
        client.blacklistRequests(pattern, responseCode);
    }

    public void whitelistRequests(final String[] patterns, final int responseCode) {
        client.whitelistRequests(patterns, responseCode);
    }

    public void addHeader(final String name, final String value) {
        client.addHeader(name, value);
    }

    public void setCaptureHeaders(final boolean captureHeaders) {
        client.setCaptureHeaders(captureHeaders);
    }

    public void setCaptureContent(final boolean captureContent) {
        client.setCaptureContent(captureContent);
    }

    public void setCaptureBinaryContent(final boolean captureBinaryContent) {
        client.setCaptureBinaryContent(captureBinaryContent);
    }

    public void clearDNSCache() {
        client.clearDNSCache();
    }

    public void setDNSCacheTimeout(final int timeout) {
        client.setDNSCacheTimeout(timeout);
    }

    public void waitForNetworkTrafficToStop(final long quietPeriodInMs, final long timeoutInMs) {
        boolean result = ThreadUtils.waitFor(new ThreadUtils.WaitCondition() {
            @Override
            public boolean checkCondition(final long elapsedTimeInMs) {
                Date lastCompleted = null;
                Har har = client.getHar();
                if (har == null || har.getLog() == null) {
                    return true;
                }

                for (HarEntry entry : har.getLog().getEntries()) {
                    // if there is an active request, just stop looking
                    if (entry.getResponse().getStatus() < 0) {
                        return false;
                    }

                    Date end = new Date(entry.getStartedDateTime().getTime() + entry.getTime());
                    if (lastCompleted == null) {
                        lastCompleted = end;
                    } else if (end.after(lastCompleted)) {
                        lastCompleted = end;
                    }
                }

                return lastCompleted != null && System.currentTimeMillis() - lastCompleted.getTime() >= quietPeriodInMs;
            }
        }, TimeUnit.MILLISECONDS, timeoutInMs);

        if (!result) {
            throw new RuntimeException("Timed out after " + timeoutInMs + " ms while waiting for network traffic to stop");
        }
    }

    public void setOptions(final Map<String, String> options) {
        if (options.containsKey("httpProxy")) {
            client.setHttpProxy(options.get("httpProxy"));
        }
    }

}
