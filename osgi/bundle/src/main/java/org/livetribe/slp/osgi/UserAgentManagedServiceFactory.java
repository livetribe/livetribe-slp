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
package org.livetribe.slp.osgi;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import org.livetribe.slp.SLP;
import org.livetribe.slp.osgi.util.DictionarySettings;
import org.livetribe.slp.sa.IServiceAgent;
import org.livetribe.slp.ua.UserAgent;


/**
 * Instances of {@link UserAgent} can be created and configured using OSGi's
 * Configuration Admin Service.
 *
 * @version $Revision$ $Date$
 * @see ManagedServiceFactory
 * @see UserAgent
 */
public class UserAgentManagedServiceFactory implements ManagedServiceFactory
{
    private final static String CLASS_NAME = UserAgentManagedServiceFactory.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Map<String, ServiceRegistration> serviceAgents = new HashMap<String, ServiceRegistration>();
    private final BundleContext bundleContext;
    private final String name;

    /**
     * Create a <code>UserAgentManagedServiceFactory</code> which will be used to
     * manage and configure instances of {@link UserAgent} using OSGi's
     * Configuration Admin Service.
     *
     * @param bundleContext The OSGi {@link BundleContext} used to register OSGi service instances.
     * @param name          The name for the factory.
     */
    public UserAgentManagedServiceFactory(BundleContext bundleContext, String name)
    {
        if (bundleContext == null) throw new IllegalArgumentException("Bundle context cannot be null");
        if (name == null) throw new IllegalArgumentException("Name cannot be null");

        this.bundleContext = bundleContext;
        this.name = name;

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("bundleContext: " + bundleContext);
            LOGGER.config("name: " + name);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }

    /**
     * Update the SLP user agent's configuration, unregistering from the
     * OSGi service registry and stopping it if it had already started.  The
     * new SLP user agent will be started with the new configuration and
     * registered in the OSGi service registry using the configuration
     * parameters as service properties.
     *
     * @param pid        The PID for this configuration.
     * @param dictionary The dictionary used to configure the SLP user agent.
     * @throws ConfigurationException Thrown if an error occurs during the SLP user agent's configuration.
     */
    public void updated(String pid, Dictionary dictionary) throws ConfigurationException
    {
        LOGGER.entering(CLASS_NAME, "updated", new Object[]{pid, dictionary});

        deleted(pid);

        UserAgent userAgent = SLP.newUserAgent(dictionary == null ? null : DictionarySettings.from(dictionary));
        userAgent.start();

        ServiceRegistration serviceRegistration = bundleContext.registerService(IServiceAgent.class.getName(), userAgent, dictionary);
        serviceAgents.put(pid, serviceRegistration);

        LOGGER.exiting(CLASS_NAME, "updated");
    }

    /**
     * Remove the SLP user agent identified by a pid that it was associated
     * with when it was first created.
     *
     * @param pid The PID for the SLP user agent to be removed.
     */
    public void deleted(String pid)
    {
        LOGGER.entering(CLASS_NAME, "deleted", pid);

        ServiceRegistration serviceRegistration = serviceAgents.remove(pid);
        if (serviceRegistration != null)
        {
            ServiceReference serviceReference = serviceRegistration.getReference();
            UserAgent userAgent = (UserAgent)bundleContext.getService(serviceReference);

            serviceRegistration.unregister();
            userAgent.stop();
        }

        LOGGER.exiting(CLASS_NAME, "deleted");
    }
}