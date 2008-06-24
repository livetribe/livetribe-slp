/*
 * Copyright 2007 the original author or authors
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.msg.Message;

/**
 * @version $Revision$ $Date$
 */
public abstract class SocketUDPConnectorServer extends AbstractConnectorServer implements UDPConnectorServer
{
    private final int bindPort;
    private ExecutorService threadPool = Defaults.get(EXECUTOR_SERVICE_KEY);
    private int maxTransmissionUnit = Defaults.get(MAX_TRANSMISSION_UNIT_KEY);
    private int multicastTimeToLive = Defaults.get(MULTICAST_TIME_TO_LIVE_KEY);

    public SocketUDPConnectorServer(Settings settings, int bindPort)
    {
        this.bindPort = bindPort;
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(EXECUTOR_SERVICE_KEY)) setThreadPool(settings.get(EXECUTOR_SERVICE_KEY));
        if (settings.containsKey(MAX_TRANSMISSION_UNIT_KEY)) setMaxTransmissionUnit(settings.get(MAX_TRANSMISSION_UNIT_KEY));
        if (settings.containsKey(MULTICAST_TIME_TO_LIVE_KEY)) setMulticastTimeToLive(settings.get(MULTICAST_TIME_TO_LIVE_KEY));
    }

    protected int getBindPort()
    {
        return bindPort;
    }

    public void setThreadPool(ExecutorService threadPool)
    {
        this.threadPool = threadPool;
    }

    public void setMaxTransmissionUnit(int maxTransmissionUnit)
    {
        this.maxTransmissionUnit = maxTransmissionUnit;
    }

    protected int getMulticastTimeToLive()
    {
        return multicastTimeToLive;
    }

    public void setMulticastTimeToLive(int multicastTimeToLive)
    {
        this.multicastTimeToLive = multicastTimeToLive;
    }

    protected void doStop()
    {
        threadPool.shutdownNow();
        clearMessageListeners();
    }

    protected void receive(Runnable receiver)
    {
        threadPool.execute(receiver);
    }

    private void handle(Runnable handler) throws RejectedExecutionException
    {
        try
        {
            threadPool.execute(handler);
        }
        catch (RejectedExecutionException x)
        {
            // Connector server stopped just after having received a datagram
            if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "UDPConnectorServer " + this + " stopping, rejecting execution of " + handler);
            throw x;
        }
    }

    protected class Receiver implements Runnable
    {
        private final DatagramSocket datagramSocket;

        public Receiver(DatagramSocket datagramSocket)
        {
            this.datagramSocket = datagramSocket;
        }

        public void run()
        {
            if (logger.isLoggable(Level.FINER))
                logger.finer("DatagramSocket acceptor running for " + datagramSocket + " in thread " + Thread.currentThread().getName());

            try
            {
                InetSocketAddress localAddress = (InetSocketAddress)datagramSocket.getLocalSocketAddress();
                while (true)
                {
                    byte[] buffer = new byte[maxTransmissionUnit];
                    DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                    datagramSocket.receive(packet);
                    if (logger.isLoggable(Level.FINER)) logger.finer("Received datagram packet " + packet + " on socket " + datagramSocket + ": " + packet.getLength() + " bytes from " + packet.getSocketAddress());
                    handle(new Handler(packet, localAddress));
                }
            }
            catch (SocketException x)
            {
                if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "Closed MulticastSocket " + datagramSocket);
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
                    logger.finer("MulticastSocket acceptor exiting for " + datagramSocket + " in thread " + Thread.currentThread().getName());
            }
        }
    }

    private class Handler implements Runnable
    {
        private final DatagramPacket packet;
        private final InetSocketAddress localAddress;

        public Handler(DatagramPacket packet, InetSocketAddress localAddress)
        {
            this.packet = packet;
            this.localAddress = localAddress;
        }

        public void run()
        {
            if (logger.isLoggable(Level.FINER))
                logger.finer("DatagramPacket handler running for " + packet + " in thread " + Thread.currentThread().getName());

            try
            {
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
                Message message = Message.deserialize(data);
                MessageEvent event = new MessageEvent(packet, message, localAddress, (InetSocketAddress)packet.getSocketAddress());
                notifyMessageListeners(event);
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "", x);
            }
            finally
            {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("DatagramPacket handler exiting for " + packet + " in thread " + Thread.currentThread().getName());
            }
        }
    }

    public static class Factory implements UDPConnectorServer.Factory
    {
        public UDPConnectorServer newUDPConnectorServer(Settings settings)
        {
            int udpBindPort = settings == null ? Defaults.get(PORT_KEY) : settings.get(PORT_KEY, Defaults.get(PORT_KEY));
            return newNotificationUDPConnectorServer(settings, udpBindPort);
        }

        public UDPConnectorServer newNotificationUDPConnectorServer(Settings settings)
        {
            int udpBindPort = settings == null ? Defaults.get(NOTIFICATION_PORT_KEY) : settings.get(NOTIFICATION_PORT_KEY, Defaults.get(NOTIFICATION_PORT_KEY));
            return newNotificationUDPConnectorServer(settings, udpBindPort);
        }

        private UDPConnectorServer newNotificationUDPConnectorServer(Settings settings, int bindPort)
        {
            Boolean broadcastEnabled = settings == null ? Boolean.FALSE : settings.get(BROADCAST_ENABLED_KEY);
            if (broadcastEnabled == null || !broadcastEnabled)
                return new MulticastSocketUDPConnectorServer(settings, bindPort);
            else
                return new BroadcastSocketUDPConnectorServer(settings, bindPort);
        }
    }
}
