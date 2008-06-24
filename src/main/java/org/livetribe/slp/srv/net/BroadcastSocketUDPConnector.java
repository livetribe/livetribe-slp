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

import java.net.DatagramSocket;
import java.net.SocketException;

import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Revision$ $Date$
 */
public class BroadcastSocketUDPConnector extends SocketUDPConnector
{
    private String broadcastAddress = Defaults.get(BROADCAST_ADDRESS_KEY);

    public BroadcastSocketUDPConnector(Settings settings)
    {
        super(settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(BROADCAST_ADDRESS_KEY)) setBroadcastAddress(settings.get(BROADCAST_ADDRESS_KEY));
    }

    public void setBroadcastAddress(String broadcastAddress)
    {
        this.broadcastAddress = broadcastAddress;
    }

    protected String getManycastAddress()
    {
        return broadcastAddress;
    }

    @Override
    protected DatagramSocket newDatagramSocket(String localAddress)
    {
        try
        {
            DatagramSocket socket = super.newDatagramSocket(localAddress);
            socket.setBroadcast(true);
            return socket;
        }
        catch (SocketException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.NETWORK_ERROR);
        }
    }
}
