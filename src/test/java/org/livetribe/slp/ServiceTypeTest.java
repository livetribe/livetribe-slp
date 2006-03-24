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
package org.livetribe.slp;

/**
 * $Rev$
 */
public class ServiceTypeTest extends SLPTestCase
{
    public void testInvalidServiceType1() throws Exception
    {
        try
        {
            new ServiceType(null);
            fail();
        }
        catch (NullPointerException ignored)
        {
        }
    }

    public void testValidServiceType1() throws Exception
    {
        ServiceType serviceType = new ServiceType("service:jmx:http");
        assertTrue(serviceType.isServiceURL());
        assertTrue(serviceType.isAbstractType());
        assertEquals("service:jmx", serviceType.getAbstractTypeName());
        assertEquals("http", serviceType.getConcreteTypeName());
        assertEquals("jmx", serviceType.getPrincipleTypeName());
        assertEquals("", serviceType.getNamingAuthority());
        assertTrue(serviceType.isNADefault());
    }

    public void testValidServiceType2() throws Exception
    {
        ServiceType serviceType = new ServiceType("service:jmx:http.na");
        assertTrue(serviceType.isServiceURL());
        assertTrue(serviceType.isAbstractType());
        assertEquals("service:jmx", serviceType.getAbstractTypeName());
        assertEquals("http", serviceType.getConcreteTypeName());
        assertEquals("jmx", serviceType.getPrincipleTypeName());
        assertEquals("na", serviceType.getNamingAuthority());
        assertFalse(serviceType.isNADefault());
    }

    public void testValidServiceType3() throws Exception
    {
        ServiceType serviceType = new ServiceType("service:jmx");
        assertTrue(serviceType.isServiceURL());
        assertFalse(serviceType.isAbstractType());
        assertEquals("", serviceType.getAbstractTypeName());
        assertEquals("", serviceType.getConcreteTypeName());
        assertEquals("jmx", serviceType.getPrincipleTypeName());
        assertEquals("", serviceType.getNamingAuthority());
        assertTrue(serviceType.isNADefault());
    }

    public void testValidServiceType4() throws Exception
    {
        ServiceType serviceType = new ServiceType("service:jmx.na");
        assertTrue(serviceType.isServiceURL());
        assertFalse(serviceType.isAbstractType());
        assertEquals("", serviceType.getAbstractTypeName());
        assertEquals("", serviceType.getConcreteTypeName());
        assertEquals("jmx", serviceType.getPrincipleTypeName());
        assertEquals("na", serviceType.getNamingAuthority());
        assertFalse(serviceType.isNADefault());
    }

    public void testValidServiceType5() throws Exception
    {
        ServiceType serviceType = new ServiceType("jmx:http");
        assertFalse(serviceType.isServiceURL());
        assertTrue(serviceType.isAbstractType());
        assertEquals("jmx", serviceType.getAbstractTypeName());
        assertEquals("http", serviceType.getConcreteTypeName());
        assertEquals("jmx", serviceType.getPrincipleTypeName());
        assertEquals("", serviceType.getNamingAuthority());
        assertTrue(serviceType.isNADefault());
    }

    public void testValidServiceType6() throws Exception
    {
        ServiceType serviceType = new ServiceType("jmx:http.na");
        assertFalse(serviceType.isServiceURL());
        assertTrue(serviceType.isAbstractType());
        assertEquals("jmx", serviceType.getAbstractTypeName());
        assertEquals("http", serviceType.getConcreteTypeName());
        assertEquals("jmx", serviceType.getPrincipleTypeName());
        assertEquals("na", serviceType.getNamingAuthority());
        assertFalse(serviceType.isNADefault());
    }

    public void testValidServiceType7() throws Exception
    {
        ServiceType serviceType = new ServiceType("http");
        assertFalse(serviceType.isServiceURL());
        assertFalse(serviceType.isAbstractType());
        assertEquals("", serviceType.getAbstractTypeName());
        assertEquals("", serviceType.getConcreteTypeName());
        assertEquals("http", serviceType.getPrincipleTypeName());
        assertEquals("", serviceType.getNamingAuthority());
        assertTrue(serviceType.isNADefault());
    }

    public void testValidServiceType8() throws Exception
    {
        ServiceType serviceType = new ServiceType("http.na");
        assertFalse(serviceType.isServiceURL());
        assertFalse(serviceType.isAbstractType());
        assertEquals("", serviceType.getAbstractTypeName());
        assertEquals("", serviceType.getConcreteTypeName());
        assertEquals("http", serviceType.getPrincipleTypeName());
        assertEquals("na", serviceType.getNamingAuthority());
        assertFalse(serviceType.isNADefault());
    }

    public void testMatch1() throws Exception
    {
        ServiceType st1 = new ServiceType("service:jmx:rmi");
        ServiceType st2 = new ServiceType("service:jmx:rmi");
        assertTrue(st1.matches(st2));
        assertTrue(st2.matches(st1));

        st1 = new ServiceType("jmx:rmi");
        st2 = new ServiceType("jmx:rmi");
        assertTrue(st1.matches(st2));
        assertTrue(st2.matches(st1));

        st1 = new ServiceType("rmi");
        st2 = new ServiceType("rmi");
        assertTrue(st1.matches(st2));
        assertTrue(st2.matches(st1));
    }

    public void testMatch2() throws Exception
    {
        ServiceType st1 = new ServiceType("service:jmx:rmi");
        ServiceType st2 = new ServiceType("service:jmx");
        assertTrue(st1.matches(st2));
        assertFalse(st2.matches(st1));

        st1 = new ServiceType("jmx:rmi");
        st2 = new ServiceType("jmx");
        assertTrue(st1.matches(st2));
        assertFalse(st2.matches(st1));
    }

    public void testMatch3() throws Exception
    {
        ServiceType st1 = new ServiceType("service:jmx:http");
        ServiceType st2 = new ServiceType("service:jmx:rmi");
        assertFalse(st1.matches(st2));
        assertFalse(st2.matches(st1));

        st1 = new ServiceType("jmx:http");
        st2 = new ServiceType("jmx:rmi");
        assertFalse(st1.matches(st2));
        assertFalse(st2.matches(st1));

        st1 = new ServiceType("http");
        st2 = new ServiceType("rmi");
        assertFalse(st1.matches(st2));
        assertFalse(st2.matches(st1));
    }

    public void testMatch4() throws Exception
    {
        ServiceType st1 = new ServiceType("service:foo:rmi");
        ServiceType st2 = new ServiceType("service:bar:rmi");
        assertFalse(st1.matches(st2));
        assertFalse(st2.matches(st1));

        st1 = new ServiceType("foo:rmi");
        st2 = new ServiceType("bar:rmi");
        assertFalse(st1.matches(st2));
        assertFalse(st2.matches(st1));

        st1 = new ServiceType("foo:rmi");
        st2 = new ServiceType("rmi");
        assertFalse(st1.matches(st2));
        assertFalse(st2.matches(st1));
    }
}
