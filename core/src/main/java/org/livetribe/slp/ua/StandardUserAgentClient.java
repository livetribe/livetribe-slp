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

import static org.livetribe.slp.settings.Keys.TCP_CONNECTOR_FACTORY_KEY;
import static org.livetribe.slp.settings.Keys.UDP_CONNECTOR_FACTORY_KEY;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Factories;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.da.DirectoryAgentInfoCache;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.ua.AbstractUserAgent;


/**
 *
 */
public class StandardUserAgentClient extends AbstractUserAgent implements UserAgentClient
{
    /**
     * Creates and configures a new StandardUserAgentClient instance.
     *
     * @param settings the configuration settings
     * @return a new configured StandardUserAgentClient
     * @see Factory#newUserAgentClient(Settings)
     */
    public static StandardUserAgentClient newInstance(Settings settings)
    {
        UDPConnector.Factory udpFactory = Factories.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = Factories.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        return new StandardUserAgentClient(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), settings);
    }

    private final DirectoryAgentInfoCache directoryAgents = new DirectoryAgentInfoCache();
    private String[] directoryAgentAddresses = Defaults.get(Keys.DA_ADDRESSES_KEY);

    public StandardUserAgentClient(UDPConnector tcpConnector, TCPConnector udpConnector)
    {
        this(tcpConnector, udpConnector, null);
    }

    public StandardUserAgentClient(UDPConnector tcpConnector, TCPConnector udpConnector, Settings settings)
    {
        super(tcpConnector, udpConnector, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(Keys.DA_ADDRESSES_KEY))
            this.directoryAgentAddresses = settings.get(Keys.DA_ADDRESSES_KEY);
    }

    public String[] getDirectoryAgentAddresses()
    {
        return directoryAgentAddresses;
    }

    public void setDirectoryAgentAddresses(String[] directoryAgentAddresses)
    {
        this.directoryAgentAddresses = directoryAgentAddresses;
    }

    public void init()
    {
        if (directoryAgentAddresses != null)
        {
            for (String daAddress : directoryAgentAddresses) directoryAgents.add(DirectoryAgentInfo.from(daAddress));
        }
    }

    protected List<DirectoryAgentInfo> findDirectoryAgents(Scopes scopes, Filter filter)
    {
        List<DirectoryAgentInfo> matchingDAs = directoryAgents.match(scopes, filter);
        if (matchingDAs.isEmpty())
            return discoverDirectoryAgents(scopes, filter);
        else
            return matchingDAs;
    }

    public static class Factory implements UserAgentClient.Factory
    {
        /**
         * Creates, configures and initializes a new StandardUserAgentClient instance.
         *
         * @param settings the configuration settings
         * @return a new configured and initialized instance of StandardUserAgentClient
         * @see StandardUserAgentClient#newInstance(Settings)
         */
        public UserAgentClient newUserAgentClient(Settings settings)
        {
            StandardUserAgentClient userAgentClient = newInstance(settings);
            userAgentClient.init();
            return userAgentClient;
        }
    }
}
