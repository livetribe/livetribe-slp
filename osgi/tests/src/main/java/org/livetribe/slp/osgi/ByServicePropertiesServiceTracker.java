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
 * @version $Revision$ $Date$
 */
public class ByServicePropertiesServiceTracker extends ServiceTracker
{
    public final static String SLP_SERVICE_TYPE = "slp.service.type";
    public final static String SLP_URL = "slp.url";
    public final static String SLP_URL_LIFETIME = "slp.url.lifetime";
    public final static String SLP_LANGUAGE = "slp.language";
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

    public ByServicePropertiesServiceTracker(BundleContext context, ServiceAgent serviceAgent)
    {
        super(context, SLP_FILTER, null);

        if (serviceAgent == null) throw new IllegalArgumentException("Service agent cannot be null");

        this.serviceAgent = serviceAgent;
    }

    @Override
    public Object addingService(ServiceReference reference)
    {
        LOGGER.entering(CLASS_NAME, "addingService", reference);

        ServiceInfo serviceInfo = generateServiceInfo(reference);

        serviceAgent.register(serviceInfo);

        LOGGER.exiting(CLASS_NAME, "addingService", serviceInfo);

        return serviceInfo;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service)
    {
        LOGGER.entering(CLASS_NAME, "modifiedService", new Object[]{reference, service});

        ServiceInfo serviceInfo = (ServiceInfo) service;
        serviceAgent.deregister(serviceInfo.getServiceURL(), serviceInfo.getLanguage());

        serviceInfo = generateServiceInfo(reference);
        serviceAgent.register(serviceInfo);

        LOGGER.exiting(CLASS_NAME, "modifiedService");
    }

    @Override
    public void removedService(ServiceReference reference, Object service)
    {
        LOGGER.entering(CLASS_NAME, "removedService", new Object[]{reference, service});

        context.ungetService(reference);

        ServiceInfo serviceInfo = (ServiceInfo) service;

        serviceAgent.deregister(serviceInfo.getServiceURL(), serviceInfo.getLanguage());

        LOGGER.exiting(CLASS_NAME, "removedService");
    }

    private ServiceInfo generateServiceInfo(ServiceReference reference)
    {
        Map<String, String> map = Utils.toMap(reference);
        Set<String> keys = map.keySet();

        String slpUrl = Utils.subst((String) reference.getProperty(SLP_URL), map);

        ServiceURL serviceURL;
        if (keys.contains(SLP_URL_LIFETIME))
        {
            try
            {
                int lifetime = Integer.parseInt((String) reference.getProperty(SLP_URL_LIFETIME));
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

        if (keys.contains(SLP_LANGUAGE)) language = (String) reference.getProperty(SLP_LANGUAGE);
        else language = Locale.getDefault().getLanguage();

        Scopes scopes = Scopes.NONE;
        if (keys.contains(SLP_SCOPES))
        {
            String[] values = ((String) reference.getProperty(SLP_SCOPES)).split(",");
            for (int i = 0; i < values.length; i++) values[i] = values[i].trim();
            scopes = Scopes.from(values);
        }

        ServiceType serviceType = null;
        if (keys.contains(SLP_SERVICE_TYPE))
        {
            serviceType = new ServiceType((String) reference.getProperty(SLP_SERVICE_TYPE));
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