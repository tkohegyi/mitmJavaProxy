// ========================================================================
// $Id: Container.java,v 1.4 2005/08/13 08:49:59 gregwilkins Exp $
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
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
import java.io.Serializable;
import java.util.EventListener;

/**
 * Abstract Container.
 * Provides base handling for LifeCycle and Component events.
 *
 * @author Greg Wilkins (gregw)
 * @version $Id: Container.java,v 1.4 2005/08/13 08:49:59 gregwilkins Exp $
 */
public abstract class Container implements LifeCycle, Serializable {
    private static final Logger log = LoggerFactory.getLogger(Container.class);

    private Object _components;

    private transient boolean _started;
    private transient boolean _starting;
    private transient boolean _stopping;

    /**
     * Start the server.
     * Generate LifeCycleEvents for starting and started either side of a call to doStart
     */
    public synchronized final void start() throws Exception {
        if (_started || _starting) {
            return;
        }

        _starting = true;

        log.debug("Starting {}", this);

        try {
            doStart();
            _started = true;
            log.info("Started {}", this);
        } catch (Throwable e) {
            if (e instanceof Exception) {
                throw (Exception) e;
            }
            throw (Error) e;
        } finally {
            _starting = false;
        }
    }

    /**
     * Do start operations.
     * This abstract method is called by start to perform the actual start operations.
     */
    protected abstract void doStart() throws Exception;
    
    public synchronized boolean isStarted() {
        return _started;
    }

    /**
     * Stop the container.
     * Generate LifeCycleEvents for stopping and stopped either side of a call to doStop
     */
    public synchronized final void stop() throws InterruptedException {
        if (!_started || _stopping) {
            return;
        }
        _stopping = true;

        log.debug("Stopping {}", this);

        try {
            doStop();
            _started = false;
            log.info("Stopped {}", this);
        } catch (Throwable e) {
            if (e instanceof InterruptedException)
                throw (InterruptedException) e;
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            if (e instanceof Error)
                throw (Error) e;
            log.warn(LogSupport.EXCEPTION, e);
        } finally {
            _stopping = false;
        }
    }

    /**
     * Do stop operations.
     * This abstract method is called by stop to perform the actual stop operations.
     */
    protected abstract void doStop() throws Exception;

    protected void addComponent(Object o) {
        if (!LazyList.contains(_components, o)) {
            _components = LazyList.add(_components, o);
            log.debug("add component: {}", o);
        }
    }

    protected void removeComponent(Object o) {
        if (LazyList.contains(_components, o)) {
            _components = LazyList.remove(_components, o);
            log.debug("remove component: {}", o);
        }
    }

    /**
     * Destroy a stopped server. Remove all components and send notifications to all event listeners.
     * The HttpServer must be stopped before it can be destroyed.
     */
    public void destroy() {
        if (isStarted()) {
            throw new IllegalStateException("Started");
        }
        _components = null;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
