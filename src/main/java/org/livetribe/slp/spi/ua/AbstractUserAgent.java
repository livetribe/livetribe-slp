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
package org.livetribe.slp.spi.ua;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.settings.Keys;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.MulticastDASrvRqstPerformer;
import org.livetribe.slp.spi.MulticastSrvRqstPerformer;
import org.livetribe.slp.spi.TCPSrvRqstPerformer;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.filter.FilterParser;
import org.livetribe.slp.spi.msg.AttributeListExtension;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.LanguageExtension;
import org.livetribe.slp.spi.msg.ScopeListExtension;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.NetUtils;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * TODO: how to handle scopes ?
 *
 * @version $Revision$ $Date$
 */
public abstract class AbstractUserAgent implements IUserAgent
{
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private final MulticastDASrvRqstPerformer multicastDASrvRqst;
    private final TCPSrvRqstPerformer tcpSrvRqst;
    private final MulticastSrvRqstPerformer multicastSrvRqst;
    private int port = Defaults.get(Keys.PORT_KEY);

    public AbstractUserAgent(UDPConnector udpConnector, TCPConnector tcpConnector, Settings settings)
    {
        this.multicastSrvRqst = new MulticastSrvRqstPerformer(udpConnector, settings);
        this.tcpSrvRqst = new TCPSrvRqstPerformer(tcpConnector, settings);
        this.multicastDASrvRqst = new MulticastDASrvRqstPerformer(udpConnector, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(Keys.PORT_KEY)) setPort(settings.get(Keys.PORT_KEY));
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public List<ServiceInfo> findServices(ServiceType serviceType, String language, Scopes scopes, String filterString)
    {
        Filter filter = new FilterParser().parse(filterString);
        List<ServiceInfo> result = new ArrayList<ServiceInfo>();

        List<DirectoryAgentInfo> directoryAgents = findDirectoryAgents(scopes, null);
        if (!directoryAgents.isEmpty())
        {
            for (DirectoryAgentInfo directoryAgent : directoryAgents)
            {
                InetSocketAddress address = new InetSocketAddress(NetUtils.getByName(directoryAgent.getHostAddress()), directoryAgent.getPort(port));
                SrvRply srvRply = tcpSrvRqst.perform(address, serviceType, language, scopes, filter);
                result.addAll(srvRplyToServiceInfos(srvRply, scopes));
            }
        }
        else
        {
            List<SrvRply> srvRplys = multicastSrvRqst.perform(serviceType, scopes, filter, language);
            for (SrvRply srvRply : srvRplys)
            {
                result.addAll(srvRplyToServiceInfos(srvRply, scopes));
            }
        }

        return result;
    }

    protected abstract List<DirectoryAgentInfo> findDirectoryAgents(Scopes scopes, Filter filter);

    protected List<DirectoryAgentInfo> discoverDirectoryAgents(Scopes scopes, Filter filter)
    {
        List<DirectoryAgentInfo> result = new ArrayList<DirectoryAgentInfo>();
        List<DAAdvert> daAdverts = multicastDASrvRqst.perform(scopes, filter, null);
        for (DAAdvert daAdvert : daAdverts) result.add(DirectoryAgentInfo.from(daAdvert));
        if (logger.isLoggable(Level.FINE)) logger.fine("UserAgent " + this + " discovered DAs: " + result);
        return result;
    }

    protected List<ServiceInfo> srvRplyToServiceInfos(SrvRply srvRply, Scopes scopes)
    {
        List<ServiceInfo> result = new ArrayList<ServiceInfo>();
        List<LanguageExtension> languageExtensions = LanguageExtension.findAll(srvRply.getExtensions());
        List<ScopeListExtension> scopesExtensions = ScopeListExtension.findAll(srvRply.getExtensions());
        List<AttributeListExtension> attributesExtensions = AttributeListExtension.findAll(srvRply.getExtensions());
        for (int i = 0; i < srvRply.getURLEntries().size(); ++i)
        {
            URLEntry entry = srvRply.getURLEntries().get(i);
            ServiceURL serviceURL = entry.toServiceURL();
            String url = serviceURL.getURL();

            String language = srvRply.getLanguage();
            if (!languageExtensions.isEmpty())
            {
                LanguageExtension languageExtension = languageExtensions.get(i);
                if (languageExtension.getURL().equals(url)) language = languageExtension.getLanguage();
            }

            Scopes serviceScopes = scopes;
            if (!scopesExtensions.isEmpty())
            {
                ScopeListExtension scopesExtension = scopesExtensions.get(i);
                if (scopesExtension.getURL().equals(url)) serviceScopes = scopesExtension.getScopes();
            }

            Attributes serviceAttributes = null;
            if (!attributesExtensions.isEmpty())
            {
                AttributeListExtension attributesExtension = attributesExtensions.get(i);
                if (attributesExtension.getURL().equals(url)) serviceAttributes = attributesExtension.getAttributes();
            }

            ServiceInfo service = new ServiceInfo(serviceURL, language, serviceScopes, serviceAttributes);
            result.add(service);
        }
        return result;
    }
}
