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
package org.livetribe.slp.da;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLP;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.sa.ServiceAgentClient;
import org.livetribe.slp.sa.ServiceListener;
import org.livetribe.slp.sa.ServiceEvent;
import org.livetribe.slp.settings.Defaults;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.Factories;
import org.livetribe.slp.srv.MulticastDASrvRqstPerformer;
import org.livetribe.slp.srv.MulticastSrvRqstPerformer;
import org.livetribe.slp.srv.da.DirectoryAgentInfo;
import org.livetribe.slp.srv.msg.DAAdvert;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.msg.SrvRply;
import org.livetribe.slp.srv.net.MessageEvent;
import org.livetribe.slp.srv.net.MessageListener;
import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.srv.net.UDPConnectorServer;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class StandardDirectoryAgentServerTest
{
    private Settings newSettings()
    {
        Defaults.reload();
        Settings settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        return settings;
    }

    @Test
    public void testStartStop() throws Exception
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        assert !da.isRunning();
        da.start();
        assert da.isRunning();
        da.stop();
        assert !da.isRunning();
    }

    @Test
    public void testDAAdvertOnBoot() throws Exception
    {
        final AtomicInteger daAdvertCount = new AtomicInteger(0);
        final AtomicBoolean failure = new AtomicBoolean(false);
        MessageListener listener = new MessageListener()
        {
            public void handle(MessageEvent event)
            {
                try
                {
                    Message message = event.getMessage();
                    if (message.getMessageType() != Message.DA_ADVERT_TYPE) assert false;

                    daAdvertCount.incrementAndGet();
                    DAAdvert daAdvert = (DAAdvert)message;
                    assert daAdvert.getErrorCode() == 0;
                    assert daAdvert.isMulticast();
                    DirectoryAgentInfo directoryAgent = DirectoryAgentInfo.from(daAdvert);
                    assert !directoryAgent.isShuttingDown();
                    assert directoryAgent.getURL().startsWith(DirectoryAgentInfo.SERVICE_TYPE.asString());
                    assert directoryAgent.getHost() != null;
                    assert directoryAgent.getHost().trim().length() > 0;
                }
                catch (Throwable x)
                {
                    failure.set(true);
                }
            }
        };

        Settings udpSettings = newSettings();
        UDPConnectorServer udpConnectorServer = Factories.newInstance(udpSettings, UDP_CONNECTOR_SERVER_FACTORY_KEY).newUDPConnectorServer(udpSettings);
        udpConnectorServer.start();
        udpConnectorServer.addMessageListener(listener);
        try
        {
            StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
            da.start();
            try
            {
                // Wait past the boot DAAdvert
                Thread.sleep(500);
                udpConnectorServer.removeMessageListener(listener);
            }
            finally
            {
                da.stop();
            }

            assert daAdvertCount.get() == 1;
            assert !failure.get();
        }
        finally
        {
            udpConnectorServer.stop();
        }
    }

    @Test
    public void testDAAdvertOnShutdown() throws Exception
    {
        final AtomicInteger daAdvertCount = new AtomicInteger(0);
        final AtomicBoolean failure = new AtomicBoolean(false);
        MessageListener listener = new MessageListener()
        {
            public void handle(MessageEvent event)
            {
                try
                {
                    Message message = event.getMessage();
                    if (message.getMessageType() != Message.DA_ADVERT_TYPE) assert false;

                    daAdvertCount.incrementAndGet();
                    DAAdvert daAdvert = (DAAdvert)message;
                    assert daAdvert.getErrorCode() == 0;
                    assert daAdvert.isMulticast();
                    DirectoryAgentInfo directoryAgent = DirectoryAgentInfo.from(daAdvert);
                    assert directoryAgent.isShuttingDown();
                    assert directoryAgent.getURL().startsWith(DirectoryAgentInfo.SERVICE_TYPE.asString());
                    assert directoryAgent.getHost() != null;
                    assert directoryAgent.getHost().trim().length() > 0;
                }
                catch (Throwable x)
                {
                    failure.set(true);
                }
            }
        };

        Settings udpSettings = newSettings();
        UDPConnectorServer udpConnectorServer = Factories.newInstance(udpSettings, UDP_CONNECTOR_SERVER_FACTORY_KEY).newUDPConnectorServer(udpSettings);
        // Start with no MessageListeners
        udpConnectorServer.start();
        try
        {
            StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
            da.start();
            try
            {
                // Wait past the boot DAAdvert
                Thread.sleep(500);
                // Listen for DAAdverts from now on
                udpConnectorServer.addMessageListener(listener);
            }
            finally
            {
                da.stop();
            }

            // Wait for the shutdown DAAdvert
            Thread.sleep(500);
            udpConnectorServer.removeMessageListener(listener);
            assert daAdvertCount.get() == 1;
            assert !failure.get();
        }
        finally
        {
            udpConnectorServer.stop();
        }
    }

    @Test
    public void testMulticastUnsolicitedDAAdverts() throws Exception
    {
        testManycastUnsolicitedDAAdverts(newSettings());
    }

    @Test
    public void testBroadcastUnsolicitedDAAdverts() throws Exception
    {
        Settings settings = newSettings();
        settings.put(BROADCAST_ENABLED_KEY, true);
        testManycastUnsolicitedDAAdverts(settings);
    }

    private void testManycastUnsolicitedDAAdverts(Settings daSettings) throws Exception
    {
        final AtomicInteger daAdvertCount = new AtomicInteger(0);
        final AtomicBoolean failure = new AtomicBoolean(false);
        MessageListener listener = new MessageListener()
        {
            public void handle(MessageEvent event)
            {
                try
                {
                    Message message = event.getMessage();
                    if (message.getMessageType() != Message.DA_ADVERT_TYPE) assert false;

                    daAdvertCount.incrementAndGet();
                    DAAdvert daAdvert = (DAAdvert)message;
                    assert daAdvert.getErrorCode() == 0;
                    assert daAdvert.isMulticast();
                    DirectoryAgentInfo directoryAgent = DirectoryAgentInfo.from(daAdvert);
                    assert !directoryAgent.isShuttingDown();
                    assert directoryAgent.getURL().startsWith(DirectoryAgentInfo.SERVICE_TYPE.asString());
                    assert directoryAgent.getHost() != null;
                    assert directoryAgent.getHost().trim().length() > 0;
                }
                catch (Throwable x)
                {
                    failure.set(true);
                }
            }
        };

        Settings udpSettings = newSettings();
        UDPConnectorServer udpConnectorServer = Factories.newInstance(udpSettings, UDP_CONNECTOR_SERVER_FACTORY_KEY).newUDPConnectorServer(udpSettings);
        udpConnectorServer.addMessageListener(listener);
        udpConnectorServer.start();
        try
        {
            int advertisementPeriod = 1;
            long advertisementPeriodMillis = TimeUnit.SECONDS.toMillis(advertisementPeriod);
            daSettings.put(DA_ADVERTISEMENT_PERIOD_KEY, advertisementPeriod);
            StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(daSettings);
            da.start();
            try
            {
                int count = 3;
                Thread.sleep(advertisementPeriodMillis * count + (advertisementPeriodMillis / 2));
                udpConnectorServer.removeMessageListener(listener);

                // DAAdvert count: 1 sent at boot + count
                assert daAdvertCount.get() == 1 + count;
                assert !failure.get();
            }
            finally
            {
                da.stop();
            }
        }
        finally
        {
            udpConnectorServer.stop();
        }
    }

    /**
     * Tests that even if the DA is configured to not send unsolicited DAAdverts,
     * the boot and shutdown DAAdverts are sent anyway
     */
    @Test
    public void testNoUnsolicitedDAAdvertsSendsBootAndShutdownDAAdverts() throws Exception
    {
        final AtomicInteger daAdvertCount = new AtomicInteger(0);
        final AtomicBoolean failure = new AtomicBoolean(false);
        MessageListener listener = new MessageListener()
        {
            public void handle(MessageEvent event)
            {
                try
                {
                    Message message = event.getMessage();
                    if (message.getMessageType() != Message.DA_ADVERT_TYPE) assert false;
                    daAdvertCount.incrementAndGet();
                }
                catch (Throwable x)
                {
                    failure.set(true);
                }
            }
        };

        Settings udpSettings = newSettings();
        UDPConnectorServer udpConnectorServer = Factories.newInstance(udpSettings, UDP_CONNECTOR_SERVER_FACTORY_KEY).newUDPConnectorServer(udpSettings);
        udpConnectorServer.addMessageListener(listener);
        udpConnectorServer.start();
        try
        {
            // Disable unsolicited DAAdverts
            Settings daSettings = newSettings();
            int advertisementPeriod = 0;
            daSettings.put(DA_ADVERTISEMENT_PERIOD_KEY, advertisementPeriod);
            StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(daSettings);
            da.start();
            try
            {
                Thread.sleep(500);
                assert daAdvertCount.get() == 1;
                assert !failure.get();
            }
            finally
            {
                da.stop();
            }

            Thread.sleep(500);
            udpConnectorServer.removeMessageListener(listener);
            assert daAdvertCount.get() == 2;
            assert !failure.get();
        }
        finally
        {
            udpConnectorServer.stop();
        }
    }

    /**
     * Tests that DA reply to multicast SrvRqst only if they have
     * service type {@link DirectoryAgentInfo#SERVICE_TYPE}
     */
    @Test
    public void testReplyForMulticastSrvRqstOnlyForDASrvRqst() throws Exception
    {
        Settings settings = newSettings();
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(settings);
        da.start();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:abstract:concrete://service");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient registrar = SLP.newServiceAgentClient(settings);
            registrar.register(service);

            // Multicast SrvRqst is ignored by DA
            UDPConnector udpConnector = Factories.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY).newUDPConnector(settings);
            MulticastSrvRqstPerformer srvRqstPerformer = new MulticastSrvRqstPerformer(udpConnector, settings);
            List<SrvRply> srvRplys = srvRqstPerformer.perform(serviceURL.getServiceType(), null, null, null);
            assert srvRplys.isEmpty();

            // Multicast DASrvRqst is served by DA
            MulticastDASrvRqstPerformer daSrvRqstPerformer = new MulticastDASrvRqstPerformer(udpConnector, settings);
            List<DAAdvert> daAdverts = daSrvRqstPerformer.perform(null, null, null);
            assert !daAdverts.isEmpty();

            // Multicast DASrvRqst is ignored by DA when scopes do not match
            daAdverts = daSrvRqstPerformer.perform(Scopes.from("scope"), null, null);
            assert daAdverts.isEmpty();
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testServiceRegistration() throws Exception
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.start();
        assert da.getServices().isEmpty();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient registrar = SLP.newServiceAgentClient(newSettings());
            registrar.register(service);
            assert da.getServices().size() == 1;
            ServiceInfo registered = da.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getAttributes().equals(service.getAttributes());
            assert registered.getRegistrationTime() > 0;
            assert registered.getScopes().equals(service.getScopes());

            // Registration with wrong scopes must fail
            Scopes newScope = Scopes.from("scope");
            ServiceInfo newService = new ServiceInfo(service.getServiceURL(), service.getLanguage(), newScope, service.getAttributes());
            try
            {
                registrar.register(newService);
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.SCOPE_NOT_SUPPORTED;
            }

            // Registration with no language must fail
            newService = new ServiceInfo(service.getServiceURL(), null, service.getScopes(), service.getAttributes());
            try
            {
                registrar.register(newService);
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
            }

            // Registration with invalid lifetime must fail
            int newLifetime = ServiceURL.LIFETIME_NONE;
            ServiceURL newServiceURL = new ServiceURL(service.getServiceURL().getURL(), newLifetime);
            newService = new ServiceInfo(newServiceURL, service.getLanguage(), service.getScopes(), service.getAttributes());
            try
            {
                registrar.register(newService);
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testServiceUpdate() throws Exception
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.start();
        assert da.getServices().isEmpty();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
            ServiceAgentClient registrar = SLP.newServiceAgentClient(newSettings());
            registrar.register(service);
            assert da.getServices().size() == 1;
            ServiceInfo registered = da.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getScopes().equals(service.getScopes());
            assert registered.getAttributes().equals(service.getAttributes());

            // Update with same information must pass
            registrar.addAttributes(service.getServiceURL(), service.getLanguage(), service.getAttributes());
            assert da.getServices().size() == 1;
            registered = da.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getScopes().equals(service.getScopes());
            assert registered.getAttributes().equals(service.getAttributes());

            // Update with different attributes must pass
            Attributes newAttributes = service.getAttributes().merge(Attributes.from("(b=false),(f=1)"));
            registrar.addAttributes(service.getServiceURL(), service.getLanguage(), newAttributes);
            assert da.getServices().size() == 1;
            registered = da.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getScopes().equals(service.getScopes());
            assert registered.getAttributes().equals(newAttributes);

            // Removing attributes must pass
            newAttributes = Attributes.from("b");
            registrar.removeAttributes(service.getServiceURL(), service.getLanguage(), newAttributes);
            assert da.getServices().size() == 1;
            registered = da.getServices().get(0);
            assert registered.getKey().equals(service.getKey());
            assert registered.getScopes().equals(service.getScopes());
            assert !registered.getAttributes().containsTag("b");

            // Update with different language must fail
            String newLanguage = Locale.ITALIAN.getLanguage();
            try
            {
                registrar.addAttributes(service.getServiceURL(), newLanguage, service.getAttributes());
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.INVALID_UPDATE;
                assert da.getServices().size() == 1;
            }

            // Update with different ServiceURL must fail
            ServiceURL newServiceURL = new ServiceURL(service.getServiceURL().getURL() + ".new");
            try
            {
                registrar.addAttributes(newServiceURL, service.getLanguage(), service.getAttributes());
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.INVALID_UPDATE;
                assert da.getServices().size() == 1;
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testServiceDeregistration() throws Exception
    {
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(newSettings());
        da.start();
        assert da.getServices().isEmpty();
        try
        {
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi");
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=long string),(d=\\FF\\00),e"));
            ServiceAgentClient registrar = SLP.newServiceAgentClient(newSettings());
            registrar.register(service);
            assert da.getServices().size() == 1;

            // Deregistering must pass
            registrar.deregister(service.getServiceURL(), service.getLanguage());
            assert da.getServices().isEmpty();

            // Register again
            registrar.register(service);
            assert da.getServices().size() == 1;

            // Update with different language must fail
            String newLanguage = Locale.ITALIAN.getLanguage();
            try
            {
                registrar.removeAttributes(service.getServiceURL(), newLanguage, service.getAttributes());
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.INVALID_UPDATE;
                assert da.getServices().size() == 1;
            }

            // Update with different ServiceURL must fail
            ServiceURL newServiceURL = new ServiceURL(service.getServiceURL().getURL() + ".new");
            try
            {
                registrar.removeAttributes(newServiceURL, service.getLanguage(), service.getAttributes());
                assert false;
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.INVALID_UPDATE;
                assert da.getServices().size() == 1;
            }
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testExpiration() throws Exception
    {
        Settings daSettings = newSettings();
        daSettings.put(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY, 0); // Be sure the purger does not kick in
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(daSettings);
        da.start();
        try
        {
            int lifetime = 3; // seconds
            ServiceURL serviceURL = new ServiceURL("foo://baz", lifetime);
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient registrar = SLP.newServiceAgentClient(newSettings());
            registrar.register(service);
            assert da.getServices().size() == 1;

            // Wait for the service to expire
            Thread.sleep(TimeUnit.SECONDS.toMillis(lifetime) + 500);

            assert da.getServices().isEmpty();
        }
        finally
        {
            da.stop();
        }
    }

    @Test
    public void testPurgeExpiredServices() throws Exception
    {
        final AtomicInteger removedCount = new AtomicInteger(0);
        ServiceListener listener = new ServiceListener()
        {
            public void serviceAdded(ServiceEvent event)
            {
            }

            public void serviceUpdated(ServiceEvent event)
            {
            }

            public void serviceRemoved(ServiceEvent event)
            {
                removedCount.incrementAndGet();
            }
        };

        Settings daSettings = newSettings();
        daSettings.put(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY, 1);
        StandardDirectoryAgentServer da = StandardDirectoryAgentServer.newInstance(daSettings);
        da.addServiceListener(listener);
        da.start();
        try
        {
            int lifetime = 3; // seconds
            ServiceURL serviceURL = new ServiceURL("foo://baz", lifetime);
            ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.NONE);
            ServiceAgentClient registrar = SLP.newServiceAgentClient(newSettings());
            registrar.register(service);
            assert da.getServices().size() == 1;

            // Wait for the service to expire
            Thread.sleep(TimeUnit.SECONDS.toMillis(lifetime + daSettings.get(DA_EXPIRED_SERVICES_PURGE_PERIOD_KEY)));

            assert removedCount.get() == 1;
        }
        finally
        {
            da.removeServiceListener(listener);
            da.stop();
        }
    }
}
