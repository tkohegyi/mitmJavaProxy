package net.lightbody.bmp.proxy;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarCookie;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.util.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class CookieTest extends DummyServerTest {

    private static final String ECHO_URL = BASE_URL + "/echo/";
    private static final String COOKIE_URL = BASE_URL + "/cookie/";

    @Test
    public void testNoDoubleCookies() throws IOException {
        proxy.setCaptureContent(true);
        proxy.newHar("Test");

        // set the cookie on the server side
        IOUtils.readFully(client.execute(new HttpGet(COOKIE_URL)).getEntity().getContent());

        String body = IOUtils.readFully(client.execute(new HttpGet(ECHO_URL)).getEntity().getContent());
        int first = body.indexOf("foo=bar");
        int last = body.lastIndexOf("foo=bar");
        Assertions.assertTrue(first != -1, "foo=bar cookie not found");
        Assertions.assertEquals(first, last);
    }

    @Test
    public void testCookiesAreCapturedWhenRequested() throws IOException {
        proxy.setCaptureContent(true);
        proxy.newHar("Test");

        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("foo", "bar");
        cookie.setDomain("127.0.0.1");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
        HttpHost proxyHost = new HttpHost("127.0.0.1", 8081, "http");
        HttpClient localClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).setProxy(proxyHost).build();
        // set the cookie on the server side
        String body = IOUtils.readFully(localClient.execute(new HttpGet(ECHO_URL)).getEntity().getContent());

        Har har = proxy.getHar();
        HarEntry entry = har.getLog().getEntries().get(0);
        HarCookie harCookie = entry.getRequest().getCookies().get(0);
        Assertions.assertEquals("foo", harCookie.getName());
        Assertions.assertEquals("bar", harCookie.getValue());
    }

}
