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
package org.livetribe.slp;

import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class ServiceTypeTest
{
    @Test
    public void testInvalidServiceType1() throws Exception
    {
        try
        {
            new ServiceType(null);
            throw new AssertionError();
        }
        catch (NullPointerException ignored)
        {
        }
    }

    @Test
    public void testValidServiceType1() throws Exception
    {
        ServiceType serviceType = new ServiceType("service:jmx:http");
        assert serviceType.isServiceURL();
        assert serviceType.isAbstractType();
        assert "service:jmx".equals(serviceType.getAbstractTypeName());
        assert "http".equals(serviceType.getConcreteTypeName());
        assert "jmx".equals(serviceType.getPrincipleTypeName());
        assert "".equals(serviceType.getNamingAuthority());
        assert serviceType.isDefaultNamingAuthority();
    }

    @Test
    public void testValidServiceType2() throws Exception
    {
        ServiceType serviceType = new ServiceType("service:jmx:http.na");
        assert serviceType.isServiceURL();
        assert serviceType.isAbstractType();
        assert "service:jmx".equals(serviceType.getAbstractTypeName());
        assert "http".equals(serviceType.getConcreteTypeName());
        assert "jmx".equals(serviceType.getPrincipleTypeName());
        assert "na".equals(serviceType.getNamingAuthority());
        assert !serviceType.isDefaultNamingAuthority();
    }

    @Test
    public void testValidServiceType3() throws Exception
    {
        ServiceType serviceType = new ServiceType("service:jmx");
        assert serviceType.isServiceURL();
        assert !serviceType.isAbstractType();
        assert "".equals(serviceType.getAbstractTypeName());
        assert "".equals(serviceType.getConcreteTypeName());
        assert "jmx".equals(serviceType.getPrincipleTypeName());
        assert "".equals(serviceType.getNamingAuthority());
        assert serviceType.isDefaultNamingAuthority();
    }

    @Test
    public void testValidServiceType4() throws Exception
    {
        ServiceType serviceType = new ServiceType("service:jmx.na");
        assert serviceType.isServiceURL();
        assert !serviceType.isAbstractType();
        assert "".equals(serviceType.getAbstractTypeName());
        assert "".equals(serviceType.getConcreteTypeName());
        assert "jmx".equals(serviceType.getPrincipleTypeName());
        assert "na".equals(serviceType.getNamingAuthority());
        assert !serviceType.isDefaultNamingAuthority();
    }

    @Test
    public void testValidServiceType5() throws Exception
    {
        ServiceType serviceType = new ServiceType("jmx:http");
        assert !serviceType.isServiceURL();
        assert serviceType.isAbstractType();
        assert "jmx".equals(serviceType.getAbstractTypeName());
        assert "http".equals(serviceType.getConcreteTypeName());
        assert "jmx".equals(serviceType.getPrincipleTypeName());
        assert "".equals(serviceType.getNamingAuthority());
        assert serviceType.isDefaultNamingAuthority();
    }

    @Test
    public void testValidServiceType6() throws Exception
    {
        ServiceType serviceType = new ServiceType("jmx:http.na");
        assert !serviceType.isServiceURL();
        assert serviceType.isAbstractType();
        assert "jmx".equals(serviceType.getAbstractTypeName());
        assert "http".equals(serviceType.getConcreteTypeName());
        assert "jmx".equals(serviceType.getPrincipleTypeName());
        assert "na".equals(serviceType.getNamingAuthority());
        assert !serviceType.isDefaultNamingAuthority();
    }

    @Test
    public void testValidServiceType7() throws Exception
    {
        ServiceType serviceType = new ServiceType("http");
        assert !serviceType.isServiceURL();
        assert !serviceType.isAbstractType();
        assert "".equals(serviceType.getAbstractTypeName());
        assert "".equals(serviceType.getConcreteTypeName());
        assert "http".equals(serviceType.getPrincipleTypeName());
        assert "".equals(serviceType.getNamingAuthority());
        assert serviceType.isDefaultNamingAuthority();
    }

    @Test
    public void testValidServiceType8() throws Exception
    {
        ServiceType serviceType = new ServiceType("http.na");
        assert !serviceType.isServiceURL();
        assert !serviceType.isAbstractType();
        assert "".equals(serviceType.getAbstractTypeName());
        assert "".equals(serviceType.getConcreteTypeName());
        assert "http".equals(serviceType.getPrincipleTypeName());
        assert "na".equals(serviceType.getNamingAuthority());
        assert !serviceType.isDefaultNamingAuthority();
    }

    @Test
    public void testEquals() throws Exception
    {
        ServiceType serviceType1 = new ServiceType("service:jmx:rmi");
        ServiceType serviceType2 = new ServiceType("jmx:rmi");
        assert !serviceType1.equals(serviceType2);

        serviceType2 = new ServiceType("service:jmx:rmi");
        assert serviceType1.equals(serviceType2);
    }

    @Test
    public void testMatch1() throws Exception
    {
        ServiceType st1 = new ServiceType("service:jmx:rmi");
        ServiceType st2 = new ServiceType("service:jmx:rmi");
        assert st1.matches(st2);
        assert st2.matches(st1);

        st1 = new ServiceType("jmx:rmi");
        st2 = new ServiceType("jmx:rmi");
        assert st1.matches(st2);
        assert st2.matches(st1);

        st1 = new ServiceType("rmi");
        st2 = new ServiceType("rmi");
        assert st1.matches(st2);
        assert st2.matches(st1);
    }

    @Test
    public void testMatch2() throws Exception
    {
        ServiceType st1 = new ServiceType("service:jmx:rmi");
        ServiceType st2 = new ServiceType("service:jmx");
        assert !st1.matches(st2);
        assert st2.matches(st1);

        st1 = new ServiceType("jmx:rmi");
        st2 = new ServiceType("jmx");
        assert !st1.matches(st2);
        assert st2.matches(st1);
    }

    @Test
    public void testMatch3() throws Exception
    {
        ServiceType st1 = new ServiceType("service:jmx:http");
        ServiceType st2 = new ServiceType("service:jmx:rmi");
        assert !st1.matches(st2);
        assert !st2.matches(st1);

        st1 = new ServiceType("jmx:http");
        st2 = new ServiceType("jmx:rmi");
        assert !st1.matches(st2);
        assert !st2.matches(st1);

        st1 = new ServiceType("http");
        st2 = new ServiceType("rmi");
        assert !st1.matches(st2);
        assert !st2.matches(st1);
    }

    @Test
    public void testMatch4() throws Exception
    {
        ServiceType st1 = new ServiceType("service:foo:rmi");
        ServiceType st2 = new ServiceType("service:bar:rmi");
        assert !st1.matches(st2);
        assert !st2.matches(st1);

        st1 = new ServiceType("foo:rmi");
        st2 = new ServiceType("bar:rmi");
        assert !st1.matches(st2);
        assert !st2.matches(st1);

        st1 = new ServiceType("foo:rmi");
        st2 = new ServiceType("rmi");
        assert !st1.matches(st2);
        assert !st2.matches(st1);
    }

    @Test
    public void testMatch5() throws Exception
    {
        ServiceType st1 = new ServiceType("service:foo:rmi");
        ServiceType st2 = new ServiceType("foo:rmi");
        assert st1.matches(st2);
        assert st2.matches(st1);
    }
}
