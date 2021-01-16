package org.rockhill.mitm.proxy;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.rockhill.mitm.jetty.proxy.ProxyServlet;


public class MitmProxyServlet extends ProxyServlet {
    
    @Override
    protected HttpClient newHttpClient() {
        SslContextFactory sslContextFactory = new SslContextFactory.Client.Client();
        sslContextFactory.setTrustAll(true);
        return new HttpClient(sslContextFactory);
    }

}
