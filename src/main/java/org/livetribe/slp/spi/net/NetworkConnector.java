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
package org.livetribe.slp.spi.net;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.SynchronousQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.livetribe.slp.api.Configuration;

/**
 * @version $Rev$ $Date$
 */
public abstract class NetworkConnector
{
    protected final Logger logger = Logger.getLogger(getClass().getName());

    private Configuration configuration;
    private final List listeners = new ArrayList();
    private final Lock listenersLock = new ReentrantLock();
    private InetAddress[] inetAddresses;
    private volatile boolean running;
    private ThreadPoolExecutor connectionPool;
    private ExecutorService acceptorPool;

    public void setConfiguration(Configuration configuration) throws IOException
    {
        this.configuration = configuration;
        String[] interfaces = configuration.getInterfaceAddresses();
        if (interfaces != null)
        {
            InetAddress[] interfaceAddresses = new InetAddress[interfaces.length];
            for (int i = 0; i < interfaces.length; ++i) interfaceAddresses[i] = InetAddress.getByName(interfaces[i]);
        }
    }

    protected Configuration getConfiguration()
    {
        return configuration;
    }

    public void setAcceptorPool(ExecutorService acceptorPool)
    {
        this.acceptorPool = acceptorPool;
    }

    public void setConnectionPool(ThreadPoolExecutor threadPool)
    {
        this.connectionPool = threadPool;
    }

    public void addMessageListener(MessageListener listener)
    {
        listenersLock.lock();
        try
        {
            listeners.add(listener);
        }
        finally
        {
            listenersLock.unlock();
        }
    }

    public void removeMessageListener(MessageListener listener)
    {
        listenersLock.lock();
        try
        {
            listeners.remove(listener);
        }
        finally
        {
            listenersLock.unlock();
        }
    }

    protected void clearMessageListeners()
    {
        listenersLock.lock();
        try
        {
            listeners.clear();
        }
        finally
        {
            listenersLock.unlock();
        }
    }

    public InetAddress[] getInetAddresses()
    {
        return inetAddresses;
    }

    public void setInetAddresses(InetAddress[] interfaceAddress)
    {
        this.inetAddresses = interfaceAddress;
    }

    public void start() throws IOException
    {
        if (isRunning())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Connector " + this + " is already started");
            return;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Connector " + this + " starting...");

        doStart();

        running = true;

        if (logger.isLoggable(Level.FINE)) logger.fine("Connector " + this + " started successfully");
    }

    protected void doStart() throws IOException
    {
        Runnable[] acceptors = createAcceptors();
        if (acceptors != null && acceptors.length > 0)
        {
            int size = acceptors.length;
            if (acceptorPool == null) acceptorPool = new ThreadPoolExecutor(size, Integer.MAX_VALUE, 5, TimeUnit.SECONDS, new SynchronousQueue());
            if (connectionPool == null) connectionPool = new ThreadPoolExecutor(size, Integer.MAX_VALUE, 5, TimeUnit.SECONDS, new LinkedBlockingQueue());
            for (int i = 0; i < acceptors.length; ++i) accept(acceptors[i]);
        }
    }

    public boolean isRunning()
    {
        return running;
    }

    public void stop() throws IOException
    {
        if (!isRunning())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Connector " + this + " is already stopped");
            return;
        }

        if (logger.isLoggable(Level.FINER)) logger.finer("Connector " + this + " stopping...");

        running = false;

        doStop();

        clearMessageListeners();

        if (logger.isLoggable(Level.FINE)) logger.fine("Connector " + this + " stopped successfully");
    }

    protected void doStop() throws IOException
    {
        if (acceptorPool != null)
        {
            acceptorPool.shutdown();
            acceptorPool = null;
        }
        if (connectionPool != null)
        {
            connectionPool.shutdown();
            connectionPool = null;
        }
        destroyAcceptors();
    }

    protected abstract Runnable[] createAcceptors() throws IOException;

    protected abstract void destroyAcceptors() throws IOException;

    protected void accept(Runnable executor)
    {
        try
        {
            if (acceptorPool == null)
            {
                if (isRunning()) throw new AssertionError("BUG: acceptor pool has been reset, but this connector is still running");
            }
            else
            {
                acceptorPool.execute(executor);
            }
        }
        catch (RejectedExecutionException x)
        {
            if (isRunning()) throw x;
            if (logger.isLoggable(Level.FINEST)) logger.finest("Connector has been stopped, rejected execution of " + executor);
        }
    }

    protected void handle(Runnable executor)
    {
        try
        {
            if (connectionPool == null)
            {
                if (isRunning()) throw new AssertionError("BUG: connection pool has been reset, but this connector is still running");
            }
            else
            {
                connectionPool.execute(executor);
            }
        }
        catch (RejectedExecutionException x)
        {
            if (isRunning()) throw x;
            if (logger.isLoggable(Level.FINEST)) logger.finest("Connector has been stopped, rejected execution of " + executor);
        }
    }

    protected void notifyMessageListeners(MessageEvent event)
    {
        List copy = new ArrayList();
        listenersLock.lock();
        try
        {
            copy.addAll(listeners);
        }
        finally
        {
            listenersLock.unlock();
        }

        if (copy.isEmpty())
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("No MessageListeners to notify of event " + event);
        }
        for (int i = 0; i < copy.size(); ++i)
        {
            MessageListener listener = (MessageListener)copy.get(i);
            try
            {
                if (logger.isLoggable(Level.FINEST)) logger.finest("Notifying MessageListener " + listener + " of event " + event);
                listener.handle(event);
                if (logger.isLoggable(Level.FINEST)) logger.finest("Notified MessageListener " + listener + " of event " + event);
            }
            catch (RuntimeException x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "MessageListener threw RuntimeException, ignored", x);
            }
        }
    }
}
