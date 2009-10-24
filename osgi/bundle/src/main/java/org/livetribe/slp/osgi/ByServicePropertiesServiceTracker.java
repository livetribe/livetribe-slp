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

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.osgi.util.Utils;
import org.livetribe.slp.sa.ServiceAgent;


/**
 * The <code>ByServicePropertiesServiceTracker</code> simplifies the managment
 * of SLP service URL registration by leverging the OSGi service registry
 * mechanism. This has the added benefit of automatic deregistration of the SLP
 * service URLshould the bundle become unresolved.
 * <p/>
 * Bundles wishing to register an SLP service URL merely need to register a
 * service in the OSGi service registry with the property <code>slp.url</code>.
 * A simple form of variable substitution can be used to construct the SLP
 * service URL.
 * <p/>
 * Any part of the <code>slp.url</code> can contain variables of the form
 * <code><b>${</b>key<b>}</b></code>, where <i>key</i> is used to index the
 * OSGi service properties to obtain a value.  If that key does not exist in
 * the service properties an empty string is used for the replacement value.
 * <p/>
 * The OSGi service properties are used to obtain the SLP service URL
 * properties.  The <code>slp.url</code> and the properties listed below are
 * removed first before creating the SLP service URL properties.
 * <p/>
 * Other OSGi service properties that are used to construct the instance of
 * {@link ServiceInfo} are:
 * <p/>
 * <ul>
 * <li><code>slp.service.type</code></li>
 * <li><code>slp.url.lifetime</code></li>
 * <li><code>slp.language</code></li>
 * <li><code>slp.scopes</code></li>
 * </ul>
 * <p/>
 * These properties do not participate in the variable subtitution for the SLP
 * service URL.  All are optional except <code>slp.url</code>.
 *
 * @version $Revision$ $Date$
 * @see ServiceInfo
 * @see ServiceAgent
 */
public class ByServicePropertiesServiceTracker extends ServiceTracker
{
    /**
     * The service type of the service
     */
    public final static String SLP_SERVICE_TYPE = "slp.service.type";

    /**
     * The service URL of the service
     */
    public final static String SLP_URL = "slp.url";

    /**
     * The lifetime, in seconds, for the service URL
     */
    public final static String SLP_URL_LIFETIME = "slp.url.lifetime";

    /**
     * The language of the service as an ISO 639 code
     */
    public final static String SLP_LANGUAGE = "slp.language";

    /**
     * The scopes of the service
     */
    public final static String SLP_SCOPES = "slp.scopes";
    private final static String CLASS_NAME = ByServicePropertiesServiceTracker.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final static Filter SLP_FILTER;
    private final ServiceAgent serviceAgent;

    static
    {
        Filter filter;
        try
        {
            filter = FrameworkUtil.createFilter("(slp.url=*)");
        }
        catch (InvalidSyntaxException ise)
        {
            LOGGER.log(Level.SEVERE, "Unable to create filter for (slp.url=*)", ise);
            filter = null;
        }
        SLP_FILTER = filter;
    }

    /**
     * Create a <code>ByServicePropertiesServiceTracker</code> which will be used to
     * automatically managed SLP service URLs for a specific {@link ServiceAgent}.
     *
     * @param context      The OSGi {@link BundleContext} used to obtain OSGi service instances.
     * @param serviceAgent The {@link ServiceAgent} used to manage SLP service URLs.
     */
    public ByServicePropertiesServiceTracker(BundleContext context, ServiceAgent serviceAgent)
    {
        super(context, SLP_FILTER, null);

        if (serviceAgent == null) throw new IllegalArgumentException("Service agent cannot be null");

        this.serviceAgent = serviceAgent;

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("context: " + context);
            LOGGER.config("serviceAgent: " + serviceAgent);
        }
    }

    /**
     * Create an instance of {@link ServiceInfo} from the OSGi service
     * reference's properties and register it with the {@link ServiceAgent},
     * effectively registering the SLP service URL.
     *
     * @param reference The reference whose properties are used to create the instance of {@link ServiceInfo}.
     * @return The instance of {@link ServiceInfo}.
     * @see org.osgi.util.tracker.ServiceTracker#addingService(ServiceReference)
     * @see org.livetribe.slp.sa.ServiceAgent#register(org.livetribe.slp.ServiceInfo)
     */
    @Override
    public Object addingService(ServiceReference reference)
    {
        LOGGER.entering(CLASS_NAME, "addingService", reference);

        ServiceInfo serviceInfo = generateServiceInfo(reference);

        serviceAgent.register(serviceInfo);

        LOGGER.exiting(CLASS_NAME, "addingService", serviceInfo);

        return serviceInfo;
    }

    /**
     * Deregister the instance of {@link ServiceInfo} from the {@link ServiceAgent},
     * effectively unregistering the SLP service URL, and reregister a new
     * {@link ServiceInfo} which was created from the modified OSGi service
     * reference's properties.
     *
     * @param reference The reference to the OSGi service that implicitly registered the SLP service URL.
     * @param service   The instance of {@link ServiceInfo} to deregister.
     * @see org.osgi.util.tracker.ServiceTracker#removedService(ServiceReference, Object)
     * @see org.livetribe.slp.sa.ServiceAgent#deregister(org.livetribe.slp.ServiceURL, String)
     * @see org.livetribe.slp.sa.ServiceAgent#register(org.livetribe.slp.ServiceInfo)
     */
    @Override
    public void modifiedService(ServiceReference reference, Object service)
    {
        LOGGER.entering(CLASS_NAME, "modifiedService", new Object[]{reference, service});

        ServiceInfo serviceInfo = (ServiceInfo)service;
        serviceAgent.deregister(serviceInfo.getServiceURL(), serviceInfo.getLanguage());

        serviceInfo = generateServiceInfo(reference);
        serviceAgent.register(serviceInfo);

        LOGGER.exiting(CLASS_NAME, "modifiedService");
    }

    /**
     * Deregister the instance of {@link ServiceInfo} from the {@link ServiceAgent},
     * effectively unregistering the SLP service URL.
     *
     * @param reference The reference to the OSGi service that implicitly registered the SLP service URL.
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

    private ServiceInfo generateServiceInfo(ServiceReference reference)
    {
        Map<String, String> map = Utils.toMap(reference);
        Set<String> keys = map.keySet();

        String slpUrl = Utils.subst((String)reference.getProperty(SLP_URL), map);

        ServiceURL serviceURL;
        if (keys.contains(SLP_URL_LIFETIME))
        {
            try
            {
                int lifetime = Integer.parseInt((String)reference.getProperty(SLP_URL_LIFETIME));
                serviceURL = new ServiceURL(slpUrl, lifetime);
            }
            catch (NumberFormatException e)
            {
                serviceURL = new ServiceURL(slpUrl);
            }
        }
        else
        {
            serviceURL = new ServiceURL(slpUrl);
        }

        String language;

        if (keys.contains(SLP_LANGUAGE)) language = (String)reference.getProperty(SLP_LANGUAGE);
        else language = Locale.getDefault().getLanguage();

        Scopes scopes = Scopes.NONE;
        if (keys.contains(SLP_SCOPES))
        {
            String[] values = ((String)reference.getProperty(SLP_SCOPES)).split(",");
            for (int i = 0; i < values.length; i++) values[i] = values[i].trim();
            scopes = Scopes.from(values);
        }

        ServiceType serviceType = null;
        if (keys.contains(SLP_SERVICE_TYPE))
        {
            serviceType = new ServiceType((String)reference.getProperty(SLP_SERVICE_TYPE));
        }

        map.remove(SLP_URL);
        map.remove(SLP_URL_LIFETIME);
        map.remove(SLP_LANGUAGE);
        map.remove(SLP_SCOPES);
        map.remove(SLP_SERVICE_TYPE);

        Attributes attributes = Attributes.from(map);

        ServiceInfo serviceInfo;
        if (serviceType != null)
        {
            serviceInfo = new ServiceInfo(serviceType, serviceURL, language, scopes, attributes);
        }
        else
        {
            serviceInfo = new ServiceInfo(serviceURL, language, scopes, attributes);
        }

        return serviceInfo;
    }

}