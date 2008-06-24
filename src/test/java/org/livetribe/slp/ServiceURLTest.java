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
package org.livetribe.slp;

import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class ServiceURLTest
{
    @Test
    public void testInvalidServiceURL1() throws Exception
    {
        try
        {
            new ServiceURL(null, ServiceURL.LIFETIME_NONE);
            assert false;
        }
        catch (NullPointerException ignored)
        {
        }

        try
        {
            new ServiceURL("http://", -2);
            assert false;
        }
        catch (IllegalArgumentException ignored)
        {
        }
    }

    @Test
    public void testValidServiceURL1() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://", ServiceURL.LIFETIME_NONE);
        assert new ServiceType("http").equals(serviceURL.getServiceType());
        assert "".equals(serviceURL.getHost());
        assert serviceURL.getPort() == ServiceURL.NO_PORT;
        assert "".equals(serviceURL.getURLPath());
    }

    @Test
    public void testValidServiceURL2() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http:///", ServiceURL.LIFETIME_NONE);
        assert new ServiceType("http").equals(serviceURL.getServiceType());
        assert "".equals(serviceURL.getHost());
        assert serviceURL.getPort() == ServiceURL.NO_PORT;
        assert "/".equals(serviceURL.getURLPath());
    }

    @Test
    public void testValidServiceURL3() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h", ServiceURL.LIFETIME_NONE);
        assert new ServiceType("http").equals(serviceURL.getServiceType());
        assert "h".equals(serviceURL.getHost());
        assert serviceURL.getPort() == ServiceURL.NO_PORT;
        assert "".equals(serviceURL.getURLPath());
    }

    @Test
    public void testValidServiceURL4() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h:1", ServiceURL.LIFETIME_NONE);
        assert new ServiceType("http").equals(serviceURL.getServiceType());
        assert "h".equals(serviceURL.getHost());
        assert serviceURL.getPort() == 1;
        assert "".equals(serviceURL.getURLPath());
    }

    @Test
    public void testValidServiceURL5() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h:13/", ServiceURL.LIFETIME_NONE);
        assert new ServiceType("http").equals(serviceURL.getServiceType());
        assert "h".equals(serviceURL.getHost());
        assert serviceURL.getPort() == 13;
        assert "/".equals(serviceURL.getURLPath());
    }

    @Test
    public void testValidServiceURL6() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h/p", ServiceURL.LIFETIME_NONE);
        assert new ServiceType("http").equals(serviceURL.getServiceType());
        assert "h".equals(serviceURL.getHost());
        assert serviceURL.getPort() == ServiceURL.NO_PORT;
        assert "/p".equals(serviceURL.getURLPath());
    }

    @Test
    public void testValidServiceURL7() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h:9/p", ServiceURL.LIFETIME_NONE);
        assert new ServiceType("http").equals(serviceURL.getServiceType());
        assert "h".equals(serviceURL.getHost());
        assert serviceURL.getPort() == 9;
        assert "/p".equals(serviceURL.getURLPath());
    }

    @Test
    public void testValidServiceURL8() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_NONE);
        assert new ServiceType("service:jmx:rmi").equals(serviceURL.getServiceType());
        assert "".equals(serviceURL.getHost());
        assert serviceURL.getPort() == ServiceURL.NO_PORT;
        assert "/jndi/rmi:///jmxrmi".equals(serviceURL.getURLPath());
    }
}
