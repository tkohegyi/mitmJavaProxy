package net.lightbody.bmp.proxy.http;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;

import java.io.IOException;
import java.util.Date;

public class SimulatedRequestExecutor extends HttpRequestExecutor {
    @Override
    protected HttpResponse doSendRequest(final HttpRequest request, final HttpClientConnection conn, final HttpContext context)
            throws IOException, HttpException {
        Date start = new Date();
        HttpResponse response = super.doSendRequest(request, conn, context);
        RequestInfo.get().send(start, new Date());
        return response;
    }

    @Override
    protected HttpResponse doReceiveResponse(final HttpRequest request, final HttpClientConnection conn, final HttpContext context)
            throws HttpException, IOException {
        Date start = new Date();
        HttpResponse response = super.doReceiveResponse(request, conn, context);
        RequestInfo.get().wait(start, new Date());
        return response;
    }

}
