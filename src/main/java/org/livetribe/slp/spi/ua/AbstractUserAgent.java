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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.MulticastDASrvRqstPerformer;
import org.livetribe.slp.spi.filter.Filter;
import org.livetribe.slp.spi.filter.FilterParser;
import org.livetribe.slp.spi.msg.AttrRply;
import org.livetribe.slp.spi.msg.AttributeListExtension;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.LanguageExtension;
import org.livetribe.slp.spi.msg.ScopeListExtension;
import org.livetribe.slp.spi.msg.SrvRply;
import org.livetribe.slp.spi.msg.SrvTypeRply;
import org.livetribe.slp.spi.msg.URLEntry;
import org.livetribe.slp.spi.net.NetUtils;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * @version $Revision$ $Date$
 */
public abstract class AbstractUserAgent implements IUserAgent
{
    protected final Logger logger = Logger.getLogger(getClass().getName());
    private final MulticastDASrvRqstPerformer multicastDASrvRqst;
    private final UnicastSrvRqstPerformer unicastSrvRqst;
    private final MulticastSrvRqstPerformer multicastSrvRqst;
    private final UnicastAttrRqstPerformer unicastAttrRqst;
    private final MulticastAttrRqstPerformer multicastAttrRqst;
    private final UnicastSrvTypeRqstPerformer unicastSrvTypeRqst;
    private final MulticastSrvTypeRqstPerformer multicastSrvTypeRqst;
    private int port = Defaults.get(PORT_KEY);
    private boolean preferTCP = Defaults.get(UA_UNICAST_PREFER_TCP);

    public AbstractUserAgent(UDPConnector udpConnector, TCPConnector tcpConnector, Settings settings)
    {
        this.multicastSrvRqst = new MulticastSrvRqstPerformer(udpConnector, settings);
        this.unicastSrvRqst = new UnicastSrvRqstPerformer(udpConnector, tcpConnector, settings);
        this.multicastDASrvRqst = new MulticastDASrvRqstPerformer(udpConnector, settings);
        this.unicastAttrRqst = new UnicastAttrRqstPerformer(udpConnector, tcpConnector, settings);
        this.multicastAttrRqst = new MulticastAttrRqstPerformer(udpConnector, settings);
        this.unicastSrvTypeRqst = new UnicastSrvTypeRqstPerformer(udpConnector, tcpConnector, settings);
        this.multicastSrvTypeRqst = new MulticastSrvTypeRqstPerformer(udpConnector, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
        if (settings.containsKey(PORT_KEY)) this.port = settings.get(PORT_KEY);
        if (settings.containsKey(UA_UNICAST_PREFER_TCP)) this.preferTCP = settings.get(UA_UNICAST_PREFER_TCP);
    }

    public int getPort()
    {
        return port;
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
                InetSocketAddress address = resolveDirectoryAgentAddress(directoryAgent);
                SrvRply srvRply = unicastSrvRqst.perform(address, preferTCP, serviceType, language, scopes, filter);
                if (srvRply.getSLPError() == SLPError.NO_ERROR) result.addAll(srvRplyToServiceInfos(srvRply, scopes));
            }
        }
        else
        {
            List<SrvRply> srvRplys = multicastSrvRqst.perform(serviceType, language, scopes, filter);
            for (SrvRply srvRply : srvRplys)
                if (srvRply.getSLPError() == SLPError.NO_ERROR) result.addAll(srvRplyToServiceInfos(srvRply, scopes));
        }

        return result;
    }

    public Attributes findAttributes(ServiceType serviceType, String language, Scopes scopes, Attributes tags)
    {
        return findAttributes(serviceType.asString(), language, scopes, tags);
    }

    public Attributes findAttributes(ServiceURL serviceURL, String language, Scopes scopes, Attributes tags)
    {
        return findAttributes(serviceURL.getURL(), language, scopes, tags);
    }

    protected Attributes findAttributes(String url, String language, Scopes scopes, Attributes tags)
    {
        Attributes result = Attributes.NONE;

        List<DirectoryAgentInfo> directoryAgents = findDirectoryAgents(scopes, null);
        if (!directoryAgents.isEmpty())
        {
            for (DirectoryAgentInfo directoryAgent : directoryAgents)
            {
                InetSocketAddress address = resolveDirectoryAgentAddress(directoryAgent);
                AttrRply attrRply = unicastAttrRqst.perform(address, preferTCP, url, language, scopes, tags);
                if (attrRply.getSLPError() == SLPError.NO_ERROR) result = result.merge(attrRply.getAttributes());
            }
        }
        else
        {
            List<AttrRply> attrRplys = multicastAttrRqst.perform(url, language, scopes, tags);
            for (AttrRply attrRply : attrRplys) result = result.merge(attrRply.getAttributes());
        }

        return result;
    }

    public List<ServiceType> findServiceTypes(String namingAuthority, Scopes scopes)
    {
        List<ServiceType> result = new ArrayList<ServiceType>();

        List<DirectoryAgentInfo> directoryAgents = findDirectoryAgents(scopes, null);
        if (!directoryAgents.isEmpty())
        {
            for (DirectoryAgentInfo directoryAgent : directoryAgents)
            {
                InetSocketAddress address = resolveDirectoryAgentAddress(directoryAgent);
                SrvTypeRply srvTypeRply = unicastSrvTypeRqst.perform(address, preferTCP, namingAuthority, scopes);
                if (srvTypeRply.getSLPError() == SLPError.NO_ERROR) result.addAll(srvTypeRply.getServiceTypes());
            }
        }
        else
        {
            List<SrvTypeRply> srvTypeRplys = multicastSrvTypeRqst.perform(namingAuthority, scopes);
            for (SrvTypeRply srvTypeRply : srvTypeRplys)
                if (srvTypeRply.getSLPError() == SLPError.NO_ERROR) result.addAll(srvTypeRply.getServiceTypes());
        }

        return result;
    }

    protected InetSocketAddress resolveDirectoryAgentAddress(DirectoryAgentInfo directoryAgent)
    {
        InetAddress daAddress = NetUtils.getByName(directoryAgent.getHostAddress());
        int daPort = directoryAgent.getUnicastPort(preferTCP, port);
        return new InetSocketAddress(daAddress, daPort);
    }

    protected abstract List<DirectoryAgentInfo> findDirectoryAgents(Scopes scopes, Filter filter);

    protected List<DirectoryAgentInfo> discoverDirectoryAgents(Scopes scopes, Filter filter)
    {
        List<DirectoryAgentInfo> result = new ArrayList<DirectoryAgentInfo>();
        List<DAAdvert> daAdverts = multicastDASrvRqst.perform(null, scopes, filter);
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
