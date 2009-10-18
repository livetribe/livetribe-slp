/*
 * Copyright 2008-2009 the original author or authors
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
package org.livetribe.slp.spi.da;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.SrvRplyPerformer;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.net.UDPConnector;


/**
 * @version $Revision$ $Date$
 */
public class UDPSrvRplyPerformer extends SrvRplyPerformer
{
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private int maxTransmissionUnit = Defaults.get(Keys.MAX_TRANSMISSION_UNIT_KEY);
    private final UDPConnector udpConnector;

    public UDPSrvRplyPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(Keys.MAX_TRANSMISSION_UNIT_KEY))
            setMaxTransmissionUnit(settings.get(Keys.MAX_TRANSMISSION_UNIT_KEY));
    }

    public void setMaxTransmissionUnit(int maxTransmissionUnit)
    {
        this.maxTransmissionUnit = maxTransmissionUnit;
    }

    public void perform(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Message message, List<? extends ServiceInfo> services)
    {
        SrvRply srvRply = newSrvRply(message, services);
        byte[] bytes = srvRply.serialize();

        if (bytes.length > maxTransmissionUnit)
        {
            logger.finer("Message bigger than maxTransmissionUnit, truncating and setting overflow bit");

            byte[] truncated = new byte[maxTransmissionUnit];
            System.arraycopy(bytes, 0, truncated, 0, truncated.length);
            truncated[5] |= 0x80;
            bytes = truncated;
        }

        udpConnector.send(localAddress.getAddress().getHostAddress(), remoteAddress, bytes);
    }
}
