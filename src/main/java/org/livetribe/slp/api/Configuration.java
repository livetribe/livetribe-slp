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
package org.livetribe.slp.api;

/**
 * @version $Rev$ $Date$
 */
public class Configuration
{
    private int port = 427;
    private String multicastAddress = "239.255.255.253";
    private int mtu = 1400; // bytes
    private int multicastTTL = 255; // 0..255
    private long multicastMaxWait = 15000; // milliseconds
    private long[] multicastTimeouts = new long[]{250L, 500L, 750L, 1000L, 1250L, 1500L, 2000L, 3000L, 4000L}; // milliseconds
    private String[] interfaceAddresses = null;
    private int unicastReadTimeout = 2000; // milliseconds
    private int unicastMaxMessageLength = 8192; // bytes
    private int daHeartBeatPeriod = 10800; // seconds
    private int daDiscoveryStartWaitBound = 3; // seconds
    private int daDiscoveryPeriod = 900; // seconds

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getPort()
    {
        return port;
    }

    public void setMulticastAddress(String multicastAddress)
    {
        this.multicastAddress = multicastAddress;
    }

    public String getMulticastAddress()
    {
        return multicastAddress;
    }

    public void setMTU(int mtu)
    {
        this.mtu = mtu;
    }

    public int getMTU()
    {
        return mtu;
    }

    public void setMulticastTTL(int multicastTTL)
    {
        this.multicastTTL = multicastTTL;
    }

    public int getMulticastTTL()
    {
        return multicastTTL;
    }

    public void setMulticastMaxWait(long multicastMaxWait)
    {
        this.multicastMaxWait = multicastMaxWait;
    }

    public long getMulticastMaxWait()
    {
        return multicastMaxWait;
    }

    public void setMulticastTimeouts(long[] multicastTimeouts)
    {
        this.multicastTimeouts = multicastTimeouts;
    }

    public long[] getMulticastTimeouts()
    {
        return multicastTimeouts;
    }

    public void setInterfaceAddresses(String[] interfaceAddresses)
    {
        this.interfaceAddresses = interfaceAddresses;
    }

    public String[] getInterfaceAddresses()
    {
        return interfaceAddresses;
    }

    public void setUnicastReadTimeout(int unicastReadTimeout)
    {
        this.unicastReadTimeout = unicastReadTimeout;
    }

    public int getUnicastReadTimeout()
    {
        return unicastReadTimeout;
    }

    public void setUnicastMaxMessageLength(int unicastMaxMessageLength)
    {
        this.unicastMaxMessageLength = unicastMaxMessageLength;
    }

    public int getUnicastMaxMessageLength()
    {
        return unicastMaxMessageLength;
    }

    public void setDAHeartBeatPeriod(int daHeartBeatPeriod)
    {
        this.daHeartBeatPeriod = daHeartBeatPeriod;
    }

    public int getDAHeartBeatPeriod()
    {
        return daHeartBeatPeriod;
    }

    public void setDADiscoveryStartWaitBound(int daDiscoveryStartWaitBound)
    {
        this.daDiscoveryStartWaitBound = daDiscoveryStartWaitBound;
    }

    public int getDADiscoveryStartWaitBound()
    {
        return daDiscoveryStartWaitBound;
    }

    public void setDADiscoveryPeriod(int daDiscoveryPeriod)
    {
        this.daDiscoveryPeriod = daDiscoveryPeriod;
    }

    public int getDADiscoveryPeriod()
    {
        return daDiscoveryPeriod;
    }
}
