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
package org.livetribe.slp.spi.sa;

import java.net.InetSocketAddress;

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.IdentifierExtension;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SAAdvert;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * @version $Revision$ $Date$
 */
public class UDPSAAdvertPerformer
{
    private final UDPConnector udpConnector;

    public UDPSAAdvertPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
    }

    public void perform(InetSocketAddress localAddress, InetSocketAddress remoteAddress, ServiceAgentInfo serviceAgent, Message message)
    {
        SAAdvert saAdvert = newSAAdvert(serviceAgent, message);
        byte[] bytes = saAdvert.serialize();
        udpConnector.send(localAddress.getAddress().getHostAddress(), remoteAddress, bytes);
    }

    private SAAdvert newSAAdvert(ServiceAgentInfo serviceAgent, Message message)
    {
        SAAdvert saAdvert = new SAAdvert();
        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        saAdvert.setLanguage(message.getLanguage());
        saAdvert.setXID(message.getXID());
        saAdvert.setAttributes(serviceAgent.getAttributes());
        saAdvert.setScopes(serviceAgent.getScopes());
        saAdvert.setURL(serviceAgent.getURL());
        if (serviceAgent.hasIdentifier())
        {
            IdentifierExtension extension = new IdentifierExtension(serviceAgent.getHostAddress(), serviceAgent.getIdentifier());
            saAdvert.addExtension(extension);
        }
        return saAdvert;
    }
}
