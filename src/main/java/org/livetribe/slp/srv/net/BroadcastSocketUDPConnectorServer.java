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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.logging.Level;

import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;

/**
 * @version $Revision$ $Date$
 */
public class BroadcastSocketUDPConnectorServer extends SocketUDPConnectorServer
{
    private String[] addresses = Defaults.get(ADDRESSES_KEY);
    private MulticastSocket[] broadcatSockets;

    public BroadcastSocketUDPConnectorServer(Settings settings, int bindPort)
    {
        super(settings, bindPort);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(ADDRESSES_KEY)) setAddresses(settings.get(ADDRESSES_KEY));
    }

    public void setAddresses(String[] addresses)
    {
        this.addresses = addresses;
    }

    protected void doStart()
    {
        broadcatSockets = new MulticastSocket[addresses.length];
        Runnable[] receivers = new Runnable[addresses.length];
        for (int i = 0; i < addresses.length; ++i)
        {
            InetSocketAddress bindAddress = new InetSocketAddress(addresses[i], getBindPort());
            broadcatSockets[i] = newBroadcastSocket(bindAddress);
            receivers[i] = new Receiver(broadcatSockets[i]);
            receive(receivers[i]);
        }
    }

    private MulticastSocket newBroadcastSocket(InetSocketAddress bindAddress)
    {
        try
        {
            MulticastSocket broadcastSocket = new MulticastSocket(bindAddress);
            broadcastSocket.setBroadcast(true);
            if (logger.isLoggable(Level.FINER)) logger.finer("Bound broadcast socket to " + bindAddress);
            return broadcastSocket;
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
        }
    }

    @Override
    protected void doStop()
    {
        for (MulticastSocket broadcastSocket : broadcatSockets) close(broadcastSocket);
        super.doStop();
    }

    private void close(MulticastSocket broadcastSocket)
    {
        if (broadcastSocket != null)
        {
            broadcastSocket.close();
            if (logger.isLoggable(Level.FINER)) logger.finer("Closed datagram socket " + broadcastSocket);
        }
    }
}
