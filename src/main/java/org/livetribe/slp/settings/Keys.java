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
package org.livetribe.slp.settings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @version $Revision$ $Date$
 */
public class Keys
{
    public static final Key<String> BROADCAST_ADDRESS_KEY = Key.from("net.slp.broadcastAddress", String.class);
    public static final Key<Boolean> BROADCAST_ENABLED_KEY = Key.from("net.slp.isBroadcastOnly", Boolean.class);

    public static final Key<String[]> DA_ADDRESSES_KEY = Key.from("net.slp.DAAddresses", String[].class);
    public static final Key<String> DA_ATTRIBUTES_KEY = Key.from("net.slp.DAAttributes", String.class);
    public static final Key<Integer> DA_ADVERTISEMENT_PERIOD_KEY = Key.from("net.slp.DAHeartBeat", Integer.class);
    public static final Key<Integer> DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY = Key.from("livetribe.slp.da.expired.services.purge.period", Integer.class);

    public static final Key<String[]> ADDRESSES_KEY = Key.from("net.slp.interfaces", String[].class);

    public static final Key<String> LANGUAGE_KEY = Key.from("net.slp.locale", String.class);

    public static final Key<Integer> MAX_TRANSMISSION_UNIT_KEY = Key.from("net.slp.MTU", Integer.class);
    public static final Key<String> MULTICAST_ADDRESS_KEY = Key.from("net.slp.multicastAddress", String.class);
    public static final Key<Integer> MULTICAST_MAX_WAIT_KEY = Key.from("net.slp.multicastMaximumWait", Integer.class);
    public static final Key<int[]> MULTICAST_TIMEOUTS_KEY = Key.from("net.slp.multicastTimeouts", int[].class);
    public static final Key<Integer> MULTICAST_TIME_TO_LIVE_KEY = Key.from("net.slp.multicastTTL", Integer.class);

    public static final Key<Integer> NOTIFICATION_PORT_KEY = Key.from("net.slp.notificationPort", Integer.class);

    public static final Key<Integer> PORT_KEY = Key.from("net.slp.port", Integer.class);

    public static final Key<String[]> SCOPES_KEY = Key.from("net.slp.useScopes", String[].class);

    public static final Key<String> SA_ATTRIBUTES_KEY = Key.from("net.slp.SAAttributes", String.class);
    public static final Key<String> SA_CLIENT_FACTORY_KEY = Key.from("livetribe.slp.sa.client.factory", String.class);
    public static final Key<String> SA_FACTORY_KEY = Key.from("livetribe.slp.sa.factory", String.class);
    public static final Key<Boolean> SA_SERVICE_RENEWAL_ENABLED_KEY = Key.from("livetribe.slp.sa.service.renewal.enabled", Boolean.class);

    public static final Key<String> TCP_CONNECTOR_FACTORY_KEY = Key.from("livetribe.slp.tcp.connector.factory", String.class);
    public static final Key<String> TCP_CONNECTOR_SERVER_FACTORY_KEY = Key.from("livetribe.slp.tcp.connector.server.factory", String.class);
    public static final Key<Integer> TCP_MESSAGE_MAX_LENGTH_KEY = Key.from("livetribe.slp.tcp.message.max.length", Integer.class);
    public static final Key<Integer> TCP_READ_TIMEOUT_KEY = Key.from("livetribe.slp.tcp.read.timeout", Integer.class);

    public static final Key<String> UA_CLIENT_FACTORY_KEY = Key.from("livetribe.slp.ua.client.factory", String.class);
    public static final Key<String> UA_FACTORY_KEY = Key.from("livetribe.slp.ua.factory", String.class);
    public static final Key<String> UDP_CONNECTOR_FACTORY_KEY = Key.from("livetribe.slp.udp.connector.factory", String.class);
    public static final Key<String> UDP_CONNECTOR_SERVER_FACTORY_KEY = Key.from("livetribe.slp.udp.connector.server.factory", String.class);

    public static final Key<ExecutorService> EXECUTOR_SERVICE_KEY = Key.from("livetribe.slp.executor.service", ExecutorService.class);
    public static final Key<ScheduledExecutorService> SCHEDULED_EXECUTOR_SERVICE_KEY = Key.from("livetribe.slp.scheduled.executor.service", ScheduledExecutorService.class);


    protected Keys()
    {
    }
}
