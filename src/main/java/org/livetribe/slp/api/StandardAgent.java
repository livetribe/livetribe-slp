/*
 * Copyright 2006 the original author or authors
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
package org.livetribe.slp.api;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.livetribe.slp.Scopes;

/**
 * @version $Rev$ $Date$
 */
public abstract class StandardAgent implements Agent
{
//    protected static final String DEFAULT_SCOPE = "DEFAULT";
//    protected static final String[] DEFAULT_SCOPES = new String[]{DEFAULT_SCOPE};

    protected final Logger logger = Logger.getLogger(getClass().getName());

    private Configuration configuration;
    private volatile boolean running;
    private Scopes scopes = Scopes.DEFAULT;

    public void setConfiguration(Configuration configuration) throws IOException
    {
        this.configuration = configuration;
    }

    protected Configuration getConfiguration()
    {
        return configuration;
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    public void setScopes(Scopes scopes)
    {
        if (scopes == null) throw new NullPointerException();
        this.scopes = scopes;
    }

    public boolean isRunning()
    {
        return running;
    }

    public void start() throws Exception
    {
        if (isRunning())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Agent " + this + " is already started");
            return;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Agent " + this + " starting...");

        doStart();

        running = true;

        if (logger.isLoggable(Level.FINE)) logger.fine("Agent " + this + " started successfully");
    }

    protected abstract void doStart() throws Exception;

    public void stop() throws Exception
    {
        if (!isRunning())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Agent " + this + " is already stopped");
            return;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Agent " + this + " stopping...");

        running = false;

        doStop();

        if (logger.isLoggable(Level.FINE)) logger.fine("Agent " + this + " stopped successfully");
    }

    protected abstract void doStop() throws Exception;
}
