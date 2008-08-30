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
package org.livetribe.slp.ua;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLP;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.da.StandardDirectoryAgentServer;
import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.sa.ServiceAgentClient;
import org.livetribe.slp.sa.ServiceNotificationEvent;
import org.livetribe.slp.sa.ServiceNotificationListener;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class StandardUserAgentTest
{
    private Settings newSettings()
    {
        Settings settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        return settings;
    }

    @Test
    public void testStartStop() throws Exception
    {
        StandardUserAgent ua = StandardUserAgent.newInstance(newSettings());
        assert !ua.isRunning();
        ua.start();
        assert ua.isRunning();
        ua.stop();
        assert !ua.isRunning();
    }

    @Test
    public void testFindServicesFromDirectoryAgent() throws Exception
    {
        Settings daSettings = newSettings();
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(daSettings);
        da.start();

        try
        {
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///suat1");
            Attributes attributes = Attributes.from("(attr=suat1)");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, attributes);
            ServiceAgentClient sa = SLP.newServiceAgentClient(newSettings());
            sa.register(service);

            Settings uaSettings = newSettings();
            uaSettings.put(UA_UNICAST_PREFER_TCP, true);
            StandardUserAgent ua = StandardUserAgent.newInstance(uaSettings);
            ua.start();
            try
            {
                List<ServiceInfo> services = ua.findServices(serviceURL.getServiceType(), null, null, null);
                assert services != null;
                assert services.size() == 1;
                ServiceInfo serviceInfo = services.get(0);
                assert serviceInfo != null;
                ServiceURL foundService = serviceInfo.getServiceURL();
                assert foundService != null;
                assert serviceURL.equals(foundService);
                assert serviceURL.getLifetime() == foundService.getLifetime();
                assert serviceInfo.getAttributes() != null;
                assert serviceInfo.getAttributes().equals(attributes);
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testListenForDirectoryAgents() throws Exception
    {
        StandardUserAgent ua = StandardUserAgent.newInstance(newSettings());
        ua.start();
        assert ua.getDirectoryAgents().size() == 0;

        try
        {
            StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
            da.start();
            try
            {
                // Wait for the UA to get the DAAdvert from DAS
                Thread.sleep(500);

                assert ua.getDirectoryAgents().size() == 1;
            }
            finally
            {
                da.stop();
            }
        }
        finally
        {
            ua.stop();
        }
    }


    @Test
    public void testDirectoryAgentDiscoveryOnStartup() throws Exception
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.start();
        try
        {
            StandardUserAgent ua = StandardUserAgent.newInstance(newSettings());
            ua.start();
            try
            {
                List<DirectoryAgentInfo> das = ua.getDirectoryAgents();
                assert das != null;
                assert das.size() == 1;
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testFindServicesFromTwoServiceAgents()
    {
        ServiceAgent sa1 = SLP.newServiceAgent(newSettings());
        ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://host/suat3");
        String language = Locale.ITALY.getLanguage();
        ServiceInfo service1 = new ServiceInfo(serviceURL1, language, Scopes.DEFAULT, null);
        sa1.register(service1);
        sa1.start();
        try
        {
            ServiceAgent sa2 = SLP.newServiceAgent(newSettings());
            ServiceURL serviceURL2 = new ServiceURL("service:jmx:http://host/suat4");
            ServiceInfo service2 = new ServiceInfo(serviceURL2, language, Scopes.DEFAULT, null);
            sa2.register(service2);
            sa2.start();
            try
            {
                StandardUserAgent ua = StandardUserAgent.newInstance(newSettings());
                ua.start();
                try
                {
                    ServiceType serviceType = new ServiceType("service:jmx");
                    List<ServiceInfo> services = ua.findServices(serviceType, null, null, null);
                    assert services.size() == 2;
                }
                finally
                {
                    ua.stop();
                }
            }
            finally
            {
                sa2.stop();
            }
        }
        finally
        {
            sa1.stop();
        }
    }

    @Test
    public void testListenForRegistrationNotifications() throws Exception
    {
        ServiceAgent sa = SLP.newServiceAgent(newSettings());
        sa.start();
        try
        {
            final AtomicReference<ServiceNotificationEvent> registered = new AtomicReference<ServiceNotificationEvent>();
            final AtomicReference<ServiceNotificationEvent> deregistered = new AtomicReference<ServiceNotificationEvent>();
            ServiceNotificationListener listener = new ServiceNotificationListener()
            {
                public void serviceRegistered(ServiceNotificationEvent event)
                {
                    registered.set(event);
                }

                public void serviceDeregistered(ServiceNotificationEvent event)
                {
                    deregistered.set(event);
                }
            };
            StandardUserAgent ua = StandardUserAgent.newInstance(newSettings());
            ua.addServiceNotificationListener(listener);
            ua.start();
            try
            {
                ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz");
                ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, null);
                sa.register(service);

                // Let the event arrive
                Thread.sleep(500);

                ServiceNotificationEvent event = registered.get();
                assert event != null;
                assert !event.isUpdate();
                assert deregistered.get() == null;
                ServiceInfo registeredService = event.getService();
                assert registeredService.getServiceURL().equals(service.getServiceURL());
                assert registeredService.getLanguage().equals(service.getLanguage());
                assert registeredService.getScopes().equals(service.getScopes());
                assert registeredService.getAttributes().isEmpty();

                registered.set(null);

                Attributes attributes = Attributes.from("(attr=value)");
                sa.addAttributes(service.getServiceURL(), service.getLanguage(), attributes);

                // Let the event arrive
                Thread.sleep(500);

                event = registered.get();
                assert event != null;
                assert event.isUpdate();
                assert deregistered.get() == null;
                registeredService = event.getService();
                assert registeredService.getServiceURL().equals(service.getServiceURL());
                assert registeredService.getLanguage().equals(service.getLanguage());
                assert registeredService.getScopes().equals(service.getScopes());
                assert registeredService.getAttributes().equals(attributes);

                registered.set(null);

                attributes = Attributes.from("attr");
                sa.removeAttributes(service.getServiceURL(), service.getLanguage(), attributes);

                // Let the event arrive
                Thread.sleep(500);

                event = deregistered.get();
                assert event != null;
                assert event.isUpdate();
                assert registered.get() == null;
                ServiceInfo deregisteredService = event.getService();
                assert deregisteredService.getServiceURL().equals(service.getServiceURL());
                assert deregisteredService.getLanguage().equals(service.getLanguage());
                assert deregisteredService.getScopes().equals(service.getScopes());
                assert deregisteredService.getAttributes().equals(attributes);

                deregistered.set(null);

                sa.deregister(service.getServiceURL(), service.getLanguage());

                // Let the event arrive
                Thread.sleep(500);

                event = deregistered.get();
                assert event != null;
                assert !event.isUpdate();
                assert registered.get() == null;
                deregisteredService = event.getService();
                assert deregisteredService.getServiceURL().equals(service.getServiceURL());
                assert deregisteredService.getLanguage().equals(service.getLanguage());
                assert deregisteredService.getScopes().equals(service.getScopes());
                assert deregisteredService.getAttributes().isEmpty();
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            sa.stop();
        }
    }
}
