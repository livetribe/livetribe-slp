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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @version $Rev$ $Date$
 */
public class Configuration
{
    private final Properties properties = new Properties();

    public Configuration configure(InputStream stream) throws IOException
    {
        properties.load(stream);
        return this;
    }

    public int getPort()
    {
        return Integer.parseInt(properties.getProperty("org.livetribe.slp.port", "427"));
    }

    public String getMulticastAddress()
    {
        return "239.255.255.253";
    }

    public int getMTU()
    {
        return Integer.parseInt(properties.getProperty("net.slp.MTU", "1400"));
    }

    public int getMulticastTTL()
    {
        return Integer.parseInt(properties.getProperty("net.slp.multicastTTL", "255"));
    }

    public int getCorePoolSize()
    {
        return Integer.parseInt(properties.getProperty("org.livetribe.slp.corePoolSize", "2"));
    }

    public int getMaxPoolSize()
    {
        return Integer.parseInt(properties.getProperty("org.livetribe.slp.maxPoolSize", "20"));
    }

    public long getPoolKeepAlive()
    {
        return Integer.parseInt(properties.getProperty("org.livetribe.slp.poolKeepAlive", "5"));
    }

    public long getMulticastMaxWait()
    {
        return Long.parseLong(properties.getProperty("net.slp.multicastMaximumWait", "15000"));
    }

    public long[] getMulticastTimeouts()
    {
        String timeoutString = properties.getProperty("net.slp.multicastTimeouts", "250,500,750,1000,1250,1500,2000,3000,4000");
        return split(timeoutString);
    }

    public String[] getInterfaceAddresses()
    {
        String interfaces = properties.getProperty("net.slp.interfaces", null);
        return interfaces == null ? null : interfaces.split(",", 0);
    }

    public int getUnicastReadTimeout()
    {
        return Integer.parseInt(properties.getProperty("org.livetribe.slp.unicast.read.timeout", "2000"));
    }

    public int getUnicastMaxMessageLength()
    {
        return Integer.parseInt(properties.getProperty("org.livetribe.slp.unicast.max.message.length", "8192"));
    }

    public int getDAHeartBeat()
    {
        return Integer.parseInt(properties.getProperty("net.slp.DAHeartBeat", "10800"));
    }

    public int getDADiscoveryStartWaitBound()
    {
        return getRandomStartWaitBound();
    }

    public int getDADiscoveryPeriod()
    {
        return Integer.parseInt(properties.getProperty("net.slp.DAActiveDiscoveryInterval", "900"));
    }

    private int getRandomStartWaitBound()
    {
        return Integer.parseInt(properties.getProperty("net.slp.randomWaitBound", "3"));
    }

    private long[] split(String value)
    {
        String[] strings = value.split(",", 0);
        long[] result = new long[strings.length];
        for (int i = 0; i < strings.length; ++i)
        {
            String string = strings[i];
            result[i] = Long.parseLong(string);
        }
        return result;
    }
}
