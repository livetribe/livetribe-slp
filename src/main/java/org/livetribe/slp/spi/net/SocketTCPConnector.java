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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

import org.livetribe.slp.spi.Defaults;

/**
 * @version $Rev$ $Date$
 */
public class SocketTCPConnector extends TCPConnector
{
    private int port = Defaults.PORT;
    private ServerSocket[] serverSockets;

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    protected Runnable[] createAcceptors() throws IOException
    {
        if (!isTCPListening()) return null;

        InetAddress[] interfaceAddresses = getInetAddresses();
        InetSocketAddress[] bindAddresses = null;
        if (interfaceAddresses == null || interfaceAddresses.length == 0)
        {
            bindAddresses = new InetSocketAddress[1];
            bindAddresses[0] = new InetSocketAddress((InetAddress)null, getPort());
        }
        else
        {
            bindAddresses = new InetSocketAddress[interfaceAddresses.length];
            for (int i = 0; i < bindAddresses.length; ++i)
            {
                bindAddresses[i] = new InetSocketAddress(interfaceAddresses[i], getPort());
            }
        }

        serverSockets = new ServerSocket[bindAddresses.length];
        Runnable[] acceptors = new Runnable[bindAddresses.length];
        for (int i = 0; i < bindAddresses.length; ++i)
        {
            InetSocketAddress bindAddress = bindAddresses[i];
            serverSockets[i] = new ServerSocket();
            serverSockets[i].setReuseAddress(true);
            serverSockets[i].bind(bindAddress);
            if (logger.isLoggable(Level.FINE)) logger.fine("Bound server socket to " + bindAddress);

            acceptors[i] = new Acceptor(serverSockets[i]);
        }

        return acceptors;
    }

    protected void destroyAcceptors() throws IOException
    {
        if (!isTCPListening()) return;

        for (int i = 0; i < serverSockets.length; ++i)
        {
            ServerSocket socket = serverSockets[i];
            if (socket != null)
            {
                socket.close();
                if (logger.isLoggable(Level.FINE)) logger.fine("Closed server socket " + socket);
            }
        }
    }

    public byte[] receive(Socket socket) throws MessageTooBigException, SocketClosedException, IOException
    {
        InputStream input = socket.getInputStream();
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        // Read the SLP version
        int slpVersion = read(input);
        data.write(slpVersion);

        // Read the Message type
        int messageType = read(input);
        data.write(messageType);

        // Read the length of the message, network byte order
        int length1 = read(input);
        data.write(length1);
        int length2 = read(input);
        data.write(length2);
        int length3 = read(input);
        data.write(length3);

        int length = (length1 << 16) + (length2 << 8) + length3;
        if (logger.isLoggable(Level.FINEST)) logger.finest("Expecting incoming TCP unicast message of length " + length);

        int maxLength = getMaxTCPMessageLength();
        if (length > maxLength) throw new MessageTooBigException("Message length " + length + " is greater than max allowed message length " + maxLength);

        byte[] buffer = new byte[32];
        int totalRead = 5; // Five bytes already read (see above)
        while (totalRead < length)
        {
            int count = Math.min(buffer.length, length - totalRead);
            int read = input.read(buffer, 0, count);
            if (read < 0) throw new SocketClosedException();
            totalRead += read;
            if (logger.isLoggable(Level.FINEST)) logger.finest("Read " + totalRead + " bytes of incoming TCP unicast message");
            data.write(buffer, 0, read);
        }

        return data.toByteArray();
    }

    private int read(InputStream stream) throws IOException
    {
        int result = stream.read();
        if (result < 0) throw new SocketClosedException();
        return result;
    }

    private void closeNoExceptions(Socket socket)
    {
        try
        {
            socket.close();
        }
        catch (IOException x)
        {
            if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "Unexpected IOException", x);
        }
    }

    public Socket send(byte[] messageBytes, InetAddress address, boolean closeSocket) throws ConnectException, IOException
    {
        Socket socket = new Socket(address, getPort());
        try
        {
            write(socket, messageBytes);
        }
        finally
        {
            if (closeSocket)
            {
                closeNoExceptions(socket);
                socket = null;
            }
        }
        return socket;
    }

    public void reply(Socket socket, byte[] messageBytes) throws IOException
    {
        write(socket, messageBytes);
    }

    private void write(Socket socket, byte[] bytes) throws IOException
    {
        OutputStream output = socket.getOutputStream();
        output.write(bytes);
        output.flush();
        if (logger.isLoggable(Level.FINEST)) logger.finest("Sent TCP unicast message to " + socket.getRemoteSocketAddress());
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
            if (logger.isLoggable(Level.FINER)) logger.finer("Socket acceptor thread running for " + serverSocket);

            while (isRunning())
            {
                try
                {
                    Socket client = serverSocket.accept();
                    if (logger.isLoggable(Level.FINE)) logger.fine("Client connected from " + client);
                    handle(new Handler(client));
                }
                catch (SocketException x)
                {
                    if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "Closed server socket " + serverSocket, x);
                    break;
                }
                catch (SocketTimeoutException x)
                {
                    // Timed out, but the server socket is still valid, don't shut down
                    if (logger.isLoggable(Level.FINEST)) logger.finest("Accept timeout on server socket " + serverSocket);
                }
                catch (IOException x)
                {
                    if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Unexpected IOException", x);
                    break;
                }
            }

            if (logger.isLoggable(Level.FINER)) logger.finer("Socket acceptor thread exiting for " + serverSocket);
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
            try
            {
                socket.setSoTimeout(getTCPReadTimeout());
            }
            catch (SocketException ignored)
            {
                if (logger.isLoggable(Level.FINEST)) logger.finest("Count not set read timeout on socket " + socket + ", ignoring");
            }

            try
            {
                while (true)
                {
                    byte[] messageBytes = receive(socket);
                    MessageEvent event = new MessageEvent(socket, messageBytes, (InetSocketAddress)socket.getRemoteSocketAddress());
                    notifyMessageListeners(event);
                }
            }
            catch (SocketTimeoutException x)
            {
                // socket is too slow, kill this connection, as it may be an attack
                if (logger.isLoggable(Level.FINEST)) logger.finest("Read timeout, closing socket " + socket);
                closeNoExceptions(socket);
            }
            catch (MessageTooBigException x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Bad message, closing socket" + socket, x);
                closeNoExceptions(socket);
            }
            catch (SocketClosedException x)
            {
                if (logger.isLoggable(Level.FINE)) logger.fine("Client closed socket " + socket);
                closeNoExceptions(socket);
            }
            catch (IOException x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Unexpected IOException, closing socket " + socket, x);
                closeNoExceptions(socket);
            }
        }
    }
}
