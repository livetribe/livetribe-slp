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

import java.util.Locale;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;

/**
 * @version $Rev$ $Date$
 */
public class ServiceInfoCacheTest extends SLPTestSupport
{
    /**
     * @testng.test
     */
    public void testPut() throws Exception
    {
        ServiceInfoCache cache = new ServiceInfoCache();

        ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz1");
        Scopes scopes = new Scopes(new String[]{"scope1"});
        ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL, scopes, null, Locale.ENGLISH.getLanguage());
        ServiceInfo result = cache.put(serviceInfo1);

        assert result == null;
        assert cache.getSize() == 1;
        ServiceInfo serviceInfo = cache.get(serviceInfo1.getKey());
        assert serviceInfo == serviceInfo1;

        // Different language
        ServiceInfo serviceInfo2 = new ServiceInfo(serviceInfo1.getServiceURL(), serviceInfo1.getScopes(), serviceInfo1.getAttributes(), Locale.GERMAN.getLanguage());
        result = cache.put(serviceInfo2);

        assert result == null;
        assert cache.getSize() == 2;
        serviceInfo = cache.get(serviceInfo2.getKey());
        assert serviceInfo == serviceInfo2;

        // Replace an existing service
        Attributes attributes = new Attributes("(attr=sict1)");
        ServiceInfo serviceInfo3 = new ServiceInfo(serviceInfo1.getServiceURL(), serviceInfo1.getScopes(), attributes, serviceInfo1.getLanguage());
        result = cache.put(serviceInfo3);

        assert result == serviceInfo1;
        assert cache.getSize() == 2;
        serviceInfo = cache.get(serviceInfo3.getKey());
        assert serviceInfo == serviceInfo3;

        // Wrong registration: same ServiceURL, different ServiceType
        ServiceType serviceType = new ServiceType("service:bar:baz");
        ServiceInfo serviceInfo4 = new ServiceInfo(serviceType, serviceInfo1.getServiceURL(), serviceInfo1.getScopes(), serviceInfo1.getAttributes(), serviceInfo1.getLanguage());
        try
        {
            cache.put(serviceInfo4);
            throw new AssertionError();
        }
        catch (IllegalArgumentException x)
        {
        }
    }

    /**
     * @testng.test
     */
    public void testUpdateAdd() throws Exception
    {
        ServiceInfoCache cache = new ServiceInfoCache();

        ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz2");
        Attributes attributes1 = new Attributes("(attr=sict1),tag");
        Scopes scopes = new Scopes(new String[]{"scope1"});
        ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL, scopes, attributes1, Locale.ENGLISH.getLanguage());
        cache.put(serviceInfo1);

        Attributes attributes2 = new Attributes("(attr=sict2),(another=1)");
        ServiceInfo serviceInfo2 = new ServiceInfo(serviceInfo1.getServiceURL(), serviceInfo1.getScopes(), attributes2, serviceInfo1.getLanguage());
        ServiceInfo existing = cache.updateAdd(serviceInfo2);

        assert existing == serviceInfo1;
        assert cache.getSize() == 1;
        ServiceInfo merged = cache.get(serviceInfo2.getKey());
        assert merged != null;
        Attributes mergedAttributes = new Attributes("(attr=sict2),tag,(another=1)");
        assert merged.getAttributes().equals(mergedAttributes);
    }

    /**
     * @testng.test
     */
    public void testUpdateRemove() throws Exception
    {
        ServiceInfoCache cache = new ServiceInfoCache();

        ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz2");
        Attributes attributes1 = new Attributes("(attr=sict1),tag");
        Scopes scopes = new Scopes(new String[]{"scope1"});
        ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL, scopes, attributes1, Locale.ENGLISH.getLanguage());
        cache.put(serviceInfo1);

        Attributes attributes2 = new Attributes("tag");
        ServiceInfo serviceInfo2 = new ServiceInfo(serviceInfo1.getServiceURL(), serviceInfo1.getScopes(), attributes2, serviceInfo1.getLanguage());
        ServiceInfo existing = cache.updateRemove(serviceInfo2);

        assert existing == serviceInfo1;
        assert cache.getSize() == 1;
        ServiceInfo merged = cache.get(serviceInfo2.getKey());
        assert merged != null;
        Attributes mergedAttributes = new Attributes("(attr=sict1)");
        assert merged.getAttributes().equals(mergedAttributes);
    }

    /**
     * @testng.test
     */
    public void testRemove() throws Exception
    {
        ServiceInfoCache cache = new ServiceInfoCache();

        ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz2");
        Attributes attributes1 = new Attributes("(attr=sict1),tag");
        Scopes scopes = new Scopes(new String[]{"scope1"});
        ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL, scopes, attributes1, Locale.ENGLISH.getLanguage());
        cache.put(serviceInfo1);

        ServiceInfo result = cache.remove(serviceInfo1.getKey());
        assert result == serviceInfo1;
        assert cache.getSize() == 0;
    }
}
