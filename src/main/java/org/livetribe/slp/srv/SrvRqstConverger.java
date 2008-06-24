/*
 * Copyright 2005 the original author or authors
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
package org.livetribe.slp.srv;

import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.livetribe.slp.srv.msg.SrvRply;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.settings.Settings;

/**
 * @version $Revision$ $Date$
 */
public class SrvRqstConverger extends Converger<SrvRply>
{
    public SrvRqstConverger(UDPConnector udpConnector, Settings settings)
    {
        super(udpConnector, settings);
    }

    protected SrvRply handle(byte[] rplyBytes, InetSocketAddress address)
    {
        Message message = Message.deserialize(rplyBytes);
        if (Message.SRV_RPLY_TYPE != message.getMessageType())
        {
            if (logger.isLoggable(Level.FINEST))
                logger.finest("Ignoring message received from " + address + ": " + message + ", expecting SrvRply");
            return null;
        }

        SrvRply srvRply = (SrvRply)message;
        srvRply.setResponder(address.getAddress().getHostAddress());
        return srvRply;
    }
}
