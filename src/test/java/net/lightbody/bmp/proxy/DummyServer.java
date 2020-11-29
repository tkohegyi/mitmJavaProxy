package net.lightbody.bmp.proxy;

import net.lightbody.bmp.proxy.jetty.http.HttpContext;
import net.lightbody.bmp.proxy.jetty.http.HttpListener;
import net.lightbody.bmp.proxy.jetty.http.SocketListener;
import net.lightbody.bmp.proxy.jetty.http.handler.ResourceHandler;
import net.lightbody.bmp.proxy.jetty.jetty.BmpServer;
import net.lightbody.bmp.proxy.jetty.jetty.servlet.ServletHttpContext;
import net.lightbody.bmp.proxy.jetty.util.InetAddrPort;
import net.lightbody.bmp.proxy.jetty.util.Resource;

import javax.servlet.http.HttpServlet;

public class DummyServer {
    private int port;
    private BmpServer bmpServer = new BmpServer();
    private ResourceHandler handler;

    public DummyServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        HttpListener listener = new SocketListener(new InetAddrPort(port));
        
        bmpServer.addListener(listener);
        addServlet("/jsonrpc/", JsonServlet.class);
        addServlet("/cookie/", SetCookieServlet.class);
        addServlet("/echo/", EchoServlet.class);

        HttpContext context = new HttpContext();
        context.setContextPath("/");
        context.setBaseResource(Resource.newResource("src/test/dummy-server"));
        bmpServer.addContext(context);
        handler = new ResourceHandler();
        context.addHandler(handler);

        bmpServer.start();
    }

    private void addServlet(String path, Class<? extends HttpServlet> servletClass) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ServletHttpContext servletContext = new ServletHttpContext();
        servletContext.setContextPath(path);
        servletContext.addServlet("/", servletClass.getName());
        bmpServer.addContext(servletContext);
    }

    public ResourceHandler getHandler() {
        return handler;
    }

    public void stop() throws InterruptedException {
        bmpServer.stop();
    }

}
