/*
 * Copyright 2005 the original author or authors
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
package org.livetribe.slp.settings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.sa.ServiceAgentClient;
import org.livetribe.slp.srv.net.TCPConnector;
import org.livetribe.slp.srv.net.TCPConnectorServer;
import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.srv.net.UDPConnectorServer;
import org.livetribe.slp.ua.UserAgent;
import org.livetribe.slp.ua.UserAgentClient;

/**
 * @version $Revision$ $Date$
 */
public class Keys
{
    public static final Key<String> BROADCAST_ADDRESS_KEY = Key.forType("net.slp.broadcastAddress", String.class);
    public static final Key<Boolean> BROADCAST_ENABLED_KEY = Key.forType("net.slp.isBroadcastOnly", Boolean.class);

    public static final Key<String[]> DA_ADDRESSES_KEY = Key.forType("net.slp.DAAddresses", String[].class);
    public static final Key<String> DA_ATTRIBUTES_KEY = Key.forType("net.slp.DAAttributes", String.class);
    public static final Key<Integer> DA_ADVERTISEMENT_PERIOD_KEY = Key.forType("net.slp.DAHeartBeat", Integer.class);
    public static final Key<Integer> DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY = Key.forType("livetribe.slp.da.expired.services.purge.period", Integer.class);

    public static final Key<String[]> ADDRESSES_KEY = Key.forType("net.slp.interfaces", String[].class);

    public static final Key<String> LANGUAGE_KEY = Key.forType("net.slp.locale", String.class);

    public static final Key<Integer> MAX_TRANSMISSION_UNIT_KEY = Key.forType("net.slp.MTU", Integer.class);
    public static final Key<String> MULTICAST_ADDRESS_KEY = Key.forType("net.slp.multicastAddress", String.class);
    public static final Key<Integer> MULTICAST_MAX_WAIT_KEY = Key.forType("net.slp.multicastMaximumWait", Integer.class);
    public static final Key<int[]> MULTICAST_TIMEOUTS_KEY = Key.forType("net.slp.multicastTimeouts", int[].class);
    public static final Key<Integer> MULTICAST_TIME_TO_LIVE_KEY = Key.forType("net.slp.multicastTTL", Integer.class);

    public static final Key<Integer> NOTIFICATION_PORT_KEY = Key.forType("net.slp.notificationPort", Integer.class);

    public static final Key<Integer> PORT_KEY = Key.forType("net.slp.port", Integer.class);

    public static final Key<String[]> SCOPES_KEY = Key.forType("net.slp.useScopes", String[].class);

    public static final Key<String> SA_ATTRIBUTES_KEY = Key.forType("net.slp.SAAttributes", String.class);
    public static final Key<Class<ServiceAgentClient.Factory>> SA_CLIENT_FACTORY_KEY = Key.forClass("livetribe.slp.sa.client.factory", ServiceAgentClient.Factory.class);
    public static final Key<Class<ServiceAgent.Factory>> SA_FACTORY_KEY = Key.forClass("livetribe.slp.sa.factory", ServiceAgent.Factory.class);
    public static final Key<Boolean> SA_SERVICE_RENEWAL_ENABLED_KEY = Key.forType("livetribe.slp.sa.service.renewal.enabled", Boolean.class);

    public static final Key<Class<TCPConnector.Factory>> TCP_CONNECTOR_FACTORY_KEY = Key.forClass("livetribe.slp.tcp.connector.factory", TCPConnector.Factory.class);
    public static final Key<Class<TCPConnectorServer.Factory>> TCP_CONNECTOR_SERVER_FACTORY_KEY = Key.forClass("livetribe.slp.tcp.connector.server.factory", TCPConnectorServer.Factory.class);
    public static final Key<Integer> TCP_MESSAGE_MAX_LENGTH_KEY = Key.forType("livetribe.slp.tcp.message.max.length", Integer.class);
    public static final Key<Integer> TCP_READ_TIMEOUT_KEY = Key.forType("livetribe.slp.tcp.read.timeout", Integer.class);

    public static final Key<Class<UserAgentClient.Factory>> UA_CLIENT_FACTORY_KEY = Key.forClass("livetribe.slp.ua.client.factory", UserAgentClient.Factory.class);
    public static final Key<Class<UserAgent.Factory>> UA_FACTORY_KEY = Key.forClass("livetribe.slp.ua.factory", UserAgent.Factory.class);
    public static final Key<Class<UDPConnector.Factory>> UDP_CONNECTOR_FACTORY_KEY = Key.forClass("livetribe.slp.udp.connector.factory", UDPConnector.Factory.class);
    public static final Key<Class<UDPConnectorServer.Factory>> UDP_CONNECTOR_SERVER_FACTORY_KEY = Key.forClass("livetribe.slp.udp.connector.server.factory", UDPConnectorServer.Factory.class);

    public static final Key<ExecutorService> EXECUTOR_SERVICE_KEY = Key.forType("livetribe.slp.executor.service", ExecutorService.class);
    public static final Key<ScheduledExecutorService> SCHEDULED_EXECUTOR_SERVICE_KEY = Key.forType("livetribe.slp.scheduled.executor.service", ScheduledExecutorService.class);


    protected Keys()
    {
    }
}
