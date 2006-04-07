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
package org.livetribe.slp.api;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @version $Rev$ $Date$
 */
public class PropertiesConfigurationFactory
{
    private PropertiesConfigurationFactory()
    {
    }

    public static Configuration newConfiguration(String resource) throws IOException
    {
        return newConfiguration(resource, PropertiesConfigurationFactory.class.getClassLoader());
    }

    public static Configuration newConfiguration(String resource, ClassLoader classLoader) throws IOException
    {
        InputStream stream = classLoader.getResourceAsStream(resource);

        if (stream == null) throw new FileNotFoundException("Could not find resource " + resource + " in classpath");

        return newConfiguration(stream);
    }

    public static Configuration newConfiguration(InputStream stream) throws IOException
    {
        Properties properties = new Properties();

        properties.load(stream);
        stream.close();

        Configuration configuration = new Configuration();

        Integer port = getAsInteger(properties.getProperty("org.livetribe.slp.port"));
        if (port != null) configuration.setPort(port.intValue());

        String multicastAddress = properties.getProperty("org.livetribe.slp.multicastAddress");
        if (multicastAddress != null) configuration.setMulticastAddress(multicastAddress);

        Integer mtu = getAsInteger(properties.getProperty("net.slp.MTU"));
        if (mtu != null) configuration.setMTU(mtu.intValue());

        Integer multicastTTL = getAsInteger(properties.getProperty("net.slp.multicastTTL"));
        if (multicastTTL != null) configuration.setMulticastTTL(multicastTTL.intValue());

        Long multicastMaxWait = getAsLong(properties.getProperty("net.slp.multicastMaximumWait"));
        if (multicastMaxWait != null) configuration.setMulticastMaxWait(multicastMaxWait.longValue());

        String timeouts = properties.getProperty("net.slp.multicastTimeouts");
        if (timeouts != null) configuration.setMulticastTimeouts(getAsLongArray(timeouts));

        String interfaces = properties.getProperty("net.slp.interfaces");
        if (interfaces != null) configuration.setInterfaceAddresses(interfaces.split(",", 0));

        Integer unicastReadTimeout = getAsInteger(properties.getProperty("org.livetribe.slp.unicastReadTimeout"));
        if (unicastReadTimeout != null) configuration.setUnicastReadTimeout(unicastReadTimeout.intValue());

        Integer unicastMaxMessageLength = getAsInteger(properties.getProperty("org.livetribe.slp.unicastMaxMessageLength"));
        if (unicastMaxMessageLength != null) configuration.setUnicastMaxMessageLength(unicastMaxMessageLength.intValue());

        Integer daHeartBeatPeriod = getAsInteger(properties.getProperty("net.slp.DAHeartBeat"));
        if (daHeartBeatPeriod != null) configuration.setDAHeartBeatPeriod(daHeartBeatPeriod.intValue());

        Integer daDiscoveryStartWaitBound = getAsInteger(properties.getProperty("net.slp.randomWaitBound"));
        if (daDiscoveryStartWaitBound != null) configuration.setDADiscoveryStartWaitBound(daDiscoveryStartWaitBound.intValue());

        Integer daDiscoveryPeriod = getAsInteger(properties.getProperty("net.slp.DAActiveDiscoveryInterval"));
        if (daDiscoveryPeriod != null) configuration.setDADiscoveryPeriod(daDiscoveryPeriod.intValue());

        return configuration;
    }

    private static Integer getAsInteger(String value)
    {
        return Integer.valueOf(value);
    }

    private static Long getAsLong(String value)
    {
        return Long.valueOf(value);
    }

    private static long[] getAsLongArray(String value)
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
