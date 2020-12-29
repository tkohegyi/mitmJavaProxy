// ========================================================================
// $Id: ThreadedServer.java,v 1.41 2005/12/10 00:38:20 gregwilkins Exp $
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
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

package net.lightbody.bmp.proxy.jetty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Threaded socket server. This class listens at a socket and gives the connections received to a pool of Threads
 * <p>
 * The class is abstract and derived classes must provide the handling for the connections.
 * <p>
 * The properties THREADED_SERVER_MIN_THREADS and THREADED_SERVER_MAX_THREADS can be set to control the number of threads created.
 * <p>
 *
 * @author Greg Wilkins
 * @version $Id: ThreadedServer.java,v 1.41 2005/12/10 00:38:20 gregwilkins Exp $
 */
abstract public class ThreadedServer extends ThreadPool {
    private static final Logger log = LoggerFactory.getLogger(ThreadedServer.class);
    
    private InetAddrPort _address = null;
    private int _soTimeOut = -1;

    private transient Acceptor[] _acceptor;
    private transient ServerSocket _listen = null;
    private transient boolean _running = false;
    
    /**
     * Construct.
     */
    public ThreadedServer() {
    }

    /**
     * Construct for specific port.
     */
    public ThreadedServer(int port) {
        setInetAddrPort(new InetAddrPort(port));
    }

    /**
     * Construct for specific address and port.
     */
    public ThreadedServer(InetAddress address, int port) {
        setInetAddrPort(new InetAddrPort(address, port));
    }

    /**
     * Construct for specific address and port.
     */
    public ThreadedServer(String host, int port) throws UnknownHostException {
        setInetAddrPort(new InetAddrPort(host, port));
    }

    /**
     * Construct for specific address and port.
     */
    public ThreadedServer(InetAddrPort address) {
        setInetAddrPort(address);
    }

    /**
     * @return The ServerSocket.
     */
    public ServerSocket getServerSocket() {
        return _listen;
    }

    /**
     * @return IP Address and port in a new Instance of InetAddrPort.
     */
    public InetAddrPort getInetAddrPort() {
        if (_address == null) {
            return null;
        }
        return new InetAddrPort(_address);
    }

    /**
     * Set the server InetAddress and port.
     *
     * @param address The Address to listen on, or 0.0.0.0:port for all interfaces.
     */
    public synchronized void setInetAddrPort(InetAddrPort address) {
        if (_address != null && _address.equals(address)) {
            return;
        }
        if (isStarted()) {
            log.warn(this + " is started");
        }
        _address = address;
    }

    /**
     * @return Host name.
     */
    public String getHost() {
        if (_address == null || _address.getInetAddress() == null) {
            return null;
        }
        return _address.getHost();
    }

    /**
     * @param host
     */
    public synchronized void setHost(String host) throws UnknownHostException {
        if (_address != null && _address.getHost() != null && _address.getHost().equals(host)) {
            return;
        }

        if (isStarted()) {
            log.warn(this + " is started");
        }

        if (_address == null) {
            _address = new InetAddrPort(host, 0);
        } else {
            _address.setHost(host);
        }
    }

    /**
     * @return IP Address.
     */
    public InetAddress getInetAddress() {
        if (_address == null) {
            return null;
        }
        return _address.getInetAddress();
    }

    /**
     * @param addr
     */
    public synchronized void setInetAddress(InetAddress addr) {
        if (_address != null && _address.getInetAddress() != null && _address.getInetAddress().equals(addr)) {
            return;
        }

        if (isStarted()) {
            log.warn(this + " is started");
        }

        if (_address == null) {
            _address = new InetAddrPort(addr, 0);
        } else {
            _address.setInetAddress(addr);
        }
    }

    /**
     * @return port number.
     */
    public int getPort() {
        if (_address == null) {
            return 0;
        }
        return _address.getPort();
    }

    /**
     * @param port
     */
    public synchronized void setPort(int port) {
        if (_address != null && _address.getPort() == port) {
            return;
        }

        if (isStarted()) {
            log.warn(this + " is started");
        }

        if (_address == null) {
            _address = new InetAddrPort(port);
        } else {
            _address.setPort(port);
        }
    }

    /**
     * Handle new connection. This method should be overridden by the derived class to implement the
     * required handling. It is called by a thread created for it and does not need to return until
     * it has finished it's task
     */
    protected void handleConnection(InputStream in, OutputStream out) {
        throw new Error("Either handlerConnection must be overridden");
    }

    /**
     * Handle new connection. If access is required to the actual socket, override this method
     * instead of handleConnection(InputStream in,OutputStream out). The default implementation of
     * this just calls handleConnection(InputStream in,OutputStream out).
     */
    protected void handleConnection(Socket connection) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Handle " + connection);
        }
        InputStream in = connection.getInputStream();
        OutputStream out = connection.getOutputStream();

        handleConnection(in, out);
        out.flush();

        in = null;
        out = null;
        connection.close();
    }

    /**
     * Handle Job. Implementation of ThreadPool.handle(), calls handleConnection.
     *
     * @param job A Connection.
     */
    public void handle(Object job) {
        Socket socket = (Socket) job;
        try {
            boolean _tcpNoDelay = true;
            if (_tcpNoDelay) socket.setTcpNoDelay(true);
            handleConnection(socket);
        } catch (Exception e) {
            log.debug("Connection problem", e);
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                log.debug("Connection problem", e);
            }
        }
    }

    /**
     * New server socket. Creates a new servers socket. May be overriden by derived class to create specialist serversockets (eg SSL).
     *
     * @param address         Address and port
     * @param acceptQueueSize Accept queue size
     * @return The new ServerSocket
     * @throws java.io.IOException
     */
    protected ServerSocket newServerSocket(InetAddrPort address, int acceptQueueSize) throws java.io.IOException {
        if (address == null) {
            return new ServerSocket(0, acceptQueueSize);
        }
        return new ServerSocket(address.getPort(), acceptQueueSize, address.getInetAddress());
    }

    /**
     * Accept socket connection. May be overriden by derived class to create specialist serversockets (eg SSL).
     *
     * @param ignored
     * @param timeout The time to wait for a connection. Normally passed the ThreadPool maxIdleTime.
     * @return Accepted Socket
     * @deprecated use acceptSocket(int timeout)
     */
    protected Socket acceptSocket(ServerSocket ignored, int timeout) {
        return acceptSocket(timeout);
    }

    /**
     * Accept socket connection. May be overriden by derived class to create specialist serversockets (eg SSL).
     *
     * @param timeout      The time to wait for a connection. Normally passed the ThreadPool maxIdleTime.
     * @return Accepted Socket
     */
    protected Socket acceptSocket(int timeout) {
        try {
            Socket s = null;

            if (_listen != null) {
                if (_soTimeOut != timeout) {
                    _soTimeOut = timeout;
                    _listen.setSoTimeout(_soTimeOut);
                }

                s = _listen.accept();
                try {
                    if (getMaxIdleTimeMs() >= 0) s.setSoTimeout(getMaxIdleTimeMs());
                    int _lingerTimeSecs = 30;
                    s.setSoLinger(true, _lingerTimeSecs);
                } catch (Exception e) {
                    //
                }
            }
            return s;
        } catch (java.net.SocketException e) {
            // TODO - this is caught and ignored due strange
            // exception from linux java1.2.v1a
        } catch (InterruptedIOException e) {
            //
        } catch (IOException e) {
            log.warn(LogSupport.EXCEPTION, e);
        }
        return null;
    }
    
    /**
     * Open the server socket. This method can be called to open the server socket in advance of
     * starting the listener. This can be used to test if the port is available.
     *
     * @throws IOException if an error occurs
     */
    public void open() throws IOException {
        if (_listen == null) {
            int _acceptQueueSize = 0;
            _listen = newServerSocket(_address, _acceptQueueSize);

            if (_address == null) {
                _address = new InetAddrPort(_listen.getInetAddress(), _listen.getLocalPort());
            } else {
                if (_address.getInetAddress() == null) {
                    _address.setInetAddress(_listen.getInetAddress());
                }
                if (_address.getPort() == 0) {
                    _address.setPort(_listen.getLocalPort());
                }
            }

            _soTimeOut = getMaxIdleTimeMs();
            if (_soTimeOut >= 0) {
                _listen.setSoTimeout(_soTimeOut);
            }
        }
    }
    
    /**
     * Start the ThreadedServer listening.
     */
    public synchronized void start() throws Exception {
        try {
            if (isStarted()) {
                return;
            }

            open();

            _running = true;
            int _acceptors = 1;
            _acceptor = new Acceptor[_acceptors];
            for (int a = 0; a < _acceptor.length; a++) {
                _acceptor[a] = new Acceptor();
                _acceptor[a].setDaemon(isDaemon());
                _acceptor[a].start();
            }

            super.start();
        } catch (Exception e) {
            log.warn("Failed to start: {}", this);
            throw e;
        }
    }

    public void stop() throws InterruptedException {
        synchronized (this) {
            // Signal that we are stopping
            _running = false;

            // Close the listener socket.
            log.debug("closing {}", _listen);
            try {
                if (_listen != null) {
                    _listen.close();
                }
                _listen = null;
            } catch (IOException e) {
                log.warn(LogSupport.EXCEPTION, e);
            }

            // Do we have an acceptor thread (running or not)
            Thread.yield();
            for (int a = 0; _acceptor != null && a < _acceptor.length; a++) {
                Acceptor acc = _acceptor[a];
                if (acc != null) {
                    acc.interrupt();
                }
            }
            Thread.sleep(100);

            for (int a = 0; _acceptor != null && a < _acceptor.length; a++) {
                Acceptor acc = _acceptor[a];

                if (acc != null) {
                    acc.forceStop();
                    _acceptor[a] = null;
                }
            }
        }

        // Stop the thread pool
        try {
            super.stop();
        } catch (Exception e) {
            log.warn(LogSupport.EXCEPTION, e);
        } finally {
            synchronized (this) {
                _acceptor = null;
            }
        }
    }

    /**
     * Kill a job. This method closes IDLE and socket associated with a job.
     *
     * @param thread
     * @param job
     */
    protected void stopJob(Thread thread, Object job) {
        if (job instanceof Socket) {
            try {
                ((Socket) job).close();
            } catch (Exception e) {
                //
            }
        }
        super.stopJob(thread, job);
    }

    public String toString() {
        if (_address == null) {
            return getName() + "@0.0.0.0:0";
        }
        if (_listen != null) {
            return getName() + "@" + _listen.getInetAddress().getHostAddress() + ":" + _listen.getLocalPort();
        }
        return getName() + "@" + getInetAddrPort();
    }

    private class Acceptor extends Thread {
        
        public void run() {
            ThreadedServer threadedServer = ThreadedServer.this;
            try {
                this.setName("Acceptor " + _listen);
                while (_running) {
                    try {
                        // Accept a socket
                        Socket socket = acceptSocket(_soTimeOut);

                        // Handle the socket
                        if (socket != null) {
                            if (_running) {
                                threadedServer.run(socket);
                            } else {
                                socket.close();
                            }
                        }
                    } catch (Throwable e) {
                        if (_running) {
                            log.warn(LogSupport.EXCEPTION, e);
                        } else {
                            log.debug(LogSupport.EXCEPTION, e);
                        }
                    }
                }
            } finally {
                if (_running) {
                    log.warn("Stopping {}", this.getName());
                } else {
                    log.info("Stopping {}", this.getName());
                }
                synchronized (threadedServer) {
                    if (_acceptor != null) {
                        for (int a = 0; a < _acceptor.length; a++) {
                            if (_acceptor[a] == this) {
                                _acceptor[a] = null;
                            }
                        }
                    }
                    threadedServer.notifyAll();
                }
            }
        }

        void forceStop() {
            if (_listen != null && _address != null) {
                InetAddress addr = _address.getInetAddress();
                try {
                    if (addr == null || addr.toString().startsWith("0.0.0.0")) {
                        addr = InetAddress.getByName("127.0.0.1");
                    }
                    log.debug("Self connect to close listener {}:{}", addr, _address.getPort());
                    Socket socket = new Socket(addr, _address.getPort());
                    Thread.yield();
                    socket.close();
                    Thread.yield();
                } catch (IOException e) {
                    log.debug("problem stopping acceptor {}: ", addr, e);
                }
            }
        }
    }

}
