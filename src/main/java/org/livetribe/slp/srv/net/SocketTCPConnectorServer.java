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
package org.livetribe.slp.srv.net;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import org.livetribe.slp.ServiceLocationException;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.msg.Message;

/**
 * @version $Revision$ $Date$
 */
public class SocketTCPConnectorServer extends AbstractConnectorServer implements TCPConnectorServer
{
    private final SocketTCPConnector connector;
    private ExecutorService threadPool = Defaults.get(EXECUTOR_SERVICE_KEY);
    private int tcpReadTimeout = Defaults.get(TCP_READ_TIMEOUT_KEY);
    private String[] addresses = Defaults.get(ADDRESSES_KEY);
    private int port = Defaults.get(PORT_KEY);
    private ServerSocket[] serverSockets;

    public SocketTCPConnectorServer(Settings settings)
    {
        this.connector = new SocketTCPConnector(settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(EXECUTOR_SERVICE_KEY)) setThreadPool(settings.get(EXECUTOR_SERVICE_KEY));
        if (settings.containsKey(TCP_READ_TIMEOUT_KEY)) setTcpReadTimeout(settings.get(TCP_READ_TIMEOUT_KEY));
        if (settings.containsKey(ADDRESSES_KEY)) setAddresses(settings.get(ADDRESSES_KEY));
        if (settings.containsKey(PORT_KEY)) setPort(settings.get(PORT_KEY));
    }

    public void setThreadPool(ExecutorService threadPool)
    {
        this.threadPool = threadPool;
    }

    public void setTcpReadTimeout(int tcpReadTimeout)
    {
        this.tcpReadTimeout = tcpReadTimeout;
    }

    public void setAddresses(String[] addresses)
    {
        this.addresses = addresses;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    protected void doStart()
    {
        serverSockets = new ServerSocket[addresses.length];
        Runnable[] acceptors = new Runnable[addresses.length];
        for (int i = 0; i < addresses.length; ++i)
        {
            InetSocketAddress bindAddress = new InetSocketAddress(addresses[i], port);
            serverSockets[i] = createServerSocket(bindAddress);
            if (logger.isLoggable(Level.FINE)) logger.fine("Bound server socket to " + bindAddress);
            acceptors[i] = new Acceptor(serverSockets[i]);
            accept(acceptors[i]);
        }
    }

    private ServerSocket createServerSocket(InetSocketAddress address)
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
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
        }
    }

    protected void doStop()
    {
        for (ServerSocket serverSocket : serverSockets) closeServerSocket(serverSocket);
        threadPool.shutdownNow();
        clearMessageListeners();
    }

    private void closeServerSocket(ServerSocket serverSocket)
    {
        try
        {
            if (serverSocket != null) serverSocket.close();
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
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
            if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "TCPConnectorServer " + this + " stopping, rejecting execution of " + handler);
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

            try
            {
                while (true)
                {
                    Socket client = serverSocket.accept();
                    if (logger.isLoggable(Level.FINE)) logger.fine("Client connected from " + client);
                    handle(new Handler(client));
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
            }
        }
    }

    private class Handler implements Runnable
    {
        private final Socket socket;

        public Handler(Socket socket)
        {
            this.socket = socket;
        }

        public void run()
        {
            if (logger.isLoggable(Level.FINER))
                logger.finer("Socket handler running for " + socket + " in thread " + Thread.currentThread().getName());

            try
            {
                try
                {
                    socket.setSoTimeout(tcpReadTimeout);
                }
                catch (SocketException ignored)
                {
                    if (logger.isLoggable(Level.FINEST))
                        logger.finest("Count not set read timeout on socket " + socket + ", ignoring");
                }

                try
                {
                    while (true)
                    {
                        byte[] messageBytes = connector.read(socket);
                        Message message = Message.deserialize(messageBytes);
                        MessageEvent event = new MessageEvent(socket, message, (InetSocketAddress)socket.getLocalSocketAddress(), (InetSocketAddress)socket.getRemoteSocketAddress());
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
            return new SocketTCPConnectorServer(settings);
        }
    }
}
