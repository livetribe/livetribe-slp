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

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * @version $Revision$ $Date$
 */
public class UDPSrvAckPerformer extends SrvAckPerformer
{
    private final UDPConnector udpConnector;

    public UDPSrvAckPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
    }

    public void perform(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Message message, int errorCode)
    {
        SrvAck srvAck = newSrvAck(message, errorCode);
        byte[] bytes = srvAck.serialize();
        udpConnector.send(localAddress.getAddress().getHostAddress(), remoteAddress, bytes);
    }
}
