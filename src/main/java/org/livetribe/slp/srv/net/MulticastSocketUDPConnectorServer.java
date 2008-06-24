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

import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.logging.Level;
import java.io.IOException;

import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Revision$ $Date$
 */
public class MulticastSocketUDPConnectorServer extends SocketUDPConnectorServer
{
    private String[] addresses = Defaults.get(ADDRESSES_KEY);
    private String multicastAddress = Defaults.get(MULTICAST_ADDRESS_KEY);
    private MulticastSocket[] multicastSockets;

    public MulticastSocketUDPConnectorServer(Settings settings, int bindPort)
    {
        super(settings, bindPort);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(ADDRESSES_KEY)) setAddresses(settings.get(ADDRESSES_KEY));
        if (settings.containsKey(MULTICAST_ADDRESS_KEY)) setMulticastAddress(settings.get(MULTICAST_ADDRESS_KEY));
    }

    public void setAddresses(String[] addresses)
    {
        this.addresses = addresses;
    }

    public void setMulticastAddress(String multicastAddress)
    {
        this.multicastAddress = multicastAddress;
    }

    protected void doStart()
    {
        multicastSockets = new MulticastSocket[addresses.length];
        Runnable[] receivers = new Runnable[addresses.length];
        for (int i = 0; i < addresses.length; ++i)
        {
            InetSocketAddress bindAddress = new InetSocketAddress(addresses[i], getBindPort());
            multicastSockets[i] = newMulticastSocket(bindAddress);
            receivers[i] = new Receiver(multicastSockets[i]);
            receive(receivers[i]);
        }
    }

    private MulticastSocket newMulticastSocket(InetSocketAddress bindAddress)
    {
        try
        {
            MulticastSocket multicastSocket = new MulticastSocket(bindAddress);
            if (logger.isLoggable(Level.FINER)) logger.finer("Bound multicast socket to " + bindAddress);
            multicastSocket.setLoopbackMode(true);
            multicastSocket.setTimeToLive(getMulticastTimeToLive());
            multicastSocket.joinGroup(NetUtils.getByName(multicastAddress));
            if (logger.isLoggable(Level.FINER)) logger.finer("Multicast socket " + multicastSocket + " joined multicast group " + multicastAddress);
            return multicastSocket;
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
        }
    }

    @Override
    protected void doStop()
    {
        for (MulticastSocket multicastSocket : multicastSockets) close(multicastSocket);
        super.doStop();
    }

    private void close(MulticastSocket multicastSocket)
    {
        try
        {
            if (multicastSocket != null)
            {
                multicastSocket.leaveGroup(NetUtils.getByName(multicastAddress));
                if (logger.isLoggable(Level.FINER)) logger.finer("Multicast socket " + multicastSocket + " left multicast group " + multicastAddress);
                multicastSocket.close();
                if (logger.isLoggable(Level.FINER)) logger.finer("Closed multicast socket " + multicastSocket);
            }
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
        }
    }
}
