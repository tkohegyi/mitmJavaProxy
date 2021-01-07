package net.lightbody.bmp.proxy.http;

import org.apache.http.HttpHost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpInetSocketAddress;
import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Date;

public class SimulatedSocketFactory implements ConnectionSocketFactory {
    protected static final Logger logger = LoggerFactory.getLogger(SimulatedSocketFactory.class);
    /**
     * Prevent unnecessary class inspection at runtime.
     */
    private static Method getHostMethod;

    static {
        try {
            getHostMethod = InetSocketAddress.class.getDeclaredMethod("getHostString", new Class[]{});
            if (!Modifier.isPublic(getHostMethod.getModifiers())) {
                getHostMethod = null;
            }
        } catch (Exception e) {
            // ok to ignore, try the fall back
        }

        if (getHostMethod == null) {
            try {
                getHostMethod = InetSocketAddress.class.getDeclaredMethod("getHostName", new Class[]{});
                logger.warn("Using InetSocketAddress.getHostName() rather than InetSocketAddress.getHostString(). Consider upgrading to Java 7 for faster performance!");
            } catch (NoSuchMethodException e) {
                String msg = "Something is wrong inside SimulatedSocketFactory and I don't know why!";
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        getHostMethod.setAccessible(true);
    }

    private HostNameResolver hostNameResolver;
    private int requestTimeout;

    public SimulatedSocketFactory(HostNameResolver hostNameResolver, int requestTimeout) {
        super();
        assert hostNameResolver != null;
        this.hostNameResolver = hostNameResolver;
        this.requestTimeout = requestTimeout;
    }

    private void configureSocket(Socket socket) {
        // Configure the socket to be Load Test Friendly!
        // If we don't set these, we can easily use up too many sockets, even when we're cleaning/closing the sockets
        // responsibly. The reason is that they will stick around in TIME_WAIT for some time (ie: 1-4 minutes) and once
        // they get to 64K (on Linux) or 16K (on Mac) we can't make any more requests. While those limits can be raised
        // with a configuration setting in the OS, we really don't need to change things globally. We just need to make
        // sure that when we close a socket it gets ditched right away and doesn't stick around in TIME_WAIT.
        //
        // This problem is most easily noticable/problematic for load tests that use a single transaction to issue
        // one HTTP request and then end the transaction, thereby shutting down the HTTP socket. This can easily create
        // 64K+ sockets in TIME_WAIT state, preventing any other requests from going out and producing a false-negative
        // "connection refused" error message.
        //
        // For further reading, check out HttpClient's FAQ on this subject:
        // http://wiki.apache.org/HttpComponents/FrequentlyAskedConnectionManagementQuestions
        try {
            socket.setReuseAddress(true);
            socket.setSoLinger(true, 0);
        } catch (Exception e) {
            //this is fine not to do anything here
        }
    }

    /**
     * A minor optimization to prevent possible host resolution when inspecting a InetSocketAddress for a hostname....
     *
     * @param remoteAddress
     * @return
     * @throws IOException
     */
    private String resolveHostName(InetSocketAddress remoteAddress) {
        String hostString = null;
        try {
            hostString = (String) getHostMethod.invoke(remoteAddress, new Object[]{});
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Expecting InetSocketAddress to have a package scoped \"getHostString\" method which returns a String and takes no input");
        }
        return hostString;
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        //Ignoring httpParams
        //apparently it's only useful to pass through a SOCKS server
        //see: http://svn.apache.org/repos/asf/httpcomponents/httpclient/trunk/httpclient/src/examples/org/apache/http/examples/client/ClientExecuteSOCKS.java

        //creating an anonymous class deriving from socket
        Socket newSocket = new Socket() {
            @Override
            public void connect(SocketAddress endpoint) throws IOException {
                super.connect(endpoint);
            }

            @Override
            public void connect(SocketAddress endpoint, int timeout) throws IOException {
                super.connect(endpoint, timeout);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return super.getInputStream();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return super.getOutputStream();
            }
        };
        configureSocket(newSocket);
        return newSocket;
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
        if (remoteAddress == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }

        if (context == null) {
            throw new IllegalArgumentException("Context may not be null.");
        }

        if (sock == null) {
            sock = createSocket(null);
        }

        if ((localAddress != null)) {
            sock.bind(localAddress);
        }

        String hostName;
        if (remoteAddress instanceof HttpInetSocketAddress) {
            hostName = ((HttpInetSocketAddress) remoteAddress).getHttpHost().getHostName();
        } else {
            hostName = resolveHostName(remoteAddress);
        }

        InetSocketAddress remoteAddr = remoteAddress;
        if (this.hostNameResolver != null) {
            remoteAddr = new InetSocketAddress(this.hostNameResolver.resolve(hostName), remoteAddress.getPort());
        }

        try {
            sock.connect(remoteAddr, connectTimeout);
        } catch (SocketTimeoutException ex) {
            throw new ConnectTimeoutException("Connect to " + remoteAddress + " timed out");
        }

        return sock;
    }
}