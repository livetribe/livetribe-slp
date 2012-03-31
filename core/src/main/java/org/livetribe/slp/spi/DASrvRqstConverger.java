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
package org.livetribe.slp.spi;

import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.net.UDPConnector;


/**
 *
 */
public class DASrvRqstConverger extends Converger<DAAdvert>
{
    public DASrvRqstConverger(UDPConnector udpConnector, Settings settings)
    {
        super(udpConnector, settings);
    }

    protected DAAdvert convert(byte[] rplyBytes, InetSocketAddress address)
    {
        Message message = Message.deserialize(rplyBytes);
        if (Message.DA_ADVERT_TYPE != message.getMessageType())
        {
            if (logger.isLoggable(Level.FINEST))
                logger.finest("Ignoring message received from " + address + ": " + message + ", expecting DAAdvert");
            return null;
        }
        return (DAAdvert)message;
    }
}
