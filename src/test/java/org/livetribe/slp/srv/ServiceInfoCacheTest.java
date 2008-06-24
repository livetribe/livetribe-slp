/*
 * Copyright 2006-2008 the original author or authors
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
package org.livetribe.slp.srv;

import java.util.Locale;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class ServiceInfoCacheTest
{
    @Test
    public void testPut() throws Exception
    {
        ServiceInfoCache<ServiceInfo> cache = new ServiceInfoCache<ServiceInfo>();

        ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz1");
        Scopes scopes = Scopes.from("scope1");
        ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), scopes, null);
        ServiceInfoCache.Result<ServiceInfo> result = cache.put(serviceInfo1);

        assert result.getPrevious() == null;
        assert cache.getSize() == 1;
        ServiceInfo serviceInfo = result.getCurrent();
        assert serviceInfo == serviceInfo1;

        // Different language
        ServiceInfo serviceInfo2 = new ServiceInfo(serviceInfo1.getServiceURL(), Locale.GERMAN.getLanguage(), serviceInfo1.getScopes(), serviceInfo1.getAttributes());
        result = cache.put(serviceInfo2);

        assert result.getPrevious() == null;
        assert cache.getSize() == 2;
        serviceInfo = result.getCurrent();
        assert serviceInfo == serviceInfo2;

        // Replace an existing service
        Attributes attributes = Attributes.from("(attr=sict1)");
        ServiceInfo serviceInfo3 = new ServiceInfo(serviceInfo1.getServiceURL(), serviceInfo1.getLanguage(), serviceInfo1.getScopes(), attributes);
        result = cache.put(serviceInfo3);

        assert result.getPrevious() == serviceInfo1;
        assert cache.getSize() == 2;
        serviceInfo = result.getCurrent();
        assert serviceInfo == serviceInfo3;

        // Wrong registration: same ServiceURL, different ServiceType
        ServiceType serviceType = new ServiceType("service:bar:baz");
        ServiceInfo serviceInfo4 = new ServiceInfo(serviceType, serviceInfo1.getServiceURL(), serviceInfo1.getLanguage(), serviceInfo1.getScopes(), serviceInfo1.getAttributes());
        try
        {
            cache.put(serviceInfo4);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
        }
    }

    @Test
    public void testUpdateAdd() throws Exception
    {
        ServiceInfoCache<ServiceInfo> cache = new ServiceInfoCache<ServiceInfo>();

        ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz2");
        Attributes attributes1 = Attributes.from("(attr=sict1),tag");
        Scopes scopes = Scopes.from("scope1");
        ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), scopes, attributes1);
        cache.put(serviceInfo1);

        Attributes attributes2 = Attributes.from("(attr=sict2),(another=1)");
        ServiceInfo serviceInfo2 = new ServiceInfo(serviceInfo1.getServiceURL(), serviceInfo1.getLanguage(), serviceInfo1.getScopes(), attributes2);
        ServiceInfoCache.Result<ServiceInfo> result = cache.addAttributes(serviceInfo1.getKey(), attributes2);

        assert result.getPrevious() == serviceInfo1;
        assert cache.getSize() == 1;
        ServiceInfo merged = result.getCurrent();
        assert merged != null;
        Attributes mergedAttributes = attributes1.merge(attributes2);
        assert merged.getAttributes().equals(mergedAttributes);
    }

    @Test
    public void testUpdateRemove() throws Exception
    {
        ServiceInfoCache<ServiceInfo> cache = new ServiceInfoCache<ServiceInfo>();

        ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz2");
        Attributes attributes1 = Attributes.from("(attr=sict1),tag");
        Scopes scopes = Scopes.from("scope1");
        ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), scopes, attributes1);
        cache.put(serviceInfo1);

        Attributes attributes2 = Attributes.from("tag");
        ServiceInfoCache.Result<ServiceInfo> result = cache.removeAttributes(serviceInfo1.getKey(), attributes2);

        assert result.getPrevious() == serviceInfo1;
        assert cache.getSize() == 1;
        ServiceInfo merged = result.getCurrent();
        assert merged != null;
        Attributes mergedAttributes = attributes1.unmerge(attributes2);
        assert merged.getAttributes().equals(mergedAttributes);
    }

    @Test
    public void testRemove() throws Exception
    {
        ServiceInfoCache<ServiceInfo> cache = new ServiceInfoCache<ServiceInfo>();

        ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz2");
        Attributes attributes1 = Attributes.from("(attr=sict1),tag");
        Scopes scopes = Scopes.from("scope1");
        ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), scopes, attributes1);
        cache.put(serviceInfo1);

        ServiceInfoCache.Result<ServiceInfo> result = cache.remove(serviceInfo1.getKey());
        assert result.getPrevious() == serviceInfo1;
        assert result.getCurrent() == null;
        assert cache.getSize() == 0;
    }

    @Test
    public void testMatchExpired() throws Exception
    {
        ServiceInfoCache<ServiceInfo> cache = new ServiceInfoCache<ServiceInfo>();

        int lifetime = 1;
        ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://testMatchExpired", lifetime);
        ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
        cache.put(service);

        // Let the service expire
        Thread.sleep(TimeUnit.SECONDS.toMillis(lifetime) + 500);

        // Be sure match() does not return the expired service
        List<ServiceInfo> results = cache.match(serviceURL.getServiceType(), null, Scopes.NONE, null);
        assert results.isEmpty();
    }
}
