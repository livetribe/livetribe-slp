/*
 * Copyright 2006-2008 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.spi;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @version $Rev$ $Date$
 */
public abstract class AbstractServer implements Server
{
    protected final Logger logger = Logger.getLogger(getClass().getName());

    private final AtomicBoolean starting = new AtomicBoolean(false);
    private volatile boolean running;

    public boolean isRunning()
    {
        return running;
    }

    public void start()
    {
        if (!starting.compareAndSet(false, true) || isRunning())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Server " + this + " is already running");
            return;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Server " + this + " starting...");

        doStart();

        running = true;

        if (logger.isLoggable(Level.FINE)) logger.fine("Server " + this + " started successfully");
    }

    protected abstract void doStart();

    public void stop()
    {
        if (!starting.compareAndSet(true, false) && !isRunning())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Server " + this + " is already stopped");
            return;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Server " + this + " stopping...");

        doStop();

        running = false;

        if (logger.isLoggable(Level.FINE)) logger.fine("Server " + this + " stopped successfully");
    }

    protected abstract void doStop();
}
