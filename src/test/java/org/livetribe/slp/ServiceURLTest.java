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

/**
 * @version $Rev$ $Date$
 */
public class ServiceURLTest extends SLPTestCase
{
    public void testInvalidServiceURL1() throws Exception
    {
        try
        {
            new ServiceURL(null, ServiceURL.LIFETIME_NONE);
            fail();
        }
        catch (NullPointerException ignored)
        {
        }
    }

    public void testValidServiceURL1() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://", ServiceURL.LIFETIME_NONE);
        assertEquals(new ServiceType("http"), serviceURL.getServiceType());
        assertEquals("", serviceURL.getHost());
        assertEquals(ServiceURL.NO_PORT, serviceURL.getPort());
        assertEquals("", serviceURL.getURLPath());
    }

    public void testValidServiceURL2() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http:///", ServiceURL.LIFETIME_NONE);
        assertEquals(new ServiceType("http"), serviceURL.getServiceType());
        assertEquals("", serviceURL.getHost());
        assertEquals(ServiceURL.NO_PORT, serviceURL.getPort());
        assertEquals("/", serviceURL.getURLPath());
    }

    public void testValidServiceURL3() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h", ServiceURL.LIFETIME_NONE);
        assertEquals(new ServiceType("http"), serviceURL.getServiceType());
        assertEquals("h", serviceURL.getHost());
        assertEquals(ServiceURL.NO_PORT, serviceURL.getPort());
        assertEquals("", serviceURL.getURLPath());
    }

    public void testValidServiceURL4() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h:1", ServiceURL.LIFETIME_NONE);
        assertEquals(new ServiceType("http"), serviceURL.getServiceType());
        assertEquals("h", serviceURL.getHost());
        assertEquals(1, serviceURL.getPort());
        assertEquals("", serviceURL.getURLPath());
    }

    public void testValidServiceURL5() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h:13/", ServiceURL.LIFETIME_NONE);
        assertEquals(new ServiceType("http"), serviceURL.getServiceType());
        assertEquals("h", serviceURL.getHost());
        assertEquals(13, serviceURL.getPort());
        assertEquals("/", serviceURL.getURLPath());
    }

    public void testValidServiceURL6() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h/p", ServiceURL.LIFETIME_NONE);
        assertEquals(new ServiceType("http"), serviceURL.getServiceType());
        assertEquals("h", serviceURL.getHost());
        assertEquals(ServiceURL.NO_PORT, serviceURL.getPort());
        assertEquals("/p", serviceURL.getURLPath());
    }

    public void testValidServiceURL7() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("http://h:9/p", ServiceURL.LIFETIME_NONE);
        assertEquals(new ServiceType("http"), serviceURL.getServiceType());
        assertEquals("h", serviceURL.getHost());
        assertEquals(9, serviceURL.getPort());
        assertEquals("/p", serviceURL.getURLPath());
    }

    public void testValidServiceURL8() throws Exception
    {
        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_NONE);
        assertEquals(new ServiceType("jmx:rmi"), serviceURL.getServiceType());
        assertEquals("", serviceURL.getHost());
        assertEquals(ServiceURL.NO_PORT, serviceURL.getPort());
        assertEquals("/jndi/rmi:///jmxrmi", serviceURL.getURLPath());
    }
}
