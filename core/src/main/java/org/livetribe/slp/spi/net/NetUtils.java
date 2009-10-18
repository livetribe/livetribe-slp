/*
 * Copyright 2007-2008 the original author or authors
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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Revision$ $Date$
 */
public class NetUtils
{
    public static InetAddress getLocalhost()
    {
        try
        {
            return InetAddress.getLocalHost();
        }
        catch (UnknownHostException x)
        {
            throw new ServiceLocationException("Could not retrieve localhost", SLPError.NETWORK_ERROR);
        }
    }

    public static InetAddress getByName(String host)
    {
        try
        {
            return InetAddress.getByName(host);
        }
        catch (UnknownHostException x)
        {
            throw new ServiceLocationException("Could not retrieve host " + host, SLPError.NETWORK_ERROR);
        }
    }

    public static InetAddress getLoopbackAddress()
    {
        return getByName(null);
    }

    /**
     * Converts the given address, only if it is the wildcard address, to the address assigned to this host.
     *
     * @param address the address to convert
     * @return the address assigned to this host, if the given address is the wildcard address, or the address itself
     *         if the given address is not the wildcard address.
     */
    public static InetAddress convertWildcardAddress(InetAddress address)
    {
        if (address.isAnyLocalAddress()) return NetUtils.getLocalhost();
        return address;
    }

    private NetUtils()
    {
    }
}
