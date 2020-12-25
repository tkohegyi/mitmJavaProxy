package net.lightbody.bmp.proxy.http;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Our own implementation of the AllowAllHostnameVerifier class.  The one that ships with HttpClient doesn't actually
 * allow all host names.  In particular, it fails to work when an upstream proxy is present.
 * <p>
 * http://javaskeleton.blogspot.com/2010/07/avoiding-peer-not-authenticated-with.html was a very helpful resource in
 * tracking down SSL problems with HttpClient.
 */
public class AllowAllHostnameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
}
