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

import org.livetribe.slp.da.DirectoryAgentListener;
import org.livetribe.slp.spi.DirectoryAgentNotifier;


/**
 * The <code>DirectoryAgentListenerServiceTracker</code> simplifies the
 * managment of SLP directory agent notification listeners by leverging the
 * OSGi service registry mechanism. This has the added benefit of automatic
 * deregistration of the listeners should the bundle become unresolved.
 * <p/>
 * Bundles wishing to register an SLP directory agent notification listeners
 * merely need to register an instance of {@link DirectoryAgentListener} in the
 * OSGi service registry.
 * <p/>
 * A {@link Filter} can be passed to the constructor to narrow which instances
 * of {@link DirectoryAgentListener} are registered.
 *
 * @see DirectoryAgentNotifier
 * @see DirectoryAgentListener
 */
public class DirectoryAgentListenerServiceTracker
{
    private final static String CLASS_NAME = DirectoryAgentListenerServiceTracker.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final ServiceTracker tracker;

    /**
     * Create a <code>DirectoryAgentListenerServiceTracker</code> which will be
     * used to automatically register instances of {@link DirectoryAgentListener}
     * to a specific {@link DirectoryAgentNotifier}.
     *
     * @param context  The OSGi {@link BundleContext} used to obtain OSGi service instances.
     * @param notifier The {@link DirectoryAgentNotifier} that directory agent notification listeners will be registered to.
     */
    public DirectoryAgentListenerServiceTracker(final BundleContext context, final DirectoryAgentNotifier notifier)
    {
        if (context == null) throw new IllegalArgumentException("Bundle context cannot be null");
        if (notifier == null) throw new IllegalArgumentException("Directory agent notifier cannot be null");

        try
        {
            tracker = generateServiceTracker(context, context.createFilter("(objectClass=" + DirectoryAgentListener.class.getName() + ")"), notifier);
        }
        catch (InvalidSyntaxException ise)
        {
            LOGGER.log(Level.WARNING, "Oddly this caused an invalid syntax exception", ise);
            throw new IllegalStateException("Oddly this caused an invalid syntax exception");
        }

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("context: " + context);
            LOGGER.config("notifier: " + notifier);
        }
    }

    /**
     * Create a <code>DirectoryAgentListenerServiceTracker</code> which will be
     * used to automatically register instances of {@link DirectoryAgentListener}
     * to a specific {@link DirectoryAgentNotifier}.
     *
     * @param context  The OSGi {@link BundleContext} used to obtain OSGi service instances.
     * @param filter   The {@link Filter} used to narrow which instances of {@link DirectoryAgentListener} are registered.
     * @param notifier The {@link DirectoryAgentNotifier} that directory agent notification listeners will be registered to.
     */
    public DirectoryAgentListenerServiceTracker(final BundleContext context, Filter filter, final DirectoryAgentNotifier notifier)
    {
        if (context == null) throw new IllegalArgumentException("Bundle context cannot be null");
        if (filter == null) throw new IllegalArgumentException("Bundle context cannot be null");
        if (notifier == null) throw new IllegalArgumentException("Directory agent notifier cannot be null");

        try
        {
            tracker = generateServiceTracker(context, context.createFilter("(&(objectClass=" + DirectoryAgentListener.class.getName() + ")" + filter + ")"), notifier);
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
            LOGGER.config("notifier: " + notifier);
        }
    }

    /**
     * Return the number of {@link DirectoryAgentListener} instances being tracked by this <code>ServiceTracker</code>.
     *
     * @return The number of listeners being tracked.
     */
    public int size()
    {
        return tracker.size();
    }

    /**
     * Open this <code>ServiceTracker</code> and begin tracking instances of {@link DirectoryAgentListener}.
     */
    public void open()
    {
        tracker.open();
    }

    /**
     * Close this <code>ServiceTracker</code>.
     * <p/>
     * This method should be called when this <code>ServiceTracker</code> should
     * end the tracking instances of {@link DirectoryAgentListener}.
     */
    public void close()
    {
        tracker.close();
    }

    private ServiceTracker generateServiceTracker(final BundleContext context, Filter filter, final DirectoryAgentNotifier notifier)
    {
        return new ServiceTracker(context,
                                  filter,
                                  new ServiceTrackerCustomizer()
                                  {
                                      public Object addingService(ServiceReference reference)
                                      {
                                          LOGGER.entering(CLASS_NAME, "addingService", reference);

                                          DirectoryAgentListener listener = (DirectoryAgentListener)context.getService(reference);

                                          notifier.addDirectoryAgentListener(listener);

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
                                          notifier.addDirectoryAgentListener((DirectoryAgentListener)listener);

                                          LOGGER.exiting(CLASS_NAME, "removedService");
                                      }
                                  });
    }
}
