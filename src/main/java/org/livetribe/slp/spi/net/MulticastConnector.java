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
package org.livetribe.slp.spi.net;

import java.io.IOException;
import java.net.InetAddress;

import org.livetribe.slp.api.Configuration;

/**
 * @version $Rev$ $Date$
 */
public abstract class MulticastConnector extends NetworkConnector
{
    private int multicastTimeToLive;
    private int maxTransmissionUnit;
    private InetAddress multicastAddress;

    public void setConfiguration(Configuration configuration) throws IOException
    {
        super.setConfiguration(configuration);
        setMulticastTimeToLive(configuration.getMulticastTTL());
        setMaxTransmissionUnit(configuration.getMTU());
        setMulticastAddress(InetAddress.getByName(configuration.getMulticastAddress()));
    }

    public void setMulticastTimeToLive(int multicastTimeToLive)
    {
        this.multicastTimeToLive = multicastTimeToLive;
    }

    public int getMulticastTimeToLive()
    {
        return multicastTimeToLive;
    }

    public int getMaxTransmissionUnit()
    {
        return maxTransmissionUnit;
    }

    public void setMaxTransmissionUnit(int maxTransmissionUnit)
    {
        this.maxTransmissionUnit = maxTransmissionUnit;
    }

    public InetAddress getMulticastAddress()
    {
        return multicastAddress;
    }

    public void setMulticastAddress(InetAddress multicastAddress)
    {
        this.multicastAddress = multicastAddress;
    }

    public abstract void send(byte[] bytes) throws IOException;
}
