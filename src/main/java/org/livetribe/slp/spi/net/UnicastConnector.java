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
import java.net.Socket;
import java.net.ConnectException;

import org.livetribe.slp.api.Configuration;

/**
 * @version $Rev$ $Date$
 */
public abstract class UnicastConnector extends NetworkConnector
{
    private boolean unicastListening;
    private int unicastReadTimeout;
    private int maxUnicastMessageLength;

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        setUnicastReadTimeout(configuration.getUnicastReadTimeout());
        setMaxUnicastMessageLength(configuration.getUnicastMaxMessageLength());
    }

    public boolean isUnicastListening()
    {
        return unicastListening;
    }

    public void setUnicastListening(boolean unicastListening)
    {
        this.unicastListening = unicastListening;
    }

    public int getUnicastReadTimeout()
    {
        return unicastReadTimeout;
    }

    public void setUnicastReadTimeout(int unicastReadTimeout)
    {
        this.unicastReadTimeout = unicastReadTimeout;
    }

    public int getMaxUnicastMessageLength()
    {
        return maxUnicastMessageLength;
    }

    public void setMaxUnicastMessageLength(int maxUnicastMessageLength)
    {
        this.maxUnicastMessageLength = maxUnicastMessageLength;
    }

    /**
     * Reads bytes containing an SLP message.
     * @param socket The socket to read the message from
     * @return The bytes containing the SLP message
     * @throws MessageTooBigException If the message length (as read from the SLP message header) is greater than
     * the {@link #getMaxUnicastMessageLength() maximum message length}
     * @throws SocketClosedException If the socket is closed by the client before the reading of all message
     * bytes is completed
     * @throws IOException In case of communication errors
     */
    public abstract byte[] receive(Socket socket) throws MessageTooBigException, SocketClosedException, IOException;

    /**
     * Send the bytes containing an SLP message.
     * @param messageBytes The bytes containing the SLP message
     * @param address The address to send the bytes to
     * @param closeSocket True if the socket must be closed
     * @return The socket used to send the bytes, or null if the socket has been closed
     * @throws ConnectException If the destination is not listening
     * @throws IOException In case of communication errors
     */
    public abstract Socket send(byte[] messageBytes, InetAddress address, boolean closeSocket) throws ConnectException, IOException;

    /**
     * Sends the bytes containing an SLP message in reply to a previos request, via the given socket.
     * @param socket The socket where the reply must be written to
     * @param messageBytes The bytes containing the SLP message
     * @throws IOException In case of communication errors
     */
    public abstract void reply(Socket socket, byte[] messageBytes) throws IOException;
}
