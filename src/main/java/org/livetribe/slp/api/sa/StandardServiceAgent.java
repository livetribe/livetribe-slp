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
package org.livetribe.slp.api.sa;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;

import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.spi.sa.ServiceAgentManager;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.ServiceLocationException;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgent extends StandardAgent implements ServiceAgent
{
    private ServiceAgentManager manager;

    public void setServiceAgentManager(ServiceAgentManager manager)
    {
        this.manager = manager;
    }

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        if (manager != null) manager.setConfiguration(configuration);
    }

    protected void doStart() throws IOException
    {
        // TODO: add listener to interpret unsolicited DAAdverts
        // TODO: add a Timer to rediscover DAs every discoveryPeriod
        manager.start();
    }

    protected void doStop() throws IOException
    {
        manager.stop();
    }

    public void registerService(ServiceType serviceType, ServiceURL serviceURL, String[] scopes, String[] attributes, String language) throws IOException, ServiceLocationException
    {
        List addresses = findDirectoryAgents(scopes);

        for (int i = 0; i < addresses.size(); ++i)
        {
            InetAddress address = (InetAddress)addresses.get(i);
            manager.unicastSrvReg(address, serviceType, serviceURL, true, scopes, attributes, language);
        }
    }

    protected List findDirectoryAgents(String[] scopes) throws IOException, ServiceLocationException
    {
        List addresses = getCachedDirectoryAgents(scopes);
        if (addresses.isEmpty()) addresses = discoverDirectoryAgents(scopes);
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

    private List discoverDirectoryAgents(String[] scopes) throws IOException
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
}
