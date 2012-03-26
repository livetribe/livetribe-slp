/**
 *
 * Copyright 2009 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.osgi.tests;

import javax.inject.Inject;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.livetribe.slp.settings.Keys.PORT_KEY;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.knopflerfish;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLP;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.da.DirectoryAgentEvent;
import org.livetribe.slp.da.DirectoryAgentListener;
import org.livetribe.slp.da.StandardDirectoryAgentServer;
import org.livetribe.slp.osgi.ByServiceInfoServiceTracker;
import org.livetribe.slp.osgi.ByServicePropertiesServiceTracker;
import org.livetribe.slp.osgi.DirectoryAgentListenerServiceTracker;
import org.livetribe.slp.osgi.ServiceNotificationListenerServiceTracker;
import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.sa.ServiceNotificationEvent;
import org.livetribe.slp.sa.ServiceNotificationListener;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.ua.UserAgent;


/**
 * @version $Revision$ $Date$
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class SlpBundleTest
{
    @Inject
    private BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure()
    {
        return options(
                junitBundles(),
                equinox(),
                felix(),
                knopflerfish(),
//                papoose(),
                provision(
                        mavenBundle().groupId("org.livetribe.slp").artifactId("livetribe-slp-osgi").version(asInProject())
                )
                // vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
                // this is necessary to let junit runner not timout the remote process before attaching debugger
                // setting timeout to 0 means wait as long as the remote service comes available.
                // starting with version 0.5.0 of PAx Exam this is no longer required as by default the framework tests
                // will not be triggered till the framework is not started
                // waitForFrameworkStartup()
        );
    }

    @Test
    public void testOSGi() throws Exception
    {
        Assert.assertNotNull(bundleContext);

        Bundle bundle = bundleContext.getBundle();
        System.out.println("symbolic name " + bundle.getSymbolicName());
        System.out.println("vendor " + bundleContext.getProperty(Constants.FRAMEWORK_VENDOR));

        for (Bundle b : bundleContext.getBundles())
        {
            System.out.println(b.getSymbolicName() + " " + b.getBundleId());
        }
    }

    @Test
    public void testPrivatePackage() throws Exception
    {
        try
        {
            getClass().getClassLoader().loadClass("org.livetribe.slp.util.Listeners");
            Assert.fail("Class should not have been loaded");
        }
        catch (ClassNotFoundException ignore)
        {
        }

        try
        {
            getClass().getClassLoader().loadClass("org.livetribe.slp.osgi.util.DictionarySettings");
            Assert.fail("Class should not have been loaded");
        }
        catch (ClassNotFoundException ignore)
        {
        }

        getClass().getClassLoader().loadClass("org.livetribe.slp.spi.DirectoryAgentNotifier");
    }

    @Test
    public void testByServicePropertiesServiceTracker() throws Exception
    {
        ServiceAgent serviceAgent = SLP.newServiceAgent(newSettings());
        UserAgent userAgent = SLP.newUserAgent(newSettings());
        final AtomicInteger counter = new AtomicInteger();

        userAgent.addServiceNotificationListener(new ServiceNotificationListener()
        {
            public void serviceRegistered(ServiceNotificationEvent event)
            {
                if ("service:printer:lpr://myprinter:1234/myqueue".equals(event.getService().getServiceURL().getURL())) counter.incrementAndGet();
            }

            public void serviceDeregistered(ServiceNotificationEvent event)
            {
                if ("service:printer:lpr://myprinter:1234/myqueue".equals(event.getService().getServiceURL().getURL())) counter.decrementAndGet();
            }
        });

        serviceAgent.start();
        userAgent.start();

        ByServicePropertiesServiceTracker tracker = new ByServicePropertiesServiceTracker(bundleContext, serviceAgent);
        tracker.open();

        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("slp.url", "service:printer:lpr://myprinter:${port}/myqueue");
        dictionary.put("port", "1234");

        ServiceRegistration serviceRegistration = bundleContext.registerService(getClass().getName(),
                                                                                this,
                                                                                dictionary);

        Thread.sleep(500);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(1, counter.get());

        serviceRegistration.unregister();

        Thread.sleep(500);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        tracker.close();

        userAgent.stop();
        serviceAgent.stop();
    }

    @Test
    public void testByServiceInfoServiceTracker() throws Exception
    {
        ServiceAgent serviceAgent = SLP.newServiceAgent(newSettings());
        UserAgent userAgent = SLP.newUserAgent(newSettings());
        final AtomicInteger counter = new AtomicInteger();

        userAgent.addServiceNotificationListener(new ServiceNotificationListener()
        {
            public void serviceRegistered(ServiceNotificationEvent event)
            {
                counter.incrementAndGet();
            }

            public void serviceDeregistered(ServiceNotificationEvent event)
            {
                counter.decrementAndGet();
            }
        });

        serviceAgent.start();
        userAgent.start();

        ByServiceInfoServiceTracker tracker = new ByServiceInfoServiceTracker(bundleContext, serviceAgent);
        tracker.open();

        ServiceRegistration serviceRegistration = bundleContext.registerService(ServiceInfo.class.getName(),
                                                                                new ServiceInfo(new ServiceURL("service:printer:lpr://myprinter/myqueue"),
                                                                                                Locale.ENGLISH.getLanguage(),
                                                                                                Scopes.DEFAULT,
                                                                                                Attributes.from("(printer-compression-supported=deflate, gzip)")),
                                                                                null);

        Thread.sleep(500);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(1, counter.get());

        serviceRegistration.unregister();

        Thread.sleep(500);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        tracker.close();

        userAgent.stop();
        serviceAgent.stop();
    }

    @Test
    public void testDirectoryAgentListenerServiceTracker() throws Exception
    {
        UserAgent userAgent = SLP.newUserAgent(newSettings());
        StandardDirectoryAgentServer directoryAgentServer = StandardDirectoryAgentServer.newInstance(newSettings());
        final AtomicInteger counter = new AtomicInteger();

        userAgent.start();

        DirectoryAgentListenerServiceTracker tracker = new DirectoryAgentListenerServiceTracker(bundleContext, userAgent);
        tracker.open();

        ServiceRegistration serviceRegistration = bundleContext.registerService(DirectoryAgentListener.class.getName(),
                                                                                new DirectoryAgentListener()
                                                                                {
                                                                                    public void directoryAgentBorn(DirectoryAgentEvent event)
                                                                                    {
                                                                                        counter.incrementAndGet();
                                                                                    }

                                                                                    public void directoryAgentDied(DirectoryAgentEvent event)
                                                                                    {
                                                                                        counter.decrementAndGet();
                                                                                    }
                                                                                },
                                                                                null);

        directoryAgentServer.start();

        Thread.sleep(1000);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(1, counter.get());

        directoryAgentServer.stop();

        Thread.sleep(1000);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(0, counter.get());

        serviceRegistration.unregister();

        Thread.sleep(500);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        tracker.close();

        directoryAgentServer.stop();
        userAgent.stop();
    }

    @Test
    public void testDirectoryAgentListenerWithFilterServiceTracker() throws Exception
    {
        UserAgent userAgent = SLP.newUserAgent(newSettings());
        StandardDirectoryAgentServer directoryAgentServer = StandardDirectoryAgentServer.newInstance(newSettings());
        final AtomicInteger counter = new AtomicInteger();

        userAgent.start();

        DirectoryAgentListenerServiceTracker tracker = new DirectoryAgentListenerServiceTracker(bundleContext, bundleContext.createFilter("(&(foo=bar)(car=cdr))"), userAgent);
        tracker.open();

        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("foo", "bar");
        dictionary.put("car", "cdr");
        ServiceRegistration serviceRegistration = bundleContext.registerService(DirectoryAgentListener.class.getName(),
                                                                                new DirectoryAgentListener()
                                                                                {
                                                                                    public void directoryAgentBorn(DirectoryAgentEvent event)
                                                                                    {
                                                                                        counter.incrementAndGet();
                                                                                    }

                                                                                    public void directoryAgentDied(DirectoryAgentEvent event)
                                                                                    {
                                                                                        counter.decrementAndGet();
                                                                                    }
                                                                                },
                                                                                dictionary);

        directoryAgentServer.start();

        Thread.sleep(1000);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(1, counter.get());

        directoryAgentServer.stop();

        Thread.sleep(1000);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(0, counter.get());

        serviceRegistration.unregister();

        Thread.sleep(500);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        dictionary.put("foo", "bad-value");
        serviceRegistration = bundleContext.registerService(DirectoryAgentListener.class.getName(),
                                                            new DirectoryAgentListener()
                                                            {
                                                                public void directoryAgentBorn(DirectoryAgentEvent event)
                                                                {
                                                                    counter.incrementAndGet();
                                                                }

                                                                public void directoryAgentDied(DirectoryAgentEvent event)
                                                                {
                                                                    counter.decrementAndGet();
                                                                }
                                                            },
                                                            dictionary);

        directoryAgentServer.start();

        Thread.sleep(1000);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        directoryAgentServer.stop();

        Thread.sleep(1000);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        serviceRegistration.unregister();

        Thread.sleep(500);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        tracker.close();

        directoryAgentServer.stop();
        userAgent.stop();
    }

    @Test
    public void testServiceNotificationListenerServiceTracker() throws Exception
    {
        UserAgent userAgent = SLP.newUserAgent(newSettings());
        ServiceAgent serviceAgent = SLP.newServiceAgent(newSettings());
        final AtomicInteger counter = new AtomicInteger();

        userAgent.start();

        ServiceNotificationListenerServiceTracker tracker = new ServiceNotificationListenerServiceTracker(bundleContext, userAgent);
        tracker.open();

        ServiceRegistration serviceRegistration = bundleContext.registerService(ServiceNotificationListener.class.getName(),
                                                                                new ServiceNotificationListener()
                                                                                {
                                                                                    public void serviceRegistered(ServiceNotificationEvent event)
                                                                                    {
                                                                                        if ("service:jmx:rmi:///jndi/jmxrmi".equals(event.getService().getServiceURL().getURL()))
                                                                                        {
                                                                                            counter.incrementAndGet();
                                                                                        }
                                                                                    }

                                                                                    public void serviceDeregistered(ServiceNotificationEvent event)
                                                                                    {
                                                                                        if ("service:jmx:rmi:///jndi/jmxrmi".equals(event.getService().getServiceURL().getURL()))
                                                                                        {
                                                                                            counter.decrementAndGet();
                                                                                        }
                                                                                    }
                                                                                },
                                                                                null);

        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/jmxrmi");
        ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=string),(d=\\FF\\00),e"));

        serviceAgent.start();
        serviceAgent.register(service);

        Thread.sleep(1000);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(1, counter.get());

        serviceAgent.deregister(service.getServiceURL(), service.getLanguage());

        Thread.sleep(1000);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(0, counter.get());

        serviceAgent.stop();
        serviceRegistration.unregister();

        Thread.sleep(500);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        tracker.close();

        serviceAgent.stop();
        userAgent.stop();
    }

    @Test
    public void testServiceNotificationListenerWithFilterServiceTracker() throws Exception
    {
        UserAgent userAgent = SLP.newUserAgent(newSettings());
        ServiceAgent serviceAgent = SLP.newServiceAgent(newSettings());
        final AtomicInteger counter = new AtomicInteger();

        userAgent.start();
        serviceAgent.start();

        ServiceNotificationListenerServiceTracker tracker = new ServiceNotificationListenerServiceTracker(bundleContext, bundleContext.createFilter("(&(foo=bar)(car=cdr))"), userAgent);
        tracker.open();

        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("foo", "bar");
        dictionary.put("car", "cdr");
        ServiceRegistration serviceRegistration = bundleContext.registerService(ServiceNotificationListener.class.getName(),
                                                                                new ServiceNotificationListener()
                                                                                {
                                                                                    public void serviceRegistered(ServiceNotificationEvent event)
                                                                                    {
                                                                                        if ("service:jmx:rmi:///jndi/jmxrmi".equals(event.getService().getServiceURL().getURL()))
                                                                                        {
                                                                                            counter.incrementAndGet();
                                                                                        }
                                                                                    }

                                                                                    public void serviceDeregistered(ServiceNotificationEvent event)
                                                                                    {
                                                                                        if ("service:jmx:rmi:///jndi/jmxrmi".equals(event.getService().getServiceURL().getURL()))
                                                                                        {
                                                                                            counter.decrementAndGet();
                                                                                        }
                                                                                    }
                                                                                },
                                                                                dictionary);

        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/jmxrmi");
        ServiceInfo service = new ServiceInfo(serviceURL, Locale.ENGLISH.getLanguage(), Scopes.DEFAULT, Attributes.from("(a=1,2),(b=true),(c=string),(d=\\FF\\00),e"));

        serviceAgent.register(service);

        Thread.sleep(1000);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(1, counter.get());

        serviceAgent.deregister(service.getServiceURL(), service.getLanguage());

        Thread.sleep(1000);

        Assert.assertEquals(1, tracker.size());
        Assert.assertEquals(0, counter.get());

        serviceRegistration.unregister();

        Thread.sleep(500);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        dictionary.put("foo", "bad-value");
        serviceRegistration = bundleContext.registerService(ServiceNotificationListener.class.getName(),
                                                            new ServiceNotificationListener()
                                                            {
                                                                public void serviceRegistered(ServiceNotificationEvent event)
                                                                {
                                                                    if ("service:jmx:rmi:///jndi/jmxrmi".equals(event.getService().getServiceURL().getURL()))
                                                                    {
                                                                        counter.incrementAndGet();
                                                                    }
                                                                }

                                                                public void serviceDeregistered(ServiceNotificationEvent event)
                                                                {
                                                                    if ("service:jmx:rmi:///jndi/jmxrmi".equals(event.getService().getServiceURL().getURL()))
                                                                    {
                                                                        counter.decrementAndGet();
                                                                    }
                                                                }
                                                            },
                                                            dictionary);

        serviceAgent.register(service);

        Thread.sleep(1000);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        serviceAgent.deregister(service.getServiceURL(), service.getLanguage());

        Thread.sleep(1000);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        serviceRegistration.unregister();

        Thread.sleep(500);

        Assert.assertEquals(0, tracker.size());
        Assert.assertEquals(0, counter.get());

        tracker.close();

        serviceAgent.stop();
        userAgent.stop();
    }

    private Settings newSettings()
    {
        Settings settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        return settings;
    }
}
