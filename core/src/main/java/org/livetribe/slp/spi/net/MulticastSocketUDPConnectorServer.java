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
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.MULTICAST_ADDRESS_KEY;
import org.livetribe.slp.settings.Settings;

/**
 * @version $Revision$ $Date$
 */
public class MulticastSocketUDPConnectorServer extends SocketUDPConnectorServer
{
    private String multicastAddress = Defaults.get(MULTICAST_ADDRESS_KEY);

    public MulticastSocketUDPConnectorServer(ExecutorService threadPool, int bindPort)
    {
        this(threadPool, bindPort, null);
    }

    public MulticastSocketUDPConnectorServer(ExecutorService threadPool, int bindPort, Settings settings)
    {
        super(threadPool, bindPort, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(MULTICAST_ADDRESS_KEY)) this.multicastAddress = settings.get(MULTICAST_ADDRESS_KEY);
    }

    public String getMulticastAddress()
    {
        return multicastAddress;
    }

    public void setMulticastAddress(String multicastAddress)
    {
        this.multicastAddress = multicastAddress;
    }

    protected MulticastSocket newMulticastSocket(InetSocketAddress bindAddress)
    {
        try
        {
            MulticastSocket multicastSocket = new MulticastSocket(bindAddress);
            if (logger.isLoggable(Level.FINER)) logger.finer("Bound multicast socket to " + bindAddress);
            multicastSocket.setTimeToLive(getMulticastTimeToLive());
            multicastSocket.joinGroup(NetUtils.getByName(multicastAddress));
            if (logger.isLoggable(Level.FINER))
                logger.finer("Multicast socket " + multicastSocket + " joined multicast group " + multicastAddress);
            return multicastSocket;
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, SLPError.NETWORK_INIT_FAILED);
        }
    }

    protected void closeMulticastSocket(MulticastSocket multicastSocket)
    {
        try
        {
            if (multicastSocket != null)
            {
                multicastSocket.leaveGroup(NetUtils.getByName(multicastAddress));
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Multicast socket " + multicastSocket + " left multicast group " + multicastAddress);
                multicastSocket.close();
                if (logger.isLoggable(Level.FINER)) logger.finer("Closed multicast socket " + multicastSocket);
            }
        }
        catch (IOException x)
        {
            throw new ServiceLocationException(x, SLPError.NETWORK_ERROR);
        }
    }
}
