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

import org.livetribe.slp.Scopes;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvTypeRply;
import org.livetribe.slp.spi.msg.SrvTypeRqst;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;


/**
 *
 */
public class UnicastSrvTypeRqstPerformer extends SrvTypeRqstPerformer
{
    private final UDPConnector udpConnector;
    private final TCPConnector tcpConnector;

    public UnicastSrvTypeRqstPerformer(UDPConnector udpConnector, TCPConnector tcpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
        this.tcpConnector = tcpConnector;
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
    }

    public SrvTypeRply perform(InetSocketAddress address, boolean preferTCP, String namingAuthority, Scopes scopes)
    {
        SrvTypeRqst srvTypeRqst = newSrvTypeRqst(namingAuthority, scopes);
        byte[] srvTypeRqstBytes = srvTypeRqst.serialize();
        byte[] srvTypeRplyBytes = null;
        if (preferTCP)
            srvTypeRplyBytes = tcpConnector.writeAndRead(address, srvTypeRqstBytes);
        else
            srvTypeRplyBytes = udpConnector.sendAndReceive(address, srvTypeRqstBytes);
        return (SrvTypeRply)Message.deserialize(srvTypeRplyBytes);
    }
}
