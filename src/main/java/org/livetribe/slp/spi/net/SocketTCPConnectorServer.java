/*
 * Copyright 2007-2008 the original author or authors
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

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.Message;

/**
 * @version $Revision$ $Date$
 */
public class SocketTCPConnectorServer extends AbstractConnectorServer implements TCPConnectorServer
{
    private final ExecutorService threadPool;
    private final SocketTCPConnector connector;
    private String[] addresses = Defaults.get(ADDRESSES_KEY);
    private int port = Defaults.get(PORT_KEY);
    private ServerSocket[] serverSockets;
    private volatile CountDownLatch startBarrier;
    private volatile CountDownLatch stopBarrier;

    public SocketTCPConnectorServer(ExecutorService threadPool)
    {
        this(threadPool, null);
    }

    public SocketTCPConnectorServer(ExecutorService threadPool, Settings settings)
    {
        this.threadPool = threadPool;
        this.connector = new SocketTCPConnector(settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(ADDRESSES_KEY)) this.addresses = settings.get(ADDRESSES_KEY);
        if (settings.containsKey(PORT_KEY)) this.port = settings.get(PORT_KEY);
    }

    public String[] getAddresses()
    {
        return addresses;
    }

    public void setAddresses(String[] addresses)
    {
        this.addresses = addresses;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    protected void doStart()
    {
        int size = addresses.length;
        startBarrier = new CountDownLatch(size);
        stopBarrier = new CountDownLatch(size);
        serverSockets = new ServerSocket[size];
        Runnable[] acceptors = new Runnable[size];
        for (int i = 0; i < size; ++i)
        {
            InetSocketAddress bindAddress = new InetSocketAddress(addresses[i], port);
            serverSockets[i] = newServerSocket(bindAddress);
            if (logger.isLoggable(Level.FINE)) logger.fine("Bound server socket to " + bindAddress);
            acceptors[i] = new Acceptor(serverSockets[i]);
            accept(acceptors[i]);
        }
        waitForStart();
    }

    private void waitForStart()
    {
        try
        {
            startBarrier.await();
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
            throw new ServiceLocationException("Could not start TCPConnectorServer " + this, SLPError.NETWORK_INIT_FAILED);
        }
    }

    protected ServerSocket newServerSocket(InetSocketAddress address)
    {
        try
        {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(address);
            return serverSocket;
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, SLPError.NETWORK_INIT_FAILED);
        }
    }

    @Override
    public boolean isRunning()
    {
        return super.isRunning() && stopBarrier.getCount() > 0;
    }

    protected void doStop()
    {
        for (ServerSocket serverSocket : serverSockets) closeServerSocket(serverSocket);
        threadPool.shutdownNow();
        clearMessageListeners();
        waitForStop();
    }

    private void waitForStop()
    {
        try
        {
            stopBarrier.await();
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
            throw new ServiceLocationException("Could not stop TCPConnectorServer " + this, SLPError.NETWORK_ERROR);
        }
    }

    private void closeServerSocket(ServerSocket serverSocket)
    {
        try
        {
            if (serverSocket != null) serverSocket.close();
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, SLPError.NETWORK_ERROR);
        }
    }

    private void accept(Runnable acceptor)
    {
        threadPool.execute(acceptor);
    }

    private void handle(Runnable handler) throws RejectedExecutionException
    {
        try
        {
            threadPool.execute(handler);
        }
        catch (RejectedExecutionException x)
        {
            // Connector server stopped just after having accepted a connection
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "TCPConnectorServer " + this + " stopping, rejecting execution of " + handler);
            throw x;
        }
    }

    private class Acceptor implements Runnable
    {
        private final ServerSocket serverSocket;

        public Acceptor(ServerSocket socket)
        {
            this.serverSocket = socket;
        }

        public void run()
        {
            if (logger.isLoggable(Level.FINER))
                logger.finer("ServerSocket acceptor running for " + serverSocket + " in thread " + Thread.currentThread().getName());

            // Signal that this thread has started
            startBarrier.countDown();

            try
            {
                while (true)
                {
                    Socket client = serverSocket.accept();
                    if (logger.isLoggable(Level.FINE)) logger.fine("Client connected from " + client);
                    handle(new Handler(client, (InetSocketAddress)client.getLocalSocketAddress(), (InetSocketAddress)client.getRemoteSocketAddress()));
                }
            }
            catch (SocketException x)
            {
                if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "Closed ServerSocket " + serverSocket);
            }
            catch (RejectedExecutionException x)
            {
                // The connector server is stopping, just exit (see handle(Runnable))
            }
            catch (IOException x)
            {
                if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Unexpected IOException", x);
            }
            finally
            {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("ServerSocket acceptor exiting for " + serverSocket + " in thread " + Thread.currentThread().getName());

                // Signal that the thread has stopped
                stopBarrier.countDown();
            }
        }
    }

    private class Handler implements Runnable
    {
        private final Socket socket;
        private final InetSocketAddress localAddress;
        private final InetSocketAddress remoteAddress;

        public Handler(Socket socket, InetSocketAddress localAddress, InetSocketAddress remoteAddress)
        {
            this.socket = socket;
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }

        public void run()
        {
            if (logger.isLoggable(Level.FINER))
                logger.finer("Socket handler running for " + socket + " in thread " + Thread.currentThread().getName());

            try
            {
                try
                {
                    while (true)
                    {
                        byte[] messageBytes = connector.read(socket);
                        Message message = Message.deserialize(messageBytes);
                        MessageEvent event = new MessageEvent(socket, message, localAddress, remoteAddress);
                        if (logger.isLoggable(Level.FINEST))
                            logger.finest("Notifying message listeners of new message " + message + " from " + remoteAddress);
                        notifyMessageListeners(event);
                    }
                }
                catch (EOFException x)
                {
                    if (logger.isLoggable(Level.FINEST)) logger.finest("Socket closed by client " + socket);
                }
                catch (SocketTimeoutException x)
                {
                    if (logger.isLoggable(Level.FINEST)) logger.finest("Socket closed by server " + socket);
                }
                finally
                {
                    connector.close(socket);
                }
            }
            finally
            {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Socket handler exiting for " + socket + " in thread " + Thread.currentThread().getName());
            }
        }
    }

    public static class Factory implements TCPConnectorServer.Factory
    {
        public TCPConnectorServer newTCPConnectorServer(Settings settings)
        {
            ExecutorService threadPool = Executors.newCachedThreadPool();
            return new SocketTCPConnectorServer(threadPool, settings);
        }
    }
}
