/*
 * Copyright 2008-2008 the original author or authors
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
package org.livetribe.slp.spi;

import java.net.InetSocketAddress;
import java.util.List;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.MAX_TRANSMISSION_UNIT_KEY;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvTypeRply;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * @version $Revision$ $Date$
 */
public class UDPSrvTypeRplyPerformer extends SrvTypeRplyPerformer
{
    private final UDPConnector udpConnector;
    private int maxTransmissionUnit = Defaults.get(MAX_TRANSMISSION_UNIT_KEY);

    public UDPSrvTypeRplyPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(MAX_TRANSMISSION_UNIT_KEY))
            this.maxTransmissionUnit = settings.get(MAX_TRANSMISSION_UNIT_KEY);
    }

    public void perform(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Message message, List<ServiceType> serviceTypes)
    {
        SrvTypeRply srvTypeRply = newSrvTypeRply(message, serviceTypes, maxTransmissionUnit);
        byte[] srvTypeRplyBytes = srvTypeRply.serialize();
        send(localAddress, remoteAddress, srvTypeRplyBytes);
    }

    public void perform(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Message message, SLPError error)
    {
        SrvTypeRply srvTypeRply = newSrvTypeRply(message, error);
        byte[] srvTypeRplyBytes = srvTypeRply.serialize();
        send(localAddress, remoteAddress, srvTypeRplyBytes);
    }

    protected void send(InetSocketAddress localAddress, InetSocketAddress remoteAddress, byte[] srvTypeRplyBytes)
    {
        udpConnector.send(localAddress.getAddress().getHostAddress(), remoteAddress, srvTypeRplyBytes);
    }
}
