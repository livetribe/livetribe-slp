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

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 */
public abstract class AbstractServer implements Server
{
    private enum Status
    {
        READY, STARTING, RUNNING, STOPPING, STOPPED
    }

    protected final Logger logger = Logger.getLogger(getClass().getName());
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.READY);

    public boolean isRunning()
    {
        return status.get() == Status.RUNNING;
    }

    public boolean start()
    {
        if (!status.compareAndSet(Status.READY, Status.STARTING))
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Server " + this + " is not ready");
            return false;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Server " + this + " starting...");

        doStart();

        status.set(Status.RUNNING);
        if (logger.isLoggable(Level.FINE)) logger.fine("Server " + this + " started successfully");

        return true;
    }

    protected abstract void doStart();

    public boolean stop()
    {
        if (!status.compareAndSet(Status.RUNNING, Status.STOPPING))
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Server " + this + " is not running");
            return false;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Server " + this + " stopping...");

        doStop();

        status.set(Status.STOPPED);
        if (logger.isLoggable(Level.FINE)) logger.fine("Server " + this + " stopped successfully");

        return true;
    }

    protected abstract void doStop();
}
