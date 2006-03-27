/*
 * Copyright 2006 the original author or authors
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
package org.livetribe.slp.api.ua;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.SAAdvert;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.ua.UserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardUserAgent extends StandardAgent implements UserAgent
{
    private UserAgentManager manager;
//    private final DirectoryAgentCache daCache = new DirectoryAgentCache();

    public void setUserAgentManager(UserAgentManager manager)
    {
        this.manager = manager;
    }

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
//        setDiscoveryStartWaitBound(configuration.getDADiscoveryStartWaitBound());
//        setDiscoveryPeriod(configuration.getDADiscoveryPeriod());
        if (manager != null) manager.setConfiguration(configuration);
    }

    protected void doStart() throws IOException
    {
        // TODO: add listeners to interpret unsolicited DAAdverts and SAAdverts
        // TODO: add a Timer to rediscover DAs every discoveryPeriod
        manager.start();
    }

    protected void doStop() throws IOException
    {
        manager.stop();
    }

    public List findServices(ServiceType serviceType, String[] scopes, String filter) throws IOException, ServiceLocationException
    {
        List addresses = findDirectoryAgents(scopes);

        List result = new ArrayList();
        for (int i = 0; i < addresses.size(); ++i)
        {
            InetAddress address = (InetAddress)addresses.get(i);
            SrvRply srvRply = manager.unicastSrvRqst(address, serviceType, scopes, filter);
            URLEntry[] entries = srvRply.getURLEntries();
            for (int j = 0; j < entries.length; ++j)
            {
                URLEntry entry = entries[j];
                result.add(entry.toServiceURL());
            }
        }
        return result;
    }

    protected List findDirectoryAgents(String[] scopes) throws IOException, ServiceLocationException
    {
        List addresses = getCachedDirectoryAgents(scopes);
        if (addresses.isEmpty()) addresses = discoverDirectoryAgents(scopes);
        if (addresses.isEmpty()) addresses = discoverServiceAgents(scopes);
        if (addresses.isEmpty()) return Collections.emptyList();
        cacheDirectoryAgents(addresses);
        return addresses;
    }

    private List getCachedDirectoryAgents(String[] scopes)
    {
//        return daCache.getDirectoryAgents(scopes);
        return Collections.emptyList();
    }

    private void cacheDirectoryAgents(List addresses)
    {
    }

    private List discoverDirectoryAgents(String[] scopes) throws IOException, ServiceLocationException
    {
        List result = new ArrayList();
        DAAdvert[] daAdverts = manager.multicastDASrvRqst(scopes, null, -1);
        for (int i = 0; i < daAdverts.length; ++i)
        {
            DAAdvert daAdvert = daAdverts[i];
            result.add(InetAddress.getByName(daAdvert.getResponder()));
        }
        return result;
    }

    private List discoverServiceAgents(String[] scopes) throws IOException, ServiceLocationException
    {
        List result = new ArrayList();
        SAAdvert[] saAdverts = manager.multicastSASrvRqst(scopes, null, -1);
        for (int i = 0; i < saAdverts.length; ++i)
        {
            SAAdvert saAdvert = saAdverts[i];
            result.add(InetAddress.getByName(saAdvert.getResponder()));
        }
        return result;
    }
}
