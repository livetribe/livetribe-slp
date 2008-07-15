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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
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
public abstract class SocketUDPConnector implements UDPConnector
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private int port = Defaults.get(PORT_KEY);
    private int notificationPort = Defaults.get(NOTIFICATION_PORT_KEY);
    private int maxTransmissionUnit = Defaults.get(MAX_TRANSMISSION_UNIT_KEY);

    public SocketUDPConnector()
    {
        this(null);
    }

    public SocketUDPConnector(Settings settings)
    {
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(PORT_KEY)) setPort(settings.get(PORT_KEY));
        if (settings.containsKey(NOTIFICATION_PORT_KEY)) setNotificationPort(settings.get(NOTIFICATION_PORT_KEY));
        if (settings.containsKey(MAX_TRANSMISSION_UNIT_KEY))
            setMaxTransmissionUnit(settings.get(MAX_TRANSMISSION_UNIT_KEY));
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setNotificationPort(int notificationPort)
    {
        this.notificationPort = notificationPort;
    }

    public void setMaxTransmissionUnit(int maxTransmissionUnit)
    {
        this.maxTransmissionUnit = maxTransmissionUnit;
    }

    protected abstract String getManycastAddress();

    /**
     * @return a new DatagramSocket bound to the wildcard address and an ephemeral port
     */
    public DatagramSocket newDatagramSocket()
    {
        return newDatagramSocket(null);
    }

    protected DatagramSocket newDatagramSocket(String localAddress)
    {
        try
        {
            return new DatagramSocket(localAddress == null ? new InetSocketAddress(0) : new InetSocketAddress(localAddress, 0));
        }
        catch (SocketException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
        }
    }

    public void manycastSend(String localAddress, byte[] bytes)
    {
        DatagramSocket datagramSocket = newDatagramSocket(localAddress);
        try
        {
            manycastSend(datagramSocket, bytes);
        }
        finally
        {
            datagramSocket.close();
        }
    }

    public void manycastSend(DatagramSocket socket, byte[] bytes)
    {
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        InetSocketAddress address = new InetSocketAddress(getManycastAddress(), port);
        packet.setSocketAddress(address);
        send(socket, packet);
    }

    private void send(DatagramSocket socket, DatagramPacket packet)
    {
        try
        {
            socket.send(packet);
            if (logger.isLoggable(Level.FINER))
                logger.finer("Sent datagram " + packet + " (" + packet.getLength() + " bytes) to " + packet.getSocketAddress() + " from socket " + socket.getLocalSocketAddress());
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
        }
    }

    public DatagramPacket receive(DatagramSocket socket, int timeout)
    {
        byte[] buffer = new byte[maxTransmissionUnit];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try
        {
            socket.setSoTimeout(timeout);
            socket.receive(packet);
            if (logger.isLoggable(Level.FINER))
                logger.finer("Received datagram packet " + packet + " on socket " + socket + ": " + packet.getLength() + " bytes from " + packet.getSocketAddress());
            return packet;
        }
        catch (SocketTimeoutException x)
        {
            if (logger.isLoggable(Level.FINER))
                logger.finer("Timeout (" + timeout + " ms) expired during receive() on socket " + socket);
            return null;
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
        }
    }

    public void unicastSend(String localAddress, InetSocketAddress remoteAddress, byte[] bytes)
    {
        DatagramSocket socket = newDatagramSocket(localAddress);
        try
        {
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            packet.setSocketAddress(remoteAddress);
            send(socket, packet);
        }
        finally
        {
            socket.close();
        }
    }

    public void manycastNotify(String localAddress, byte[] bytes)
    {
        DatagramSocket datagramSocket = newDatagramSocket(localAddress);
        try
        {
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            InetSocketAddress address = new InetSocketAddress(getManycastAddress(), notificationPort);
            packet.setSocketAddress(address);
            send(datagramSocket, packet);
        }
        finally
        {
            datagramSocket.close();
        }
    }

    public static class Factory implements UDPConnector.Factory
    {
        public UDPConnector newUDPConnector(Settings settings)
        {
            Boolean broadcastEnabled = settings == null ? Boolean.FALSE : settings.get(BROADCAST_ENABLED_KEY, Defaults.get(BROADCAST_ENABLED_KEY));
            if (broadcastEnabled == null || !broadcastEnabled)
                return new MulticastSocketUDPConnector(settings);
            else
                return new BroadcastSocketUDPConnector(settings);
        }
    }
}
