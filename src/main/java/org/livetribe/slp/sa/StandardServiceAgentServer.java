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
package org.livetribe.slp.sa;

import java.io.File;
import java.net.Socket;
import java.util.logging.Level;

import org.livetribe.slp.settings.PropertiesSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.Attributes;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.srv.Factories;
import org.livetribe.slp.srv.Server;
import org.livetribe.slp.srv.TCPSrvAckPerformer;
import org.livetribe.slp.srv.ServiceInfoCache;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.msg.SrvAck;
import org.livetribe.slp.srv.msg.SrvDeReg;
import org.livetribe.slp.srv.msg.SrvReg;
import org.livetribe.slp.srv.net.MessageEvent;
import org.livetribe.slp.srv.net.MessageListener;
import org.livetribe.slp.srv.net.TCPConnector;
import org.livetribe.slp.srv.net.TCPConnectorServer;
import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.srv.net.UDPConnectorServer;
import org.livetribe.slp.srv.sa.AbstractServiceAgent;
import org.livetribe.slp.srv.sa.SAServiceInfo;
import org.livetribe.slp.srv.sa.ServiceAgentInfo;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgentServer extends AbstractServiceAgent
{
    public static void main(String[] args) throws Exception
    {
        Settings settings = null;
        if (args.length > 0) settings = PropertiesSettings.from(new File(args[0]));
        Server server = newInstance(settings);
        server.start();
    }

    public static StandardServiceAgentServer newInstance(Settings settings)
    {
        UDPConnector.Factory udpFactory = Factories.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = Factories.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        UDPConnectorServer.Factory udpServerFactory = Factories.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        TCPConnectorServer.Factory tcpServerFactory = Factories.newInstance(settings, TCP_CONNECTOR_SERVER_FACTORY_KEY);
        return new StandardServiceAgentServer(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), udpServerFactory.newUDPConnectorServer(settings), tcpServerFactory.newTCPConnectorServer(settings), settings);
    }

    private final MessageListener listener = new ServiceAgentMessageListener();
    private final TCPConnectorServer tcpConnectorServer;
    private final TCPSrvAckPerformer tcpSrvAck;

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
        setAttributes(getAttributes().merge(Attributes.from("(" + ServiceAgentInfo.TCP_PORT_TAG + "=" + getPort() + ")")));

        super.doStart();

        tcpConnectorServer.addMessageListener(listener);
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
        tcpConnectorServer.removeMessageListener(listener);
        tcpConnectorServer.stop();
    }

    protected void handleTCPSrvReg(SrvReg srvReg, Socket socket)
    {
        try
        {
            boolean update = srvReg.isUpdating();
            SAServiceInfo givenService = new SAServiceInfo(ServiceInfo.from(srvReg));
            ServiceInfoCache.Result<SAServiceInfo> result = cacheService(givenService, update);
            forwardRegistration(givenService, result.getPrevious(), result.getCurrent(), update);
            tcpSrvAck.perform(socket, srvReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            tcpSrvAck.perform(socket, srvReg, x.getErrorCode());
        }
    }

    protected void handleTCPSrvDeReg(SrvDeReg srvDeReg, Socket socket)
    {
        try
        {
            boolean update = srvDeReg.isUpdating();
            SAServiceInfo givenService = new SAServiceInfo(ServiceInfo.from(srvDeReg));
            ServiceInfoCache.Result<SAServiceInfo> result = uncacheService(givenService, update);
            forwardDeregistration(givenService, result.getPrevious(), result.getCurrent(), update);
            tcpSrvAck.perform(socket, srvDeReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            tcpSrvAck.perform(socket, srvDeReg, x.getErrorCode());
        }
    }

    /**
     * ServiceAgents listen for tcp messages from ServiceAgentClients.
     * They are interested in:
     * <ul>
     * <li>SrvReg, from ServiceAgentClients that want register services; the reply is a SrvAck</li>
     * <li>SrvDeReg, from ServiceAgentClients that want deregister services; the reply is a SrvAck</li>
     * </ul>
     */
    private class ServiceAgentMessageListener implements MessageListener
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
