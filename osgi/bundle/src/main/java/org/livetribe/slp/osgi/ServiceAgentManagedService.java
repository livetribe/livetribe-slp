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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import org.livetribe.slp.SLP;
import org.livetribe.slp.osgi.util.DictionarySettings;
import org.livetribe.slp.sa.IServiceAgent;
import org.livetribe.slp.sa.ServiceAgent;


/**
 * Instances of {@link ServiceAgent} can be created and configured using OSGi's
 * Configuration Admin Service.
 * <p/>
 * A default {@link ServiceAgent} instance can be created at construction which
 * can be replaced when the OSGi service's configuration is updated or no
 * {@link ServiceAgent} instance until the service's configuration is updated.
 *
 * @version $Revision$ $Date$
 * @see ManagedService
 * @see ServiceAgent
 */
public class ServiceAgentManagedService implements ManagedService
{
    private final static String CLASS_NAME = ServiceAgentManagedService.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Object lock = new Object();
    private final BundleContext bundleContext;
    private ServiceAgent serviceAgent;
    private ServiceRegistration serviceRegistration;

    /**
     * Create a <code>ServiceAgentManagedService</code> which will be used to
     * manage and configure instances of {@link ServiceAgent} using OSGi's
     * Configuration Admin Service.
     *
     * @param bundleContext The OSGi {@link BundleContext} used to register OSGi service instances.
     * @param startDefault  Start a default instance of {@link ServiceAgent} if <code>true</code> or wait until the service's configuration is updated otherwise.
     */
    public ServiceAgentManagedService(BundleContext bundleContext, boolean startDefault)
    {
        if (bundleContext == null) throw new IllegalArgumentException("Bundle context cannot be null");
        this.bundleContext = bundleContext;

        if (startDefault)
        {
            if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("Server " + this + " starting...");

            serviceAgent = SLP.newServiceAgent(null);
            serviceAgent.start();

            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Server " + this + " started successfully");

            serviceRegistration = bundleContext.registerService(IServiceAgent.class.getName(), serviceAgent, null);
        }

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("bundleContext: " + bundleContext);
            LOGGER.config("startDefault: " + startDefault);
        }
    }

    /**
     * Update the SLP service agent's configuration, unregistering from the
     * OSGi service registry and stopping it if it had already started.  The
     * new SLP service agent will be started with the new configuration and
     * registered in the OSGi service registry using the configuration
     * parameters as service properties.
     *
     * @param dictionary The dictionary used to configure the SLP service agent.
     * @throws ConfigurationException Thrown if an error occurs during the SLP service agent's configuration.
     */
    public void updated(Dictionary dictionary) throws ConfigurationException
    {
        LOGGER.entering(CLASS_NAME, "updated", dictionary);

        synchronized (lock)
        {
            if (serviceAgent != null)
            {
                serviceRegistration.unregister();

                if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("Server " + this + " stopping...");

                serviceAgent.stop();

                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Server " + this + " stopped successfully");
            }

            serviceAgent = SLP.newServiceAgent(dictionary == null ? null : DictionarySettings.from(dictionary));

            if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("Server " + this + " starting...");

            serviceAgent.start();

            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Server " + this + " started successfully");

            serviceRegistration = bundleContext.registerService(IServiceAgent.class.getName(), serviceAgent, dictionary);
        }

        LOGGER.exiting(CLASS_NAME, "updated");
    }
}
