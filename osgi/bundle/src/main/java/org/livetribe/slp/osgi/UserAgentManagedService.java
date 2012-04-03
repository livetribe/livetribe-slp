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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import org.livetribe.slp.SLP;
import org.livetribe.slp.osgi.util.DictionarySettings;
import org.livetribe.slp.spi.ua.IUserAgent;
import org.livetribe.slp.ua.UserAgent;


/**
 * Instances of {@link UserAgent} can be created and configured using OSGi's
 * Configuration Admin Service.
 * <p/>
 * A default {@link UserAgent} instance can be created at construction which
 * can be replaced when the OSGi service's configuration is updated or no
 * {@link UserAgent} instance until the service's configuration is updated.
 *
 * @see ManagedService
 * @see UserAgent
 */
public class UserAgentManagedService implements ManagedService, Closeable
{
    private final static String CLASS_NAME = UserAgentManagedService.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Object lock = new Object();
    private final BundleContext bundleContext;
    private UserAgent userAgent;
    private ServiceRegistration serviceRegistration;

    /**
     * Create a <code>UserAgentManagedService</code> which will be used to
     * manage and configure instances of {@link UserAgent} using OSGi's
     * Configuration Admin Service.
     *
     * @param bundleContext The OSGi {@link BundleContext} used to register OSGi service instances.
     * @param startDefault  Start a default instance of {@link UserAgent} if <code>true</code> or wait until the service's configuration is updated otherwise.
     */
    public UserAgentManagedService(BundleContext bundleContext, boolean startDefault)
    {
        if (bundleContext == null) throw new IllegalArgumentException("Bundle context cannot be null");
        this.bundleContext = bundleContext;

        if (startDefault)
        {
            userAgent = SLP.newUserAgent(null);

            if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("User Agent " + this + " starting...");

            userAgent.start();

            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("User Agent " + this + " started successfully");

            serviceRegistration = bundleContext.registerService(IUserAgent.class.getName(), userAgent, null);
        }

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("bundleContext: " + bundleContext);
            LOGGER.config("startDefault: " + startDefault);
        }
    }

    /**
     * Update the SLP user agent's configuration, unregistering from the
     * OSGi service registry and stopping it if it had already started.  The
     * new SLP user agent will be started with the new configuration and
     * registered in the OSGi service registry using the configuration
     * parameters as service properties.
     *
     * @param dictionary The dictionary used to configure the SLP user agent.
     * @throws ConfigurationException Thrown if an error occurs during the SLP user agent's configuration.
     */
    public void updated(Dictionary dictionary) throws ConfigurationException
    {
        LOGGER.entering(CLASS_NAME, "updated", dictionary);

        synchronized (lock)
        {
            if (userAgent != null)
            {
                serviceRegistration.unregister();

                if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("User Agent " + this + " stopping...");

                userAgent.stop();

                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("User Agent " + this + " stopped successfully");
            }

            userAgent = SLP.newUserAgent(dictionary == null ? null : DictionarySettings.from(dictionary));

            if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("User Agent " + this + " starting...");

            userAgent.start();

            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("User Agent " + this + " started successfully");

            serviceRegistration = bundleContext.registerService(IUserAgent.class.getName(), userAgent, dictionary);
        }

        LOGGER.exiting(CLASS_NAME, "updated");
    }

    /**
     * Shuts down the user agent if one exists.
     */
    public void close()
    {
        LOGGER.entering(CLASS_NAME, "close");

        synchronized (lock)
        {
            if (userAgent != null)
            {
                serviceRegistration.unregister();

                if (LOGGER.isLoggable(Level.FINER)) LOGGER.finer("User Agent " + this + " stopping...");

                userAgent.stop();

                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("User Agent " + this + " stopped successfully");
            }
        }

        LOGGER.exiting(CLASS_NAME, "close");
    }
}