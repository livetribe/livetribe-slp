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
package org.livetribe.slp.spi;

/**
 * @version $Revision$ $Date$
 */
public interface Defaults
{
    /**
     * Default value for the SLP port, 427.
     */
    public static final int PORT = 427;

    /**
     * Default value for the SLP notification port, 1847.
     */
    public static final int NOTIFICATION_PORT = 1847;

    /**
     * Default value for the SLP multicast address, 239.255.255.253.
     */
    public static final String MULTICAST_ADDRESS = "239.255.255.253";

    /**
     * Default value for the MTU, 1400 bytes.
     */
    public static final int MAX_TRANSMISSION_UNIT = 1400; // bytes

    /**
     * Default value for the multicast TTL, 255.
     */
    public static final int MULTICAST_TIME_TO_LIVE = 255;

    /**
     * Default value for the max multicast convergence wait, 15000 milliseconds.
     */
    public static final long MULTICAST_MAX_WAIT = 15000L; // milliseconds

    /**
     * Default values for the timeouts of the multicast convergence algorithm (in milliseconds).
     */
    public static final long[] MULTICAST_TIMEOUTS = new long[]{250L, 500L, 750L, 1000L, 1250L, 1500L, 2000L, 3000L, 4000L}; // milliseconds

    /**
     * Default value for the TCP read timeout, 2000 milliseconds.
     */
    public static final int TCP_READ_TIMEOUT = 2000; // milliseconds

    /**
     * Default value for the TCP max message length, 8192 bytes.
     */
    public static final int TCP_MAX_MESSAGE_LENGTH = 8192; // bytes

    /**
     * Default value for DirectoryAgents of the period to check for expired services, 1 second.
     */
    public static final int DA_SERVICE_EXPIRATION_PERIOD = 1; // seconds

    /**
     * Default value for DirectoryAgents of the period to advertise their presence, 10800 seconds.
     */
    public static final int DA_ADVERTISEMENT_PERIOD = 10800; // seconds

    /**
     * Default value for UserAgents of the period to discover DirectoryAgents, 900 seconds.
     */
    public static final long UA_DISCOVERY_PERIOD = 900; // seconds

    /**
     * Default value for UserAgent of the maximum time to wait before start discovery of DirectoryAgents, 3 seconds.
     */
    public static final int UA_DISCOVERY_INITIAL_WAIT_BOUND = 3; // seconds

    /**
     * Default value for UserAgents of the period to discover DirectoryAgents, 900 seconds.
     */
    public static final long SA_DISCOVERY_PERIOD = UA_DISCOVERY_PERIOD;

    /**
     * Default value for UserAgent of the maximum time to wait before start discovery of DirectoryAgents, 3 seconds.
     */
    public static final int SA_DISCOVERY_INITIAL_WAIT_BOUND = UA_DISCOVERY_INITIAL_WAIT_BOUND;
}
