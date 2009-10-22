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
 * @version $Revision$ $Date$
 */
public class DirectoryAgentListenerServiceTracker
{
    private final static String CLASS_NAME = DirectoryAgentListenerServiceTracker.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final ServiceTracker tracker;

    public DirectoryAgentListenerServiceTracker(final BundleContext context, final DirectoryAgentNotifier notifier) throws InvalidSyntaxException
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

    }

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
    }

    public int size()
    {
        return tracker.size();
    }

    public void open()
    {
        tracker.open();
    }

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

                                          DirectoryAgentListener listener = (DirectoryAgentListener) context.getService(reference);

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
                                          notifier.addDirectoryAgentListener((DirectoryAgentListener) listener);

                                          LOGGER.exiting(CLASS_NAME, "removedService");
                                      }
                                  });
    }
}
