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
package org.livetribe.slp.sa;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLP;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.da.StandardDirectoryAgentServer;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.ua.UserAgent;
import org.livetribe.slp.ua.UserAgentClient;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgentTest
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
        StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
        assert !sa.isRunning();
        sa.start();
        assert sa.isRunning();
        sa.stop();
        assert !sa.isRunning();
    }

    @Test
    public void testRegisterThenStartForwardsToDA() throws Exception
    {
        final AtomicInteger registrationCount = new AtomicInteger(0);
        ServiceListener serviceListener = new ServiceListener()
        {
            public void serviceAdded(ServiceEvent event)
            {
                registrationCount.incrementAndGet();
            }

            public void serviceUpdated(ServiceEvent event)
            {
            }

            public void serviceRemoved(ServiceEvent event)
            {
            }
        };
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.addServiceListener(serviceListener);
        da.start();

        try
        {
            StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat1");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
            sa.register(service);
            sa.start();

            try
            {
                assert registrationCount.get() == 1;

                List<ServiceInfo> daServices = da.getServices();
                assert daServices != null;
                assert daServices.size() == 1;
                ServiceInfo saService = daServices.get(0);
                assert saService != null;
                assert saService.getKey().equals(service.getKey());
                assert saService.getServiceURL().getLifetime() == service.getServiceURL().getLifetime();
                assert saService.getScopes().equals(service.getScopes());
                assert saService.getAttributes().equals(service.getAttributes());
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            da.stop();
            da.removeServiceListener(serviceListener);
        }
    }

    @Test
    public void testStartThenRegisterForwardsToDA() throws Exception
    {
        final AtomicInteger registrationCount = new AtomicInteger(0);
        ServiceListener serviceListener = new ServiceListener()
        {
            public void serviceAdded(ServiceEvent event)
            {
                registrationCount.incrementAndGet();
            }

            public void serviceUpdated(ServiceEvent event)
            {
            }

            public void serviceRemoved(ServiceEvent event)
            {
            }
        };
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.addServiceListener(serviceListener);
        da.start();
        try
        {
            StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
            sa.start();
            try
            {
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat1");
                ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
                sa.register(service);

                assert registrationCount.get() == 1;

                List<ServiceInfo> daServices = da.getServices();
                assert daServices != null;
                assert daServices.size() == 1;
                ServiceInfo daService = daServices.get(0);
                assert daService != null;
                assert daService.getKey().equals(service.getKey());
                assert daService.getServiceURL().getLifetime() == service.getServiceURL().getLifetime();
                assert daService.getAttributes().equals(service.getAttributes());
                assert daService.getScopes().equals(service.getScopes());
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            da.stop();
            da.removeServiceListener(serviceListener);
        }
    }

    @Test
    public void testSAAndDAAreInSyncOnRegisterAndUpdate()
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.start();
        try
        {
            StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
            sa.start();
            try
            {
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat1");
                ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
                sa.register(service);

                List<ServiceInfo> saServices = sa.getServices();
                assert saServices.size() == 1;
                ServiceInfo saService = saServices.get(0);

                List<ServiceInfo> daServices = da.getServices();
                assert daServices.size() == 1;
                ServiceInfo daService = daServices.get(0);

                assert saService.getKey().equals(daService.getKey());
                assert saService.getScopes().equals(daService.getScopes());
                assert saService.getAttributes().equals(daService.getAttributes());

                // Now add attributes, and check again SA and DA have the same service
                Attributes newAttributes = Attributes.from("(f=7)");
                sa.addAttributes(service.getServiceURL(), service.getLanguage(), newAttributes);

                saServices = sa.getServices();
                assert saServices.size() == 1;
                saService = saServices.get(0);

                daServices = da.getServices();
                assert daServices.size() == 1;
                daService = daServices.get(0);

                assert saService.getKey().equals(daService.getKey());
                assert saService.getScopes().equals(daService.getScopes());
                assert saService.getAttributes().equals(daService.getAttributes());

                // Now remove attributes, and check again SA and DA have the same service
                newAttributes = Attributes.from("c");
                sa.removeAttributes(service.getServiceURL(), service.getLanguage(), newAttributes);

                saServices = sa.getServices();
                assert saServices.size() == 1;
                saService = saServices.get(0);

                daServices = da.getServices();
                assert daServices.size() == 1;
                daService = daServices.get(0);

                assert saService.getKey().equals(daService.getKey());
                assert saService.getScopes().equals(daService.getScopes());
                assert saService.getAttributes().equals(daService.getAttributes());
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testRegisterWithDADoesNotNotify() throws Exception
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.start();
        try
        {
            StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
            sa.start();
            try
            {
                final AtomicInteger notificationCount = new AtomicInteger(0);
                ServiceNotificationListener listener = new ServiceNotificationListener()
                {
                    public void serviceRegistered(ServiceNotificationEvent event)
                    {
                        notificationCount.incrementAndGet();
                    }

                    public void serviceDeregistered(ServiceNotificationEvent event)
                    {
                    }
                };
                UserAgent ua = SLP.newUserAgent(newSettings());
                ua.addServiceNotificationListener(listener);
                ua.start();
                try
                {
                    // Since there is a DA, the UA must not be notified of service registration
                    ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat1");
                    ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
                    sa.register(service);
                    Thread.sleep(500);

                    assert notificationCount.get() == 0;
                }
                finally
                {
                    ua.stop();
                    ua.removeServiceNotificationListener(listener);
                }
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testRegisterWithoutDANotifies() throws Exception
    {
        StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
        sa.start();
        try
        {
            final AtomicInteger notificationCount = new AtomicInteger(0);
            ServiceNotificationListener listener = new ServiceNotificationListener()
            {
                public void serviceRegistered(ServiceNotificationEvent event)
                {
                    notificationCount.incrementAndGet();
                }

                public void serviceDeregistered(ServiceNotificationEvent event)
                {
                }
            };
            UserAgent ua = SLP.newUserAgent(newSettings());
            ua.addServiceNotificationListener(listener);
            ua.start();
            try
            {
                // Since there is not a DA, the UA must be notified of service registration
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat1");
                ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
                sa.register(service);
                Thread.sleep(500);

                assert notificationCount.get() == 1;
            }
            finally
            {
                ua.stop();
                ua.removeServiceNotificationListener(listener);
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testRenewalWithDA() throws Exception
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.start();
        try
        {
            StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
            sa.start();
            try
            {
                int lifetime = 3;
                ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://service", lifetime);
                ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
                sa.register(service);

                // Add the listener for renewals
                final AtomicInteger renewalCount = new AtomicInteger(0);
                ServiceListener listener = new ServiceListener()
                {
                    public void serviceAdded(ServiceEvent event)
                    {
                        renewalCount.incrementAndGet();
                    }

                    public void serviceUpdated(ServiceEvent event)
                    {
                    }

                    public void serviceRemoved(ServiceEvent event)
                    {
                    }
                };
                da.addServiceListener(listener);

                // Sleep for the renewal to happen
                Thread.sleep(TimeUnit.SECONDS.toMillis(lifetime));
                assert renewalCount.get() == 1;

                // Update the service; this should cancel the previous renewal and schedule a new one
                renewalCount.set(0);
                sa.addAttributes(service.getServiceURL(), service.getLanguage(), Attributes.from("(a=1,2)"));

                // Sleep for the renewal to happen
                Thread.sleep(TimeUnit.SECONDS.toMillis(lifetime));
                assert renewalCount.get() == 1;

                // Update again
                renewalCount.set(0);
                sa.removeAttributes(service.getServiceURL(), service.getLanguage(), Attributes.from("a"));

                // Sleep for the renewal to happen
                Thread.sleep(TimeUnit.SECONDS.toMillis(lifetime));
                assert renewalCount.get() == 1;

                // Remove the service
                renewalCount.set(0);
                sa.deregister(service.getServiceURL(), service.getLanguage());
                // Sleep for the renewal period and check it has been cancelled
                Thread.sleep(TimeUnit.SECONDS.toMillis(lifetime));
                assert renewalCount.get() == 0;
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testRenewalWithoutDA() throws Exception
    {
        StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
        sa.start();
        try
        {
            int lifetime = 3;
            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://service", lifetime);
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
            sa.register(service);

            // Add the listener for renewals
            final AtomicInteger registered = new AtomicInteger(0);
            final AtomicInteger deregistered = new AtomicInteger(0);
            ServiceNotificationListener listener = new ServiceNotificationListener()
            {
                public void serviceRegistered(ServiceNotificationEvent event)
                {
                    registered.incrementAndGet();
                }

                public void serviceDeregistered(ServiceNotificationEvent event)
                {
                    deregistered.incrementAndGet();
                }
            };
            UserAgent ua = SLP.newUserAgent(newSettings());
            ua.addServiceNotificationListener(listener);
            ua.start();
            try
            {
                // Since there is not a DA, the UA must be notified of service renewal
                // Sleep for the renewal to happen
                TimeUnit.SECONDS.sleep(lifetime);
                assert registered.get() == 1;

                // Sleep again, be sure the renewal keeps going
                TimeUnit.SECONDS.sleep(lifetime);
                assert registered.get() == 2;

                // Update the service; this should cancel the previous renewal and schedule a new one
                registered.set(0);
                sa.addAttributes(service.getServiceURL(), service.getLanguage(), Attributes.from("(a=1,2)"));

                // Wait for the notification of update to arrive
                Thread.sleep(500);
                assert registered.get() == 1;

                // Sleep for the renewal to happen
                TimeUnit.SECONDS.sleep(lifetime);
                assert registered.get() == 2;

                // Sleep again, be sure the renewal keeps going
                TimeUnit.SECONDS.sleep(lifetime);
                assert registered.get() == 3;

                // Update again
                registered.set(0);
                sa.removeAttributes(service.getServiceURL(), service.getLanguage(), Attributes.from("a"));

                // Wait for the notification of update to arrive
                Thread.sleep(500);
                assert deregistered.get() == 1;

                // Sleep for the renewal to happen
                TimeUnit.SECONDS.sleep(lifetime);
                assert registered.get() == 1;

                // Sleep again, be sure the renewal keeps going
                TimeUnit.SECONDS.sleep(lifetime);
                assert registered.get() == 2;

                // Remove the service
                registered.set(0);
                deregistered.set(0);
                sa.deregister(service.getServiceURL(), service.getLanguage());

                // Wait for the notification of removal to arrive
                Thread.sleep(500);
                assert deregistered.get() == 1;

                // Sleep for the renewal period and check it has been cancelled
                TimeUnit.SECONDS.sleep(lifetime);
                assert registered.get() == 0;
            }
            finally
            {
                ua.stop();
                ua.removeServiceNotificationListener(listener);
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testListenForDirectoryAgents() throws Exception
    {
        StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
        sa.start();
        assert sa.getDirectoryAgents().size() == 0;

        try
        {
            StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
            da.start();
            try
            {
                // Wait for the SA to get the DAAdvert from DAS
                Thread.sleep(500);

                assert sa.getDirectoryAgents().size() == 1;
            }
            finally
            {
                da.stop();
            }
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testDADiscoveryOnStartup() throws Exception
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.start();
        try
        {
            StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
            sa.start();
            try
            {
                List<DirectoryAgentInfo> das = sa.getDirectoryAgents();
                assert das != null;
                assert das.size() == 1;
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testRegistrationFailureNoLanguage() throws Exception
    {
        StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
        sa.start();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:http://ssat8", ServiceURL.LIFETIME_PERMANENT);
            ServiceInfo serviceInfo = new ServiceInfo(serviceURL, null, Scopes.DEFAULT, null);
            sa.register(serviceInfo);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getError() == ServiceLocationException.Error.INVALID_REGISTRATION;
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testRegistrationFailureNoLifetime() throws Exception
    {
        StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
        sa.start();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:http://ssat9", ServiceURL.LIFETIME_NONE);
            ServiceInfo serviceInfo = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, null);
            sa.register(serviceInfo);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getError() == ServiceLocationException.Error.INVALID_REGISTRATION;
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testRegistrationFailureNoScopesMatch() throws Exception
    {
        StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
        sa.setScopes(Scopes.from("scope"));
        sa.start();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:foo://bar");
            ServiceInfo serviceInfo = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.from("unsupported"), null);
            sa.register(serviceInfo);
            assert false;
        }
        catch (ServiceLocationException x)
        {
            assert x.getError() == ServiceLocationException.Error.SCOPE_NOT_SUPPORTED;
        }
        finally
        {
            sa.stop();
        }
    }

    @Test
    public void testMulticastFindServices() throws Exception
    {
        StandardServiceAgent sa = StandardServiceAgent.newInstance(newSettings());
        Scopes defaultScopes = Scopes.from("default");
        Scopes otherScopes = Scopes.from("other");
        sa.setScopes(Scopes.from("default", "other"));
        sa.start();
        try
        {
            String english = Locale.ENGLISH.getLanguage();
            ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://host");
            ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL1, english, defaultScopes, null);
            sa.register(serviceInfo1);

            String italian = Locale.ITALIAN.getLanguage();
            ServiceInfo serviceInfo2 = new ServiceInfo(serviceURL1, italian, defaultScopes, null);
            sa.register(serviceInfo2);

            ServiceURL serviceURL2 = new ServiceURL("service:jmx:ws://host");
            Attributes wsAttributes = Attributes.from("(port=80),(confidential=false)");
            ServiceInfo serviceInfo3 = new ServiceInfo(serviceURL2, english, otherScopes, wsAttributes);
            sa.register(serviceInfo3);

            assert sa.getServices().size() == 3;

            UserAgentClient ua = SLP.newUserAgentClient(newSettings());

            // Search for all services
            ServiceType abstractServiceType = new ServiceType("service:jmx");
            ServiceType rmiServiceType = new ServiceType("jmx:rmi");
            List<ServiceInfo> services = ua.findServices(abstractServiceType, null, null, null);
            assert services.size() == 3;

            List<ServiceInfo> italianResult = ua.findServices(abstractServiceType, italian, null, null);
            assert italianResult.size() == 1;
            assert italianResult.get(0).getLanguage().equals(italian);

            List<ServiceInfo> rmiResult = ua.findServices(rmiServiceType, null, null, null);
            assert rmiResult.size() == 2;

            rmiResult = ua.findServices(rmiServiceType, english, null, null);
            assert rmiResult.size() == 1;

            List<ServiceInfo> wsResult = ua.findServices(serviceURL2.getServiceType(), null, null, null);
            assert wsResult.size() == 1;

            List<ServiceInfo> wrongScopesResult = ua.findServices(abstractServiceType, null, Scopes.from("wrong"), null);
            assert wrongScopesResult.size() == 0;

            List<ServiceInfo> wsScopesResult = ua.findServices(abstractServiceType, null, otherScopes, null);
            assert wsScopesResult.size() == 1;

            List<ServiceInfo> attrsResult = ua.findServices(abstractServiceType, null, null, "(&(confidential=false)(port=80))");
            assert attrsResult.size() == 1;

            attrsResult = ua.findServices(abstractServiceType, null, null, "(port=80)");
            assert attrsResult.size() == 1;

            attrsResult = ua.findServices(abstractServiceType, null, null, "(port=81)");
            assert attrsResult.size() == 0;

            List<ServiceInfo> germanResult = ua.findServices(abstractServiceType, Locale.GERMAN.getLanguage(), null, null);
            assert germanResult.size() == 0;
        }
        finally
        {
            sa.stop();
        }
    }
}
