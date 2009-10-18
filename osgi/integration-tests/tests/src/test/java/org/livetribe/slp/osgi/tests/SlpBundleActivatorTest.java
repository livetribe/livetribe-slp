/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import org.ops4j.pax.exam.Inject;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLP;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.osgi.ByServiceInfoServiceTracker;
import org.livetribe.slp.osgi.ByServicePropertiesServiceTracker;
import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.sa.ServiceNotificationEvent;
import org.livetribe.slp.sa.ServiceNotificationListener;
import static org.livetribe.slp.settings.Keys.PORT_KEY;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.ua.UserAgent;


/**
 * @version $Revision$ $Date$
 */
@RunWith(JUnit4TestRunner.class)
public class SlpBundleActivatorTest
{
    @Inject
    private BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure()
    {
        return options(
                //                equinox(),
                //                felix(),
                //                knopflerfish(),
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
        assertThat(bundleContext, is(notNullValue()));

        Bundle bundle = bundleContext.getBundle();
        System.out.println("symbolic name " + bundle.getSymbolicName());
        System.out.println("vendor " + bundleContext.getProperty(Constants.FRAMEWORK_VENDOR));

        for (Bundle b : bundleContext.getBundles())
        {
            System.out.println(b.getSymbolicName() + " " + b.getBundleId());
        }
    }

    @Test
    public void testByServicePropertiesServiceTracker() throws Exception
    {
        ClassLoader saved = Thread.currentThread().getContextClassLoader();

        try
        {
            Thread.currentThread().setContextClassLoader(SLP.class.getClassLoader());

            assertThat(bundleContext, is(notNullValue()));

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
        finally
        {
            Thread.currentThread().setContextClassLoader(saved);
        }
    }

    @Test
    public void testByServiceInfoServiceTracker() throws Exception
    {
        ClassLoader saved = Thread.currentThread().getContextClassLoader();

        try
        {
            Thread.currentThread().setContextClassLoader(SLP.class.getClassLoader());

            assertThat(bundleContext, is(notNullValue()));

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
        finally
        {
            Thread.currentThread().setContextClassLoader(saved);
        }
    }

    private Settings newSettings()
    {
        Settings settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        return settings;
    }
}
