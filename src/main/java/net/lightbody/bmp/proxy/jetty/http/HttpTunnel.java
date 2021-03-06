// ========================================================================
// $Id: HttpTunnel.java,v 1.11 2005/10/05 11:14:37 gregwilkins Exp $
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package net.lightbody.bmp.proxy.jetty.http;

import net.lightbody.bmp.proxy.jetty.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * HTTP Tunnel.
 * A HTTP Tunnel can be used to take over a HTTP connection in order to tunnel another protocol over it.
 * The prime example is the CONNECT method handled by the ProxyHandler to setup a SSL tunnel between the client and
 * the real server.
 *
 * @author Greg Wilkins (gregw)
 * @version $Revision: 1.11 $
 * @see HttpConnection
 */
public class HttpTunnel {
    private final Logger log = LoggerFactory.getLogger(HttpTunnel.class);

    private Thread _thread;
    private int _timeoutMs;
    private Socket _socket;
    private InputStream _sIn;
    private OutputStream _sOut;
    private InputStream _in;
    private OutputStream _out;

    /**
     * Constructor.
     */
    protected HttpTunnel() {
    }

    /**
     * Constructor.
     *
     * @param socket The tunnel socket.
     * @throws IOException
     */
    public HttpTunnel(Socket socket) throws IOException {
        _socket = socket;
        _sIn = _socket.getInputStream();
        _sOut = socket.getOutputStream();
        _timeoutMs = 30000;
    }

    /**
     * Copy Stream in to Stream for byteCount bytes or until EOF or exception.
     *
     * @return Copied bytes count or -1 if no bytes were read *and* EOF was reached
     */
    public static int copyBytes(InputStream in, OutputStream out, long byteCount) throws IOException {
        byte[] buffer = new byte[IO.bufferSize];
        int len = IO.bufferSize;
        int totalCount = 0;

        if (byteCount >= 0) {
            totalCount = (int) byteCount;
            while (byteCount > 0) {
                try {
                    if (byteCount < IO.bufferSize) {
                        len = in.read(buffer, 0, (int) byteCount);
                    } else {
                        len = in.read(buffer, 0, IO.bufferSize);
                    }
                    if (len == -1 && totalCount == byteCount) {
                        totalCount = (int) byteCount - 1;
                    }
                } catch (InterruptedIOException e) {
                    if (totalCount == byteCount) {
                        throw e;
                    }
                    len = 0;
                }

                if (len <= 0) {
                    break;
                }
                byteCount -= len;
                out.write(buffer, 0, len);
            }
            totalCount -= byteCount;
        } else {
            while (len > 0) {
                try {
                    len = in.read(buffer, 0, IO.bufferSize);
                    if (len == -1 && totalCount == 0) {
                        totalCount = -1;
                    }
                } catch (InterruptedIOException e) {
                    if (totalCount == 0) {
                        throw e;
                    }
                    len = 0;
                }
                if (len > 0) {
                    out.write(buffer, 0, len);
                    totalCount += len;
                }
            }
        }
        return totalCount;
    }

    /**
     * handle method.
     * This method is called by the HttpConnection.handleNext() method if this HttpTunnel has been set on that connection.
     * The default implementation of this method copies between the HTTP socket and the socket passed in the constructor.
     *
     * @param in
     * @param out
     */
    public void handle(InputStream in, OutputStream out) {
        Copy copy = new Copy();
        _in = in;
        _out = out;
        try {
            _thread = Thread.currentThread();
            copy.start();

            copydata(_sIn, _out);
        } catch (Exception e) {
            log.debug("Ex at Tunnel copydata", e);
        } finally {
            try {
                _in.close();
                if (_socket != null) {
                    _socket.shutdownOutput();
                    _socket.close();
                } else {
                    _sIn.close();
                    _sOut.close();
                }
            } catch (Exception e) {
                //
            }
            copy.interrupt();
        }
    }

    private void copydata(InputStream in, OutputStream out) throws java.io.IOException {
        long timestamp = 0;
        long byteCount = 0;
        while (true) {
            try {
                byteCount = copyBytes(in, out, -1);
                timestamp = 0;
                if (byteCount == -1) {
                    return;
                }
            } catch (InterruptedIOException e) {
                if (timestamp == 0) {
                    timestamp = System.currentTimeMillis();
                } else {
                    if (_timeoutMs > 0 && (System.currentTimeMillis() - timestamp) > _timeoutMs) {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * @return Returns the socket.
     */
    public Socket getSocket() {
        return _socket;
    }

    /**
     * @return Returns the timeoutMs.
     */
    public int getTimeoutMs() {
        return _timeoutMs;
    }

    /**
     * @param timeoutMs The timeoutMs to set.
     */
    public void setTimeoutMs(int timeoutMs) {
        _timeoutMs = timeoutMs;
    }

    /**
     * Copy thread.
     * Helper thread to copy from the HTTP input to the sockets output
     */
    private class Copy extends Thread {
        public void run() {
            try {
                copydata(_in, _sOut);
            } catch (Exception e) {
                //
            } finally {
                try {
                    _out.close();
                    if (_socket != null) {
                        _socket.shutdownInput();
                        _socket.close();
                    } else {
                        _sOut.close();
                        _sIn.close();
                    }
                } catch (Exception e) {
                    //
                }
                _thread.interrupt();
            }
        }
    }

}
