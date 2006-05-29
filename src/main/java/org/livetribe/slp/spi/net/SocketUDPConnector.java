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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

/**
 * @version $Rev$ $Date$
 */
public class SocketUDPConnector extends UDPConnector
{
    private MulticastSocket[] sockets;

    protected Runnable[] createAcceptors() throws IOException
    {
        InetAddress[] interfaceAddresses = getInetAddresses();
        InetSocketAddress[] bindAddresses = null;
        if (interfaceAddresses == null || interfaceAddresses.length == 0)
        {
            // No interface addresses defined, bind on the wildcard address
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

        sockets = new MulticastSocket[bindAddresses.length];
        Runnable[] acceptors = new Runnable[bindAddresses.length];
        for (int i = 0; i < bindAddresses.length; ++i)
        {
            InetSocketAddress bindAddress = bindAddresses[i];
            sockets[i] = new MulticastSocket(bindAddress);
            if (logger.isLoggable(Level.FINE)) logger.fine("Bound multicast socket " + sockets[i] + " to " + bindAddress);

            sockets[i].setTimeToLive(getMulticastTimeToLive());
            sockets[i].setLoopbackMode(false);

            sockets[i].joinGroup(getMulticastAddress());
            if (logger.isLoggable(Level.FINE)) logger.fine("Multicast socket " + bindAddress + " joined multicast group " + getMulticastAddress());

            acceptors[i] = new Receiver(sockets[i]);
        }

        return acceptors;
    }

    protected void destroyAcceptors() throws IOException
    {
        for (int i = 0; i < sockets.length; ++i)
        {
            MulticastSocket socket = sockets[i];
            socket.leaveGroup(getMulticastAddress());
            if (logger.isLoggable(Level.FINE)) logger.fine("Multicast socket " + socket + " left multicast group " + getMulticastAddress());
            socket.close();
            if (logger.isLoggable(Level.FINE)) logger.fine("Closed multicast socket " + socket);
        }
    }

    public DatagramSocket unicastSend(DatagramSocket socket, InetSocketAddress address, byte[] bytes) throws IOException
    {
        return send(socket, address, bytes);
    }

    public DatagramSocket multicastSend(DatagramSocket socket, byte[] bytes) throws IOException
    {
        return send(socket, new InetSocketAddress(getMulticastAddress(), getPort()), bytes);
    }

    private DatagramSocket send(DatagramSocket socket, InetSocketAddress address, byte[] bytes) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        packet.setSocketAddress(address);

        // Use an ephemeral port to send the packet in case the socket has not been provided.
        // It is very important not to use the multicast socket to send datagrams, since there normally are
        // several multicast socket listening, and a unicast UDP sent via the multicast socket is only received
        // by one listener (which one depends on the operative system). If the receiving listener is the one
        // we used to send the message, the effect is exactly as if the message has never been sent.
        if (socket == null) socket = new DatagramSocket();
        socket.send(packet);
        if (logger.isLoggable(Level.FINER)) logger.finer("Sent datagram " + packet + " (" + packet.getLength() + " bytes) to " + packet.getSocketAddress() + " from socket " + socket.getLocalSocketAddress());
        return socket;
    }

    private class Receiver extends Acceptor
    {
        private final MulticastSocket socket;

        public Receiver(MulticastSocket socket)
        {
            this.socket = socket;
        }

        public void run()
        {
            if (logger.isLoggable(Level.FINER)) logger.finer("Multicast acceptor thread running for " + socket);

            while (isRunning())
            {
                try
                {
                    byte[] buffer = new byte[getMaxTransmissionUnit()];
                    DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                    socket.receive(packet);
                    if (logger.isLoggable(Level.FINER)) logger.finer("Received datagram packet " + packet + " on socket " + socket + ": " + packet.getLength() + " bytes from " + packet.getSocketAddress());
                    handle(new Handler(packet));
                }
                catch (SocketException x)
                {
                    if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "Closed server socket " + socket, x);
                    break;
                }
                catch (SocketTimeoutException x)
                {
                    // Timed out, but the socket is still valid, don't shut down
                    if (logger.isLoggable(Level.FINEST)) logger.finest("Timeout during receive() on multicast socket " + socket);
                }
                catch (IOException x)
                {
                    if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Unexpected IOException", x);
                    break;
                }
            }

            if (logger.isLoggable(Level.FINER)) logger.finer("Multicast acceptor thread exiting for " + socket);
        }
    }

    private class Handler implements Runnable
    {
        private final DatagramPacket packet;

        public Handler(DatagramPacket packet)
        {
            this.packet = packet;
        }

        public void run()
        {
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
            MessageEvent event = new MessageEvent(packet, data, (InetSocketAddress)packet.getSocketAddress());
            notifyMessageListeners(event);
        }
    }
}
