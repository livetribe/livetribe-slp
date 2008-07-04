/**
 *
 * Copyright 2008 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.builder;

import java.util.concurrent.ScheduledExecutorService;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.sa.StandardServiceAgent;
import org.livetribe.slp.srv.net.MulticastSocketUDPConnector;
import org.livetribe.slp.srv.net.MulticastSocketUDPConnectorServer;
import org.livetribe.slp.srv.net.SocketTCPConnector;
import org.livetribe.slp.srv.net.TCPConnector;
import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.srv.net.UDPConnectorServer;


/**
 * @version $Revision$ $Date$
 */
public class StandardServiceAgentBuilder implements Builder<StandardServiceAgent>
{
    private UDPConnector udpConnector;
    private TCPConnector tcpConnector;
    private UDPConnectorServer udpConnectorServer;
    private ScheduledExecutorService scheduledExecutorService;
    private String multicastAddress;
    private String[] directoryAgentAddresses;
    private String[] addresses;
    private Integer port;
    private Scopes scopes;
    private Attributes attributes;
    private String language;
    private Boolean periodicServiceRenewalEnabled;

    public UDPConnector getUdpConnector()
    {
        return udpConnector;
    }

    public void setUdpConnector(UDPConnector udpConnector)
    {
        this.udpConnector = udpConnector;
    }

    public TCPConnector getTcpConnector()
    {
        return tcpConnector;
    }

    public void setTcpConnector(TCPConnector tcpConnector)
    {
        this.tcpConnector = tcpConnector;
    }

    public UDPConnectorServer getUdpConnectorServer()
    {
        return udpConnectorServer;
    }

    public void setUdpConnectorServer(UDPConnectorServer udpConnectorServer)
    {
        this.udpConnectorServer = udpConnectorServer;
    }

    public ScheduledExecutorService getScheduledExecutorService()
    {
        return scheduledExecutorService;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService)
    {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public String getMulticastAddress()
    {
        return multicastAddress;
    }

    public void setMulticastAddress(String multicastAddress)
    {
        this.multicastAddress = multicastAddress;
    }

    public String[] getDirectoryAgentAddresses()
    {
        return directoryAgentAddresses;
    }

    public void setDirectoryAgentAddresses(String[] directoryAgentAddresses)
    {
        this.directoryAgentAddresses = directoryAgentAddresses;
    }

    public String[] getAddresses()
    {
        return addresses;
    }

    public void setAddresses(String[] addresses)
    {
        this.addresses = addresses;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public Scopes getScopes()
    {
        return scopes;
    }

    public void setScopes(Scopes scopes)
    {
        this.scopes = scopes;
    }

    public Attributes getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Attributes attributes)
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

    public Boolean getPeriodicServiceRenewalEnabled()
    {
        return periodicServiceRenewalEnabled;
    }

    public void setPeriodicServiceRenewalEnabled(Boolean periodicServiceRenewalEnabled)
    {
        this.periodicServiceRenewalEnabled = periodicServiceRenewalEnabled;
    }

    public StandardServiceAgent build()
    {
        if (udpConnector == null)
        {
            udpConnector = new MulticastSocketUDPConnector(null);

            if (multicastAddress != null) ((MulticastSocketUDPConnector) udpConnectorServer).setMulticastAddress(multicastAddress);
        }
        if (tcpConnector == null) tcpConnector = new SocketTCPConnector(null);
        if (udpConnectorServer == null)
        {
            udpConnectorServer = new MulticastSocketUDPConnectorServer(null, port == null ? 427 : port);

            if (multicastAddress != null) ((MulticastSocketUDPConnectorServer) udpConnectorServer).setMulticastAddress(multicastAddress);
            if (addresses != null) ((MulticastSocketUDPConnectorServer) udpConnectorServer).setAddresses(addresses);
        }

        StandardServiceAgent serviceAgent = new StandardServiceAgent(udpConnector, tcpConnector, udpConnectorServer, null);

        if (scheduledExecutorService != null) serviceAgent.setScheduledExecutorService(scheduledExecutorService);
        if (directoryAgentAddresses != null) serviceAgent.setDirectoryAgentAddresses(directoryAgentAddresses);
        if (addresses != null) serviceAgent.setAddresses(addresses);
        if (port != null) serviceAgent.setPort(port);
        if (scopes != null) serviceAgent.setScopes(scopes);
        if (attributes != null) serviceAgent.setAttributes(attributes);
        if (language != null) serviceAgent.setLanguage(language);
        if (periodicServiceRenewalEnabled != null) serviceAgent.setPeriodicServiceRenewalEnabled(periodicServiceRenewalEnabled);

        return serviceAgent;
    }

    public void clear()
    {
        udpConnector = null;
        tcpConnector = null;
        udpConnectorServer = null;
        scheduledExecutorService = null;
        multicastAddress = null;
        directoryAgentAddresses = null;
        addresses = null;
        port = null;
        scopes = null;
        attributes = null;
        language = null;
        periodicServiceRenewalEnabled = null;
    }
}
