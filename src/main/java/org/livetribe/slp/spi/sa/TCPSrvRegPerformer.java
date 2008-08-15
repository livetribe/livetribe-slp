/*
 * Copyright 2005-2008 the original author or authors
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

import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.net.TCPConnector;

/**
 * @version $Revision$ $Date$
 */
public class TCPSrvRegPerformer extends SrvRegPerformer
{
    private final TCPConnector tcpConnector;

    public TCPSrvRegPerformer(TCPConnector tcpConnector, Settings settings)
    {
        this.tcpConnector = tcpConnector;
    }

    public SrvAck perform(InetSocketAddress address, ServiceInfo service, boolean update)
    {
        SrvReg srvReg = newSrvReg(service, update);
        byte[] requestBytes = srvReg.serialize();
        byte[] replyBytes = tcpConnector.writeAndRead(address, requestBytes);
        return (SrvAck)Message.deserialize(replyBytes);
    }
}
