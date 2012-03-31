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

import java.io.Closeable;
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
import org.livetribe.slp.sa.ServiceAgent;


/**
 * Instances of {@link ServiceAgent} can be created and configured using OSGi's
 * Configuration Admin Service.
 *
 * @see ManagedServiceFactory
 * @see ServiceAgent
 */
public class ServiceAgentManagedServiceFactory implements ManagedServiceFactory, Closeable
{
    private final static String CLASS_NAME = ServiceAgentManagedServiceFactory.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Map<String, ServiceRegistration> serviceAgents = new HashMap<String, ServiceRegistration>();
    private final BundleContext bundleContext;
    private final String name;

    /**
     * Create a <code>ServiceAgentManagedServiceFactory</code> which will be used to
     * manage and configure instances of {@link ServiceAgent} using OSGi's
     * Configuration Admin Service.
     *
     * @param bundleContext The OSGi {@link BundleContext} used to register OSGi service instances.
     * @param name          The name for the factory.
     */
    public ServiceAgentManagedServiceFactory(BundleContext bundleContext, String name)
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
     * Update the SLP service agent's configuration, unregistering from the
     * OSGi service registry and stopping it if it had already started.  The
     * new SLP service agent will be started with the new configuration and
     * registered in the OSGi service registry using the configuration
     * parameters as service properties.
     *
     * @param pid        The PID for this configuration.
     * @param dictionary The dictionary used to configure the SLP service agent.
     * @throws ConfigurationException Thrown if an error occurs during the SLP service agent's configuration.
     */
    public void updated(String pid, Dictionary dictionary) throws ConfigurationException
    {
        LOGGER.entering(CLASS_NAME, "updated", new Object[]{pid, dictionary});

        /**
         * This could be an update to an existing SLP service agent.  Delete
         * it first then we'll recreate it.
         */
        deleted(pid);

        ServiceAgent serviceAgent = SLP.newServiceAgent(dictionary == null ? null : DictionarySettings.from(dictionary));

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("Service Agent " + pid + " starting...");

        serviceAgent.start();

        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Service Agent " + pid + " started successfully");

        ServiceRegistration serviceRegistration = bundleContext.registerService(IServiceAgent.class.getName(), serviceAgent, dictionary);
        serviceAgents.put(pid, serviceRegistration);

        LOGGER.exiting(CLASS_NAME, "updated");
    }

    /**
     * Remove the SLP service agent identified by a pid that it was associated
     * with when it was first created.
     *
     * @param pid The PID for the SLP service agent to be removed.
     */
    public void deleted(String pid)
    {
        LOGGER.entering(CLASS_NAME, "deleted", pid);

        ServiceRegistration serviceRegistration = serviceAgents.remove(pid);
        if (serviceRegistration != null)
        {
            ServiceReference serviceReference = serviceRegistration.getReference();
            ServiceAgent serviceAgent = (ServiceAgent)bundleContext.getService(serviceReference);

            serviceRegistration.unregister();

            if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("Service Agent " + pid + " starting...");

            serviceAgent.stop();

            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Service Agent " + pid + " stopped successfully");
        }

        LOGGER.exiting(CLASS_NAME, "deleted");
    }

    /**
     * Close all the configured service agents.
     */
    public void close()
    {
        LOGGER.entering(CLASS_NAME, "close");

        for (String pid : serviceAgents.keySet())
        {
            deleted(pid);
        }

        LOGGER.exiting(CLASS_NAME, "close");
    }
}
