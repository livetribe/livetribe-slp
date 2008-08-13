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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;

/**
 * @version $Revision$ $Date$
 */
public class SocketTCPConnector implements TCPConnector
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private int tcpMessageMaxLength = Defaults.get(TCP_MESSAGE_MAX_LENGTH_KEY);
    private int tcpReadTimeout = Defaults.get(TCP_READ_TIMEOUT_KEY);

    public SocketTCPConnector()
    {
        this(null);
    }

    public SocketTCPConnector(Settings settings)
    {
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(TCP_MESSAGE_MAX_LENGTH_KEY))
            this.tcpMessageMaxLength = settings.get(TCP_MESSAGE_MAX_LENGTH_KEY);
        if (settings.containsKey(TCP_READ_TIMEOUT_KEY)) this.tcpReadTimeout = settings.get(TCP_READ_TIMEOUT_KEY);
    }

    public int getTcpMessageMaxLength()
    {
        return tcpMessageMaxLength;
    }

    public void setTCPMessageMaxLength(int tcpMessageMaxLength)
    {
        this.tcpMessageMaxLength = tcpMessageMaxLength;
    }

    public int getTcpReadTimeout()
    {
        return tcpReadTimeout;
    }

    public void setTcpReadTimeout(int tcpReadTimeout)
    {
        this.tcpReadTimeout = tcpReadTimeout;
    }

    public byte[] writeAndRead(InetSocketAddress address, byte[] bytes)
    {
        Socket socket = null;
        try
        {
            socket = new Socket(address.getAddress(), address.getPort());
            write(socket, bytes);
            return read(socket);
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.Error.NETWORK_ERROR);
        }
        finally
        {
            close(socket);
        }
    }

    protected void close(Socket socket)
    {
        try
        {
            if (socket != null) socket.close();
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.Error.NETWORK_ERROR);
        }
    }

    public void write(Socket socket, byte[] bytes)
    {
        try
        {
            OutputStream output = socket.getOutputStream();
            output.write(bytes);
            output.flush();
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.Error.NETWORK_ERROR);
        }
    }

    public byte[] read(Socket socket) throws EOFException, SocketTimeoutException
    {
        try
        {
            socket.setSoTimeout(tcpReadTimeout);

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
            if (logger.isLoggable(Level.FINEST))
                logger.finest("Expecting incoming TCP unicast message of length " + length);

            int maxLength = tcpMessageMaxLength;
            if (length > maxLength)
                throw new IOException("Message length " + length + " is greater than max allowed message length " + maxLength);

            byte[] buffer = new byte[128];
            int totalRead = 5; // Five bytes already read (see above)
            while (totalRead < length)
            {
                int count = Math.min(buffer.length, length - totalRead);
                int read = input.read(buffer, 0, count);
                if (read < 0) throw new EOFException();
                totalRead += read;
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("Read " + totalRead + " bytes of incoming TCP unicast message");
                data.write(buffer, 0, read);
            }

            return data.toByteArray();
        }
        catch (EOFException x)
        {
            throw x;
        }
        catch (SocketTimeoutException x)
        {
            throw x;
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.Error.NETWORK_ERROR);
        }
    }

    private int read(InputStream stream) throws EOFException, SocketTimeoutException
    {
        try
        {
            int result = stream.read();
            if (result < 0) throw new EOFException();
            return result;
        }
        catch (EOFException x)
        {
            throw x;
        }
        catch (SocketTimeoutException x)
        {
            throw x;
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.Error.NETWORK_ERROR);
        }
    }

    public static class Factory implements TCPConnector.Factory
    {
        public TCPConnector newTCPConnector(Settings settings)
        {
            return new SocketTCPConnector(settings);
        }
    }
}
