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

/**
 * The configuration {@link Key}s used in LiveTribe SLP.
 * <br />
 * Key strings beginning with 'net.slp' are those defined by RFC 2614.
 * Key strings beginning with 'livetribe.slp' are specific to LiveTribe's implementation.
 * <br />
 * Abbreviations:
 * <ul>
 * <li>DA = DirectoryAgent server</li>
 * <li>SA = ServiceAgent</li>
 * <li>SAC = ServiceAgent client</li>
 * <li>SAS = ServiceAgent server</li>
 * <li>UA = UserAgent</li>
 * <li>UAC = UserAgent client</li>
 * </ul>
 *
 * @version $Revision$ $Date$
 */
public class Keys
{
    /**
     * The key to specify the broadcast IP address, used instead of the multicast address, to send SLP messages via UDP.
     * Default value is "255.255.255.255".
     *
     * @see #BROADCAST_ENABLED_KEY
     * @see #MULTICAST_ADDRESS_KEY
     */
    public static final Key<String> BROADCAST_ADDRESS_KEY = Key.from("net.slp.broadcastAddress", String.class);

    /**
     * The key to specify a comma separated list of the IP addresses of the DAs present on the network.
     * Default value is no addresses, so that DAs will be discovered via multicast.
     */
    public static final Key<String[]> DA_ADDRESSES_KEY = Key.from("net.slp.DAAddresses", String[].class);

    /**
     * The key to specify the attributes string of the DA.
     * Default value is the empty string, therefore no attributes.
     */
    public static final Key<String> DA_ATTRIBUTES_KEY = Key.from("net.slp.DAAttributes", String.class);

    /**
     * The key to specify the interval, in seconds, between advertisements emitted by DAs.
     * Corresponds to RFC 2608 CONFIG_DA_BEAT.
     * Default value is 3 hours, therefore 10800 seconds.
     */
    public static final Key<Integer> DA_ADVERTISEMENT_PERIOD_KEY = Key.from("net.slp.DAHeartBeat", Integer.class);

    /**
     * The key to specify a comma separated list of timeouts, in milliseconds, to wait after unicast message sends.
     * The number of timeouts also specifies the number of times to retry before giving up.
     * Default value is the list "150,250,400".
     *
     * @see #MULTICAST_TIMEOUTS_KEY
     */
    public static final Key<int[]> UNICAST_TIMEOUTS_KEY = Key.from("net.slp.datagramTimeouts", int[].class);

    /**
     * The key to specify a comma separated list of IP addresses in case of multihomed hosts with no multicast
     * routing between interfaces.
     * Default value is the any address, "0.0.0.0".
     */
    public static final Key<String[]> ADDRESSES_KEY = Key.from("net.slp.interfaces", String[].class);

    /**
     * The key to specify whether broadcast is enabled or not (if not, multicast is used instead).
     * Default value is false, therefore broadcast is not enabled.
     */
    public static final Key<Boolean> BROADCAST_ENABLED_KEY = Key.from("net.slp.isBroadcastOnly", Boolean.class);

    /**
     * The key to specify the language.
     * Default value is "en".
     */
    public static final Key<String> LANGUAGE_KEY = Key.from("net.slp.locale", String.class);

    /**
     * The key to specify the max transmission unit (MTU).
     * Default value is 1400.
     */
    public static final Key<Integer> MAX_TRANSMISSION_UNIT_KEY = Key.from("net.slp.MTU", Integer.class);

    /**
     * The key to specify the SLP multicast address.
     * Default value is "239.255.255.253"
     */
    public static final Key<String> MULTICAST_ADDRESS_KEY = Key.from("net.slp.multicastAddress", String.class);

    /**
     * The key to specify the maximum wait, in milliseconds, in the multicast convergence algorithm.
     * Corresponds to RFC 2608 CONFIG_MC_MAX.
     * Default value is 15 seconds, therefore 15000 milliseconds.
     */
    public static final Key<Integer> MULTICAST_MAX_WAIT_KEY = Key.from("net.slp.multicastMaximumWait", Integer.class);

    /**
     * The key to specify a comma separated list of timeouts, in milliseconds, in the multicast convergence algorithm.
     * Default value is the list "150,250,400,600,1000".
     *
     * @see #UNICAST_TIMEOUTS_KEY
     */
    public static final Key<int[]> MULTICAST_TIMEOUTS_KEY = Key.from("net.slp.multicastTimeouts", int[].class);

    /**
     * The key to specify the multicast time to live (TTL).
     * Default value is 255.
     */
    public static final Key<Integer> MULTICAST_TIME_TO_LIVE_KEY = Key.from("net.slp.multicastTTL", Integer.class);

    /**
     * The key to specify the SLP notification port as assigned by IANA.
     * Default value is 1847.
     */
    public static final Key<Integer> NOTIFICATION_PORT_KEY = Key.from("net.slp.notificationPort", Integer.class);

    /**
     * The key to specify the SLP port as assigned by IANA.
     * Default value is 427.
     */
    public static final Key<Integer> PORT_KEY = Key.from("net.slp.port", Integer.class);

    /**
     * The key to specify the attributes string of SA and SAS.
     * Default value is the empty string, therefore no attributes.
     */
    public static final Key<String> SA_ATTRIBUTES_KEY = Key.from("net.slp.SAAttributes", String.class);

    /**
     * The key to specify a comma separated list of scopes.
     * Default value is "default".
     */
    public static final Key<String[]> SCOPES_KEY = Key.from("net.slp.useScopes", String[].class);

    /**
     * The key to specify the period, in seconds, between purges out of the cache of expired services.
     * Default value is 60.
     */
    public static final Key<Integer> DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY = Key.from("livetribe.slp.da.expired.services.purge.period", Integer.class);

    /**
     * The key to specify the IP address to which the ServiceAgentClient connects to.
     * Default value is 127.0.0.1
     */
    public static final Key<String> SA_CLIENT_CONNECT_ADDRESS = Key.from("livetribe.slp.sa.client.connect.address", String.class);

    /**
     * The key to specify the full qualified name of the ServiceAgentClient factory class.
     */
    public static final Key<String> SA_CLIENT_FACTORY_KEY = Key.from("livetribe.slp.sa.client.factory", String.class);

    /**
     * The key to specify whether the ServiceAgentClient should use TCP to contact the ServiceAgentServer.
     */
    public static final Key<Boolean> SA_CLIENT_USE_TCP = Key.from("livetribe.slp.sa.client.use.tcp", Boolean.class);

    /**
     * The key to specify the full qualified name of the ServiceAgent factory class.
     */
    public static final Key<String> SA_FACTORY_KEY = Key.from("livetribe.slp.sa.factory", String.class);

    /**
     * The key to specify whether service agents renew service registration.
     * Default value is true.
     */
    public static final Key<Boolean> SA_SERVICE_RENEWAL_ENABLED_KEY = Key.from("livetribe.slp.sa.service.renewal.enabled", Boolean.class);

    /**
     * The key to specify the full qualified name of the TCPConnector factory class.
     */
    public static final Key<String> TCP_CONNECTOR_FACTORY_KEY = Key.from("livetribe.slp.tcp.connector.factory", String.class);

    /**
     * The key to specify the full qualified name of the TCPConnectorServer factory class.
     */
    public static final Key<String> TCP_CONNECTOR_SERVER_FACTORY_KEY = Key.from("livetribe.slp.tcp.connector.server.factory", String.class);

    /**
     * The key to specify the maximum message length, in bytes, that can be read via TCP.
     * Default value is 4096.
     */
    public static final Key<Integer> TCP_MESSAGE_MAX_LENGTH_KEY = Key.from("livetribe.slp.tcp.message.max.length", Integer.class);

    /**
     * The key to specify the timeout, in milliseconds, for a TCP read to block waiting for data.
     * Corresponds to RFC 2608 CONFIG_CLOSE_CONN.
     * Default value is 5 minutes, therefore 300000 milliseconds.
     */
    public static final Key<Integer> TCP_READ_TIMEOUT_KEY = Key.from("livetribe.slp.tcp.read.timeout", Integer.class);

    /**
     * The key to specify the full qualified name of the UserAgentClient factory class.
     */
    public static final Key<String> UA_CLIENT_FACTORY_KEY = Key.from("livetribe.slp.ua.client.factory", String.class);

    /**
     * The key to specify the full qualified name of the UserAgent factory class.
     */
    public static final Key<String> UA_FACTORY_KEY = Key.from("livetribe.slp.ua.factory", String.class);

    /**
     * The key to specify the full qualified name of the UDPConnector factory class.
     */
    public static final Key<String> UDP_CONNECTOR_FACTORY_KEY = Key.from("livetribe.slp.udp.connector.factory", String.class);

    /**
     * The key to specify the full qualified name of the UDPConnectorServer factory class.
     */
    public static final Key<String> UDP_CONNECTOR_SERVER_FACTORY_KEY = Key.from("livetribe.slp.udp.connector.server.factory", String.class);

    private Keys()
    {
    }
}
