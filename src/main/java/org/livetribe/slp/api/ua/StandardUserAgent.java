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
import java.util.List;

import org.livetribe.slp.ServiceType;
import org.livetribe.slp.api.DirectoryAgentCache;
import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.spi.ua.UserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardUserAgent extends StandardAgent implements UserAgent
{
    private UserAgentManager manager;
    private final DirectoryAgentCache daCache = new DirectoryAgentCache();

    public void setUserAgentManager(UserAgentManager manager)
    {
        this.manager = manager;
    }

/*
    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        setDiscoveryStartWait(configuration.getDADiscoveryStartWait());
        setDiscoveryPeriod(configuration.getDADiscoveryPeriod());
    }
*/

    protected void doStart() throws IOException
    {
    }

    protected void doStop() throws IOException
    {
    }

    public List findServices(ServiceType serviceType, String[] scopes, String filter)
    {
//        List addresses = getCachedDirectoryAgents(scopes);
//        if (addresses.isEmpty()) addresses = discoverDirectoryAgents(scopes);
//        if (addresses.isEmpty()) addresses = discoverServiceAgents(scopes);
//        if (addresses.isEmpty()) return Collections.EMPTY_LIST;

        // 1. Use cached DAs, and call manager.unicastSrvRqst()
        // 2. No cached DAs, call manager.multicastDASrvRqst() to discover DAs; go back to 1.
        // 3. No DAs, call manager.multicastSrvRqst() to discover SAs; call manager.unicastSrvRqst()
        // 4. Extract the ServiceURL from SrvRplys and return them to user

        return null;
    }
/*
    private List getCachedDirectoryAgents(String[] scopes)
    {
        return daCache.getDirectoryAgents(scopes);
    }

    private List discoverDirectoryAgents(String[] scopes)
    {
        List result = new ArrayList();
        manager.multicastDASrvRqst(scopes, null, -1);
        return result;
    }

    private List discoverServiceAgents(String[] scopes)
    {
        List result = new ArrayList();
        manager.multicastSASrvRqst(scopes, null, -1);
        return result;
    }
*/
}
