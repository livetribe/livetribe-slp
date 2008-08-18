/*
 * Copyright 2006-2008 the original author or authors
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
package org.livetribe.slp.sa;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.settings.Factories;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.PropertiesSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.Server;
import org.livetribe.slp.spi.ServiceInfoCache;
import org.livetribe.slp.spi.TCPSrvAckPerformer;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvDeReg;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.net.MessageEvent;
import org.livetribe.slp.spi.net.MessageListener;
import org.livetribe.slp.spi.net.TCPConnector;
import org.livetribe.slp.spi.net.TCPConnectorServer;
import org.livetribe.slp.spi.net.UDPConnector;
import org.livetribe.slp.spi.net.UDPConnectorServer;
import org.livetribe.slp.spi.sa.AbstractServiceAgent;
import org.livetribe.slp.spi.sa.ServiceAgentInfo;

/**
 * Implementation of an SLP service agent standalone server that can be started as a service in a host.
 * <br />
 * Only one instance of this server can be started per each host, as it listens on the SLP TCP port.
 * In SLP, a service agent standalone server exposes the services of all applications in the host it resides,
 * so that each application does not need to start a {@link ServiceAgent}, but only uses a {@link ServiceAgentClient}
 * to contact the service agent standalone server.
 *
 * @version $Rev$ $Date$
 */
public class StandardServiceAgentServer extends AbstractServiceAgent
{
    /**
     * Main method to start this service agent.
     * <br />
     * It accepts a single program argument, the file path of the configuration file that overrides the
     * defaults for this service agent
     *
     * @param args the program arguments
     * @throws IOException in case the configuration file cannot be read
     */
    public static void main(String[] args) throws IOException
    {
        Settings settings = null;
        if (args.length > 0) settings = PropertiesSettings.from(new File(args[0]));
        Server server = newInstance(settings);
        server.start();
    }

    /**
     * @param settings the configuration settings that override the defaults
     * @return a new instance of this service agent
     */
    public static StandardServiceAgentServer newInstance(Settings settings)
    {
        UDPConnector udpConnector = Factories.<UDPConnector.Factory>newInstance(settings, UDP_CONNECTOR_FACTORY_KEY).newUDPConnector(settings);
        TCPConnector tcpConnector = Factories.<TCPConnector.Factory>newInstance(settings, TCP_CONNECTOR_FACTORY_KEY).newTCPConnector(settings);
        UDPConnectorServer udpConnectorServer = Factories.<UDPConnectorServer.Factory>newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY).newUDPConnectorServer(settings);
        TCPConnectorServer tcpConnectorServer = Factories.<TCPConnectorServer.Factory>newInstance(settings, TCP_CONNECTOR_SERVER_FACTORY_KEY).newTCPConnectorServer(settings);
        return new StandardServiceAgentServer(udpConnector, tcpConnector, udpConnectorServer, tcpConnectorServer, settings);
    }

    private final MessageListener tcpListener = new TCPMessageListener();
    private final TCPConnectorServer tcpConnectorServer;
    private final TCPSrvAckPerformer tcpSrvAck;

    /**
     * Creates a new StandardServiceAgentServer using the default settings
     *
     * @param udpConnector       the connector that handles udp traffic
     * @param tcpConnector       the connector that handles tcp traffic
     * @param udpConnectorServer the connector that listens for udp traffic
     * @param tcpConnectorServer the connector that listens for tcp traffic
     * @see org.livetribe.slp.settings.Defaults
     */
    public StandardServiceAgentServer(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, TCPConnectorServer tcpConnectorServer)
    {
        this(udpConnector, tcpConnector, udpConnectorServer, tcpConnectorServer, null);
    }

    /**
     * Creates a new StandardServiceAgentServer
     *
     * @param udpConnector       the connector that handles udp traffic
     * @param tcpConnector       the connector that handles tcp traffic
     * @param udpConnectorServer the connector that listens for udp traffic
     * @param tcpConnectorServer the connector that listens for tcp traffic
     * @param settings           the configuration settings that override the defaults
     */
    public StandardServiceAgentServer(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, TCPConnectorServer tcpConnectorServer, Settings settings)
    {
        super(udpConnector, tcpConnector, udpConnectorServer, settings);
        this.tcpConnectorServer = tcpConnectorServer;
        this.tcpSrvAck = new TCPSrvAckPerformer(tcpConnector, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
    }

    protected void doStart()
    {
        setAttributes(getAttributes().union(Attributes.from("(" + ServiceAgentInfo.TCP_PORT_TAG + "=" + getPort() + ")")));

        super.doStart();

        tcpConnectorServer.addMessageListener(tcpListener);
        tcpConnectorServer.start();

        Runtime.getRuntime().addShutdownHook(new Shutdown());
    }

    protected ServiceAgentInfo newServiceAgentInfo(String address, Scopes scopes, Attributes attributes, String language)
    {
        return ServiceAgentInfo.from(null, address, scopes, attributes, language);
    }

    protected void doStop()
    {
        super.doStop();
        tcpConnectorServer.removeMessageListener(tcpListener);
        tcpConnectorServer.stop();
    }

    /**
     * Handles a unicast TCP SrvReg message arrived to this service agent.
     * <br />
     * This service agent will reply with an acknowledge containing the result of the registration.
     *
     * @param srvReg the SrvReg message to handle
     * @param socket the socket connected to th client where to write the reply
     */
    protected void handleTCPSrvReg(SrvReg srvReg, Socket socket)
    {
        try
        {
            boolean update = srvReg.isUpdating();
            ServiceInfo givenService = ServiceInfo.from(srvReg);
            ServiceInfoCache.Result<ServiceInfo> result = cacheService(givenService, update);
            forwardRegistration(givenService, result.getPrevious(), result.getCurrent(), update);
            tcpSrvAck.perform(socket, srvReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            tcpSrvAck.perform(socket, srvReg, x.getSLPError().getCode());
        }
    }

    /**
     * Handles a unicast TCP SrvDeReg message arrived to this service agent.
     * <br />
     * This service agent will reply with an acknowledge containing the result of the deregistration.
     *
     * @param srvDeReg the SrvDeReg message to handle
     * @param socket   the socket connected to the client where to write the reply
     */
    protected void handleTCPSrvDeReg(SrvDeReg srvDeReg, Socket socket)
    {
        try
        {
            boolean update = srvDeReg.isUpdating();
            ServiceInfo givenService = ServiceInfo.from(srvDeReg);
            ServiceInfoCache.Result<ServiceInfo> result = uncacheService(givenService, update);
            forwardDeregistration(givenService, result.getPrevious(), result.getCurrent(), update);
            tcpSrvAck.perform(socket, srvDeReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            tcpSrvAck.perform(socket, srvDeReg, x.getSLPError().getCode());
        }
    }

    /**
     * ServiceAgents listen for tcp messages that may arrive.
     */
    private class TCPMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            Message message = event.getMessage();
            if (logger.isLoggable(Level.FINEST))
                logger.finest("ServiceAgent server message listener received message " + message);

            Socket socket = (Socket)event.getSource();
            if (socket.getInetAddress().isLoopbackAddress())
            {
                switch (message.getMessageType())
                {
                    case Message.SRV_REG_TYPE:
                        handleTCPSrvReg((SrvReg)message, socket);
                        break;
                    case Message.SRV_DEREG_TYPE:
                        handleTCPSrvDeReg((SrvDeReg)message, socket);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("ServiceAgent server " + this + " dropping tcp message " + message + ": not handled by ServiceAgents");
                        break;
                }
            }
            else
            {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("ServiceAgent server " + this + " dropping tcp message " + message + ": not from loopback address");
            }
        }
    }

    private class Shutdown extends Thread
    {
        @Override
        public void run()
        {
            if (StandardServiceAgentServer.this.isRunning()) StandardServiceAgentServer.this.stop();
        }
    }
}
