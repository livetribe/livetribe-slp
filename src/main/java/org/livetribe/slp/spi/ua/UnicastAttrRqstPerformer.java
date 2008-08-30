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
package org.livetribe.slp.spi.ua;

import java.net.InetSocketAddress;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.AttrRply;
import org.livetribe.slp.spi.msg.AttrRqst;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * @version $Revision$ $Date$
 */
public class UnicastAttrRqstPerformer extends AttrRqstPerformer
{
    private final UDPConnector udpConnector;
    private final TCPConnector tcpConnector;

    public UnicastAttrRqstPerformer(UDPConnector udpConnector, TCPConnector tcpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
        this.tcpConnector = tcpConnector;
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
    }

    public AttrRply perform(InetSocketAddress address, boolean preferTCP, String url, String language, Scopes scopes, Attributes tags)
    {
        AttrRqst attrRqst = newAttrRqst(url, language, scopes, tags);
        byte[] attrRqstBytes = attrRqst.serialize();
        byte[] attrRplyBytes = null;
        if (preferTCP)
            attrRplyBytes = tcpConnector.writeAndRead(address, attrRqstBytes);
        else
            attrRplyBytes = udpConnector.sendAndReceive(address, attrRqstBytes);
        return (AttrRply)Message.deserialize(attrRplyBytes);
    }
}
