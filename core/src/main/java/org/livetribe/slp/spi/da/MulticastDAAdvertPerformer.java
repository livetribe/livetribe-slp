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
package org.livetribe.slp.spi.da;

import java.util.Collection;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.net.NetUtils;
import org.livetribe.slp.spi.net.UDPConnector;


/**
 *
 */
public class MulticastDAAdvertPerformer
{
    private final UDPConnector udpConnector;

    public MulticastDAAdvertPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
    }

    public void perform(Collection<DirectoryAgentInfo> directoryAgents, boolean shutdown)
    {
        for (DirectoryAgentInfo directoryAgent : directoryAgents)
        {
            String address = directoryAgent.getHostAddress();

            if (NetUtils.getLoopbackAddress().getHostAddress().equals(address))
            {
                // Skip advertisement on the loopback interface if there are other interfaces
                if (directoryAgents.size() > 1) continue;
            }

            DAAdvert daAdvert = newDAAdvert(directoryAgent, shutdown);
            byte[] bytes = daAdvert.serialize();
            udpConnector.manycastSend(address, bytes);
        }
    }

    private DAAdvert newDAAdvert(DirectoryAgentInfo directoryAgent, boolean shutdown)
    {
        DAAdvert daAdvert = new DAAdvert();
        daAdvert.setBootTime(shutdown ? 0 : directoryAgent.getBootTime());
        daAdvert.setScopes(directoryAgent.getScopes());
        daAdvert.setAttributes(directoryAgent.getAttributes());
        daAdvert.setLanguage(directoryAgent.getLanguage());
        daAdvert.setURL(directoryAgent.getURL());
        // RFC 2608, 8.0
        daAdvert.setXID(0);
        // RFC 2608, 8.5
        daAdvert.setSLPError(SLPError.NO_ERROR);
        daAdvert.setMulticast(true);
        return daAdvert;
    }
}
