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
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.spi.da.DirectoryAgentCache;
import org.livetribe.slp.spi.da.DirectoryAgentInfo;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SAAdvert;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;
import org.livetribe.slp.spi.ua.StandardUserAgentManager;
import org.livetribe.slp.spi.ua.UserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardUserAgent extends StandardAgent implements UserAgent
{
    private int discoveryStartWaitBound;
    private long discoveryPeriod;
    private UserAgentManager manager;
    private MessageListener multicastListener;
    private final DirectoryAgentCache daCache = new DirectoryAgentCache();
    private ScheduledExecutorService scheduledExecutorService;

    public void setUserAgentManager(UserAgentManager manager)
    {
        this.manager = manager;
    }

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        setDiscoveryStartWaitBound(configuration.getDADiscoveryStartWaitBound());
        setDiscoveryPeriod(configuration.getDADiscoveryPeriod());
        if (manager != null) manager.setConfiguration(configuration);
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService)
    {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public int getDiscoveryStartWaitBound()
    {
        return discoveryStartWaitBound;
    }

    /**
     * Sets the bound (in seconds) to the initial random delay this UserAgent waits
     * before attempting to discover DirectoryAgents
     */
    public void setDiscoveryStartWaitBound(int discoveryStartWaitBound)
    {
        this.discoveryStartWaitBound = discoveryStartWaitBound;
    }

    public long getDiscoveryPeriod()
    {
        return discoveryPeriod;
    }

    public void setDiscoveryPeriod(long discoveryPeriod)
    {
        this.discoveryPeriod = discoveryPeriod;
    }

    protected void doStart() throws IOException
    {
        if (manager == null)
        {
            manager = createUserAgentManager();
            manager.setConfiguration(getConfiguration());
        }
        manager.start();

        multicastListener = new MulticastListener();
        manager.addMessageListener(multicastListener, true);

        if (scheduledExecutorService == null) scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        long delay = new Random(System.currentTimeMillis()).nextInt(getDiscoveryStartWaitBound() + 1) * 1000L;
        scheduledExecutorService.scheduleWithFixedDelay(new DirectoryAgentDiscovery(), delay, getDiscoveryPeriod() * 1000L, TimeUnit.MILLISECONDS);
    }

    protected UserAgentManager createUserAgentManager()
    {
        return new StandardUserAgentManager();
    }

    protected void doStop() throws IOException
    {
        if (scheduledExecutorService != null)
        {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }

        manager.removeMessageListener(multicastListener, true);
        manager.stop();
    }

    public List findServices(ServiceType serviceType, String[] scopes, String filter, String language) throws IOException, ServiceLocationException
    {
        List result = new ArrayList();

        List das = findDirectoryAgents(scopes);
        if (!das.isEmpty())
        {
            for (int i = 0; i < das.size(); ++i)
            {
                DirectoryAgentInfo info = (DirectoryAgentInfo)das.get(i);
                InetAddress address = InetAddress.getByName(info.getHost());
                SrvRply srvRply = manager.unicastSrvRqst(address, serviceType, scopes, filter, language);
                URLEntry[] entries = srvRply.getURLEntries();
                for (int j = 0; j < entries.length; ++j)
                {
                    URLEntry entry = entries[j];
                    result.add(entry.toServiceURL());
                }
            }
        }
        else
        {
            List sas = findServiceAgents(scopes);
            for (int i = 0; i < sas.size(); ++i)
            {
                ServiceAgentInfo info = (ServiceAgentInfo)sas.get(i);
                InetAddress address = InetAddress.getByName(info.getHost());
                SrvRply srvRply = manager.unicastSrvRqst(address, serviceType, scopes, filter, language);
                URLEntry[] entries = srvRply.getURLEntries();
                for (int j = 0; j < entries.length; ++j)
                {
                    URLEntry entry = entries[j];
                    result.add(entry.toServiceURL());
                }
            }
        }
        return result;
    }

    protected List findDirectoryAgents(String[] scopes) throws IOException, ServiceLocationException
    {
        List das = getCachedDirectoryAgents(scopes);
        if (das.isEmpty())
        {
            das = discoverDirectoryAgents(scopes);
            cacheDirectoryAgents(das);
        }
        return das;
    }

    protected List getCachedDirectoryAgents(String[] scopes)
    {
        return daCache.getByScopes(scopes);
    }

    private void cacheDirectoryAgents(List infos)
    {
        daCache.addAll(infos);
    }

    private boolean cacheDirectoryAgent(DirectoryAgentInfo info)
    {
        return daCache.add(info);
    }

    private boolean uncacheDirectoryAgent(DirectoryAgentInfo info)
    {
        return daCache.remove(info);
    }

    protected List discoverDirectoryAgents(String[] scopes) throws IOException
    {
        List result = new ArrayList();
        DAAdvert[] daAdverts = manager.multicastDASrvRqst(scopes, null, null, -1);
        for (int i = 0; i < daAdverts.length; ++i)
        {
            DAAdvert daAdvert = daAdverts[i];
            DirectoryAgentInfo info = DirectoryAgentInfo.from(daAdvert);
            result.add(info);
        }
        if (logger.isLoggable(Level.FINE)) logger.fine("UserAgent " + this + " discovered DAs: " + result);
        return result;
    }

    protected List findServiceAgents(String[] scopes) throws IOException, ServiceLocationException
    {
        return discoverServiceAgents(scopes);
    }

    private List discoverServiceAgents(String[] scopes) throws IOException
    {
        List result = new ArrayList();
        SAAdvert[] saAdverts = manager.multicastSASrvRqst(scopes, null, null, -1);
        for (int i = 0; i < saAdverts.length; ++i)
        {
            SAAdvert saAdvert = saAdverts[i];
            ServiceAgentInfo info = ServiceAgentInfo.from(saAdvert);
            result.add(info);
        }
        if (logger.isLoggable(Level.FINE)) logger.fine("UserAgent " + this + " discovered SAs: " + result);
        return result;
    }

    protected void handleMulticastDAAdvert(DAAdvert message, InetSocketAddress address)
    {
        List scopesList = Arrays.asList(getScopes());
        List messageScopesList = Arrays.asList(message.getScopes());
        if (!scopesList.contains(DEFAULT_SCOPE) && Collections.disjoint(scopesList, messageScopesList))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("UserAgent " + this + " dropping message " + message + ": no scopes match among UA scopes " + scopesList + " and message scopes " + messageScopesList);
            return;
        }

        DirectoryAgentInfo info = DirectoryAgentInfo.from(message);
        if (message.getBootTime() == 0L)
        {
            boolean removed = uncacheDirectoryAgent(info);
            // TODO
//            if (removed) notifyListeners();
        }
        else
        {
            boolean added = cacheDirectoryAgent(info);
            // TODO
//            if (added) notifyListeners();
        }
    }

    /**
     * UserAgents listen for multicast messages that may arrive.
     * They are interested in:
     * <ul>
     * <li>DAAdverts, from DAs that boot or shutdown</li>
     * </ul>
     */
    private class MulticastListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            InetSocketAddress address = event.getSocketAddress();
            try
            {
                Message message = Message.deserialize(event.getMessageBytes());
                if (logger.isLoggable(Level.FINEST))
                    logger.finest("UserAgent multicast message listener received message " + message);

                if (!message.isMulticast())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("UserAgent " + this + " dropping message " + message + ": expected multicast flag set");
                    return;
                }

                switch (message.getMessageType())
                {
                    case Message.DA_ADVERT_TYPE:
                        handleMulticastDAAdvert((DAAdvert)message, address);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("UserAgent " + this + " dropping multicast message " + message + ": not handled by UserAgents");
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "UserAgent " + this + " received bad multicast message from: " + address + ", ignoring", x);
            }
        }
    }

    private class DirectoryAgentDiscovery implements Runnable
    {
        public void run()
        {
            if (logger.isLoggable(Level.FINE)) logger.fine("UserAgent " + this + " executing periodic discovery of DAs");
            try
            {
                List das = discoverDirectoryAgents(getScopes());
                cacheDirectoryAgents(das);
            }
            catch (IOException x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Could not discover DAs", x);
            }
        }
    }
}
