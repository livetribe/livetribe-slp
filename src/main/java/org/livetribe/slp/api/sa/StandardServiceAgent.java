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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.StandardAgent;
import org.livetribe.slp.spi.da.DirectoryAgentCache;
import org.livetribe.slp.spi.da.DirectoryAgentInfo;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;
import org.livetribe.slp.spi.sa.ServiceAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgent extends StandardAgent implements ServiceAgent
{
    private int discoveryStartWaitBound;
    private long discoveryPeriod;
    private ServiceType serviceType;
    private ServiceURL serviceURL;
    private String[] attributes;
    private String language;
    private ServiceAgentManager manager;
    private MessageListener multicastListener;
    private final DirectoryAgentCache daCache = new DirectoryAgentCache();
    private Timer timer;

    public void setServiceAgentManager(ServiceAgentManager manager)
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

    public int getDiscoveryStartWaitBound()
    {
        return discoveryStartWaitBound;
    }

    /**
     * Sets the bound (in seconds) to the initial random delay this ServiceAgent waits
     * before attempting to discover DirectoryAgents
     * @param discoveryStartWaitBound
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

    public ServiceType getServiceType()
    {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType)
    {
        this.serviceType = serviceType;
    }

    public ServiceURL getServiceURL()
    {
        return serviceURL;
    }

    public void setServiceURL(ServiceURL serviceURL)
    {
        this.serviceURL = serviceURL;
    }

    public String[] getAttributes()
    {
        return attributes;
    }

    public void setAttributes(String[] attributes)
    {
        this.attributes = attributes;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    protected void doStart() throws Exception
    {
        if (getServiceURL() == null) throw new IllegalStateException("Could not start ServiceAgent " + this + ", its ServiceURL has not been set");

        multicastListener = new MulticastListener();
        manager.addMessageListener(multicastListener, true);
        manager.start();

        timer = new Timer(true);
        long delay = new Random(System.currentTimeMillis()).nextInt(getDiscoveryStartWaitBound() + 1) * 1000L;
        timer.schedule(new DirectoryAgentDiscovery(), delay, getDiscoveryPeriod() * 1000L);

        register();
    }

    protected void doStop() throws IOException
    {
        timer.cancel();

        manager.stop();
        manager.removeMessageListener(multicastListener, true);
    }

    public void register() throws IOException, ServiceLocationException
    {
        List das = findDirectoryAgents(getScopes());

        for (int i = 0; i < das.size(); ++i)
        {
            DirectoryAgentInfo info = (DirectoryAgentInfo)das.get(i);
            register(info);
        }
    }

    private void register(DirectoryAgentInfo da) throws IOException, ServiceLocationException
    {
        ServiceURL su = getServiceURL();

        ServiceType st = getServiceType();
        if (st == null) st = su.getServiceType();

        InetAddress address = InetAddress.getByName(da.getHost());
        ServiceAgentInfo sa = new ServiceAgentInfo(st, su, getScopes(), getAttributes(), getLanguage(), true);
        SrvAck srvAck = manager.unicastSrvReg(address, sa);
        int errorCode = srvAck.getErrorCode();
        if (errorCode != 0) throw new ServiceLocationException("Could not register service " + serviceURL + " to DirectoryAgent " + address, errorCode);
        if (logger.isLoggable(Level.FINE)) logger.fine("Registered service " + serviceURL + " to DirectoryAgent " + address);

        long renewalPeriod = calculateRenewalPeriod(sa);
        long renewalDelay = calculateRenewalDelay(sa);
        if (renewalPeriod > 0) timer.schedule(new RegistrationRenewal(da), renewalDelay, renewalPeriod);
    }

    private long calculateRenewalPeriod(ServiceAgentInfo info)
    {
        long lifetime = info.getServiceURL().getLifetime();
        if (lifetime < ServiceURL.LIFETIME_NONE) lifetime = ServiceURL.LIFETIME_MAXIMUM;
        // Convert from seconds to milliseconds
        lifetime *= 1000L;
        return lifetime;
    }

    private long calculateRenewalDelay(ServiceAgentInfo info)
    {
        long lifetime = calculateRenewalPeriod(info);
        // Renew when 80% of the lifetime is passed
        return lifetime - (lifetime >> 3);
    }

    public void deregister() throws IOException, ServiceLocationException
    {
        List das = findDirectoryAgents(getScopes());

        for (int i = 0; i < das.size(); ++i)
        {
            DirectoryAgentInfo info = (DirectoryAgentInfo)das.get(i);
            deregister(info);
        }
    }

    private void deregister(DirectoryAgentInfo da) throws IOException, ServiceLocationException
    {
        ServiceURL su = getServiceURL();

        ServiceType st = getServiceType();
        if (st == null) st = su.getServiceType();

        InetAddress address = InetAddress.getByName(da.getHost());
        ServiceAgentInfo sa = new ServiceAgentInfo(st, su, getScopes(), null, null, true);

        SrvAck srvAck = manager.unicastSrvDeReg(address, sa);
        int errorCode = srvAck.getErrorCode();
        if (errorCode != 0) throw new ServiceLocationException("Could not deregister service " + serviceURL + " from DirectoryAgent " + address, errorCode);
        if (logger.isLoggable(Level.FINE)) logger.fine("Deregistered service " + serviceURL + " from DirectoryAgent " + address);
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

    private boolean cacheDirectoryAgent(DirectoryAgentInfo info)
    {
        return daCache.add(info);
    }

    private void cacheDirectoryAgents(List infos)
    {
        daCache.addAll(infos);
    }

    private void uncacheDirectoryAgent(DirectoryAgentInfo info)
    {
        daCache.remove(info);
    }

    protected List discoverDirectoryAgents(String[] scopes) throws IOException
    {
        List result = new ArrayList();
        DAAdvert[] daAdverts = manager.multicastDASrvRqst(scopes, null, -1);
        for (int i = 0; i < daAdverts.length; ++i)
        {
            DAAdvert daAdvert = daAdverts[i];
            DirectoryAgentInfo info = DirectoryAgentInfo.from(daAdvert);
            result.add(info);
        }
        return result;
    }

    protected void handleMulticastDAAdvert(DAAdvert message, InetSocketAddress address)
    {
        List scopesList = Arrays.asList(getScopes());
        List messageScopesList = Arrays.asList(message.getScopes());
        if (!scopesList.contains(DEFAULT_SCOPE) && Collections.disjoint(scopesList, messageScopesList))
        {
            if (logger.isLoggable(Level.FINE))
                logger.fine("ServiceAgent " + this + " dropping message " + message + ": no scopes match among SA scopes " + scopesList + " and message scopes " + messageScopesList);
            return;
        }

        DirectoryAgentInfo info = DirectoryAgentInfo.from(message);
        if (message.getBootTime() == 0L)
        {
            uncacheDirectoryAgent(info);
        }
        else
        {
            boolean isNew = cacheDirectoryAgent(info);
            if (isNew)
            {
                try
                {
                    // TODO: RFC 2608, 12.2.2 requires to wait some time before registering
                    register(info);
                }
                catch (IOException x)
                {
                    if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "ServiceAgent " + this + " dropping message " + message + ": could not register service to DA " + info, x);
                }
                catch (ServiceLocationException x)
                {
                    if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "ServiceAgent " + this + " dropping message " + message + ": could not register service to DA " + info, x);
                }
            }
        }
    }

    /**
     * ServiceAgents listen for multicast messages that may arrive.
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
                    logger.finest("ServiceAgent multicast message listener received message " + message);

                if (!message.isMulticast())
                {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("ServiceAgent " + this + " dropping message " + message + ": expected multicast flag set");
                    return;
                }

                switch (message.getMessageType())
                {
                    case Message.DA_ADVERT_TYPE:
                        handleMulticastDAAdvert((DAAdvert)message, address);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("ServiceAgent " + this + " dropping multicast message " + message + ": not handled by ServiceAgents");
                        break;
                }
            }
            catch (ServiceLocationException x)
            {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "ServiceAgent " + this + " received bad multicast message from: " + address + ", ignoring", x);
            }
        }
    }

    /**
     * ServiceAgent must refresh their registration with DAs before the lifetime specified
     * in the ServiceURL expires, otherwise the DA does not advertise them anymore.
     */
    private class RegistrationRenewal extends TimerTask
    {
        private final DirectoryAgentInfo da;

        public RegistrationRenewal(DirectoryAgentInfo da)
        {
            this.da = da;
        }

        public void run()
        {
            try
            {
                register(da);
            }
            catch (Exception x)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Could not renew service registration", x);
            }
        }
    }

    private class DirectoryAgentDiscovery extends TimerTask
    {
        public void run()
        {
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
