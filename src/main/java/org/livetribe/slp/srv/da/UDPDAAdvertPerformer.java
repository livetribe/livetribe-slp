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
package org.livetribe.slp.srv.da;

import java.net.InetSocketAddress;

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.msg.DAAdvert;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.net.UDPConnector;

/**
 * @version $Revision$ $Date$
 */
public class UDPDAAdvertPerformer
{
    private final UDPConnector udpConnector;

    public UDPDAAdvertPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
    }

    public void perform(InetSocketAddress remoteAddress, DirectoryAgentInfo directoryAgent, Message message)
    {
        DAAdvert daAdvert = newDAAdvert(directoryAgent, message);
        byte[] bytes = daAdvert.serialize();
        udpConnector.unicastSend(directoryAgent.getHost(), remoteAddress, bytes);
    }

    private DAAdvert newDAAdvert(DirectoryAgentInfo directoryAgent, Message message)
    {
        DAAdvert daAdvert = new DAAdvert();
        // Replies must have the same language and XID as the request (RFC 2608, 8.0)
        daAdvert.setLanguage(message.getLanguage());
        daAdvert.setXID(message.getXID());
        daAdvert.setBootTime(directoryAgent.getBootTime());
        daAdvert.setURL(directoryAgent.getURL());
        daAdvert.setScopes(directoryAgent.getScopes());
        daAdvert.setAttributes(directoryAgent.getAttributes());
        return daAdvert;
    }
}
