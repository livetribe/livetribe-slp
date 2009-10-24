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
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.sa.ServiceAgent;


/**
 * The <code>ByServiceInfoServiceTracker</code> simplifies the managment of SLP
 * service URL registration by leverging the OSGi service registry mechanism.
 * This has the added benefit of automatic deregistration of the SLP service
 * URLshould the bundle become unresolved.
 * <p/>
 * Bundles wishing to register an SLP service URL merely need to register an
 * instance of {@link ServiceInfo} in the OSGi service registry.  The instance
 * of {@link ServiceInfo} contains all the information to register an SLP
 * service URL.
 *
 * @version $Revision$ $Date$
 * @see ServiceInfo
 * @see ServiceAgent
 */
public class ByServiceInfoServiceTracker extends ServiceTracker
{
    private final static String CLASS_NAME = ByServiceInfoServiceTracker.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final ServiceAgent serviceAgent;

    /**
     * Create a <code>ByServiceInfoServiceTracker</code> which will be used to
     * automatically managed SLP service URLs for a specific {@link ServiceAgent}.
     *
     * @param context      The OSGi {@link BundleContext} used to obtain OSGi service instances.
     * @param serviceAgent The {@link ServiceAgent} used to manage SLP service URLs.
     */
    public ByServiceInfoServiceTracker(BundleContext context, ServiceAgent serviceAgent)
    {
        super(context, ServiceInfo.class.getName(), null);

        if (serviceAgent == null) throw new IllegalArgumentException("Service agent cannot be null");

        this.serviceAgent = serviceAgent;

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("context: " + context);
            LOGGER.config("serviceAgent: " + serviceAgent);
        }
    }

    /**
     * Obtain the instance of {@link ServiceInfo} from the OSGi service
     * reference and register it with the {@link ServiceAgent}, effectively
     * registering the SLP service URL.
     *
     * @param reference The reference to the instance of {@link ServiceInfo}.
     * @return The instance of {@link ServiceInfo}.
     * @see org.osgi.util.tracker.ServiceTracker#addingService(ServiceReference)
     * @see org.livetribe.slp.sa.ServiceAgent#register(org.livetribe.slp.ServiceInfo)
     */
    @Override
    public Object addingService(ServiceReference reference)
    {
        LOGGER.entering(CLASS_NAME, "addingService", reference);

        ServiceInfo serviceInfo = (ServiceInfo)context.getService(reference);

        serviceAgent.register(serviceInfo);

        LOGGER.exiting(CLASS_NAME, "addingService", serviceInfo);

        return serviceInfo;
    }

    /**
     * Deregister the instance of {@link ServiceInfo} from the {@link ServiceAgent},
     * effectively unregistering the SLP service URL.
     *
     * @param reference The reference to the instance of {@link ServiceInfo}.
     * @param service   The instance of {@link ServiceInfo} to deregister.
     * @see org.osgi.util.tracker.ServiceTracker#removedService(ServiceReference, Object)
     * @see org.livetribe.slp.sa.ServiceAgent#deregister(org.livetribe.slp.ServiceURL, String)
     */
    @Override
    public void removedService(ServiceReference reference, Object service)
    {
        LOGGER.entering(CLASS_NAME, "removedService", new Object[]{reference, service});

        context.ungetService(reference);

        ServiceInfo serviceInfo = (ServiceInfo)service;

        serviceAgent.deregister(serviceInfo.getServiceURL(), serviceInfo.getLanguage());

        LOGGER.exiting(CLASS_NAME, "removedService");
    }

}