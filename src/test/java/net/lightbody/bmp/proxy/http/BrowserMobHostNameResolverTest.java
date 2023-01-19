package net.lightbody.bmp.proxy.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Address;
import org.xbill.DNS.Cache;
import org.xbill.DNS.DClass;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class BrowserMobHostNameResolverTest {

    @Test
    public void testLibraryUsageLocalhost() {
        InetAddress addr = null;
        try {
            addr = Address.getByName("localhost");
            Assertions.assertNotNull(addr);  //this is expected in java >1.8
        } catch (UnknownHostException e) {
            Assertions.assertNull(addr);  //this is expected in java <= 1.8
        }
    }

    @Test
    public void testLibraryUsageDnsJava() throws UnknownHostException {
        InetAddress addr = Address.getByName("www.dnsjava.org");
        Assertions.assertNotNull(addr);
    }

    @Test
    public void testLibraryUsageGoogle() throws UnknownHostException {
        InetAddress addr = Address.getByName("google.com");
        Assertions.assertNotNull(addr);
    }

    @Test
    public void testMitmProxyNameResolverLocalhost() throws IOException {
        BrowserMobHostNameResolver browserMobHostNameResolver = new BrowserMobHostNameResolver(new Cache(DClass.ANY));
        InetAddress addr = browserMobHostNameResolver.resolve("localhost");
        Assertions.assertNotNull(addr);
        Assertions.assertEquals("127.0.0.1", addr.getHostName());
    }

    @Test
    public void testMitmProxyNameResolverDnsJava() throws IOException {
        BrowserMobHostNameResolver browserMobHostNameResolver = new BrowserMobHostNameResolver(new Cache(DClass.ANY));
        InetAddress addr = browserMobHostNameResolver.resolve("www.dnsjava.org");
        Assertions.assertNotNull(addr);
    }

    @Test
    public void testMitmProxyNameResolverGoogle() throws IOException {
        BrowserMobHostNameResolver browserMobHostNameResolver = new BrowserMobHostNameResolver(new Cache(DClass.ANY));
        InetAddress addr = browserMobHostNameResolver.resolve("google.com");
        Assertions.assertNotNull(addr);
    }

}
