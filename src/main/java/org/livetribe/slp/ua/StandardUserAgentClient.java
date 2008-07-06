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
package org.livetribe.slp.ua;

import java.util.List;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.da.DirectoryAgentInfo;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.ua.AbstractUserAgent;

/**
 * @version $Revision$ $Date$
 */
public class StandardUserAgentClient extends AbstractUserAgent implements UserAgentClient
{
    public static UserAgentClient newInstance(Settings settings)
    {
        UDPConnector.Factory udpFactory = org.livetribe.slp.settings.Factory.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = org.livetribe.slp.settings.Factory.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        return new StandardUserAgentClient(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), settings);
    }

    public StandardUserAgentClient(UDPConnector tcpConnector, TCPConnector udpConnector, Settings settings)
    {
        super(tcpConnector, udpConnector, settings);
    }

    protected List<DirectoryAgentInfo> findDirectoryAgents(Scopes scopes, Filter filter)
    {
        return discoverDirectoryAgents(scopes, filter);
    }

    public static class Factory implements UserAgentClient.Factory
    {
        public UserAgentClient newUserAgentClient(Settings settings)
        {
            return newInstance(settings);
        }
    }
}
