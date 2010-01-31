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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.livetribe.slp.sa.ServiceNotificationListener;
import org.livetribe.slp.ua.UserAgent;


/**
 * The <code>ServiceNotificationListenerServiceTracker</code> simplifies the
 * management of SLP service notification listeners by leveraging the
 * OSGi service registry mechanism. This has the added benefit of automatic
 * deregistration of the listeners should the bundle become unresolved.
 * <p/>
 * Bundles wishing to register an SLP service notification listeners
 * merely need to register an instance of {@link ServiceNotificationListener}
 * in the OSGi service registry.
 * <p/>
 * A {@link Filter} can be passed to the constructor to narrow which instances
 * of {@link ServiceNotificationListener} are registered.
 *
 * @version $Revision$ $Date$
 * @see UserAgent
 * @see ServiceNotificationListener
 */
public class ServiceNotificationListenerServiceTracker
{
    private final static String CLASS_NAME = ServiceNotificationListenerServiceTracker.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final ServiceTracker tracker;

    /**
     * Create a <code>ServiceNotificationListenerServiceTracker</code> which will be
     * used to automatically register instances of {@link ServiceNotificationListener}
     * to a specific {@link UserAgent}.
     *
     * @param context   The OSGi {@link BundleContext} used to obtain OSGi service instances.
     * @param userAgent The {@link UserAgent} that service notification listeners will be registered to.
     */
    public ServiceNotificationListenerServiceTracker(final BundleContext context, final UserAgent userAgent)
    {
        if (context == null) throw new IllegalArgumentException("Bundle context cannot be null");
        if (userAgent == null) throw new IllegalArgumentException("Directory agent notifier cannot be null");

        try
        {
            tracker = generateServiceTracker(context, context.createFilter("(objectClass=" + ServiceNotificationListener.class.getName() + ")"), userAgent);
        }
        catch (InvalidSyntaxException ise)
        {
            LOGGER.log(Level.WARNING, "Oddly this caused an invalid syntax exception", ise);
            throw new IllegalStateException("Oddly this caused an invalid syntax exception");
        }

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("context: " + context);
            LOGGER.config("userAgent: " + userAgent);
        }
    }

    /**
     * Create a <code>ServiceNotificationListenerServiceTracker</code> which will be
     * used to automatically register instances of {@link ServiceNotificationListener}
     * to a specific {@link UserAgent}.
     *
     * @param context   The OSGi {@link BundleContext} used to obtain OSGi service instances.
     * @param filter    The {@link Filter} used to narrow which instances of {@link ServiceNotificationListener} are registered.
     * @param userAgent The {@link UserAgent} that service notification listeners will be registered to.
     */
    public ServiceNotificationListenerServiceTracker(final BundleContext context, Filter filter, final UserAgent userAgent)
    {
        if (context == null) throw new IllegalArgumentException("Bundle context cannot be null");
        if (filter == null) throw new IllegalArgumentException("Bundle context cannot be null");
        if (userAgent == null) throw new IllegalArgumentException("Directory agent notifier cannot be null");

        try
        {
            tracker = generateServiceTracker(context, context.createFilter("(&(objectClass=" + ServiceNotificationListener.class.getName() + ")" + filter + ")"), userAgent);
        }
        catch (InvalidSyntaxException ise)
        {
            LOGGER.log(Level.WARNING, "Oddly this caused an invalid syntax exception", ise);
            throw new IllegalStateException("Oddly this caused an invalid syntax exception");
        }

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("context: " + context);
            LOGGER.config("filter: " + filter);
            LOGGER.config("userAgent: " + userAgent);
        }
    }

    /**
     * Return the number of {@link ServiceNotificationListener} instances being tracked by this <code>ServiceTracker</code>.
     *
     * @return The number of listeners being tracked.
     */
    public int size()
    {
        return tracker.size();
    }

    /**
     * Open this <code>ServiceTracker</code> and begin tracking instances of {@link ServiceNotificationListener}.
     */
    public void open()
    {
        tracker.open();
    }


    /**
     * Close this <code>ServiceTracker</code>.
     * <p/>
     * This method should be called when this <code>ServiceTracker</code> should
     * end the tracking instances of {@link ServiceNotificationListener}.
     */
    public void close()
    {
        tracker.close();
    }

    private ServiceTracker generateServiceTracker(final BundleContext context, Filter filter, final UserAgent userAgent)
    {
        return new ServiceTracker(context,
                                  filter,
                                  new ServiceTrackerCustomizer()
                                  {
                                      public Object addingService(ServiceReference reference)
                                      {
                                          LOGGER.entering(CLASS_NAME, "addingService", reference);

                                          ServiceNotificationListener listener = (ServiceNotificationListener)context.getService(reference);

                                          userAgent.addServiceNotificationListener(listener);

                                          LOGGER.exiting(CLASS_NAME, "addingService", listener);

                                          return listener;
                                      }

                                      public void modifiedService(ServiceReference reference, Object service)
                                      {
                                      }

                                      public void removedService(ServiceReference reference, Object listener)
                                      {
                                          LOGGER.entering(CLASS_NAME, "removedService", new Object[]{reference, listener});

                                          context.ungetService(reference);
                                          userAgent.removeServiceNotificationListener((ServiceNotificationListener)listener);

                                          LOGGER.exiting(CLASS_NAME, "removedService");
                                      }
                                  });
    }
}
