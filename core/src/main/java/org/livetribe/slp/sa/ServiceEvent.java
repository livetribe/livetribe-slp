/*
 * Copyright 2005-2008 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.sa;

import java.util.EventObject;

import org.livetribe.slp.ServiceInfo;

/**
 * The event that indicates that a service has been registered or updated or deregistered.
 * <br />
 * Service agents and directory agents manage service registration, update and deregistration and forward these
 * events to interested listeners.
 *
 * @version $Revision$ $Date$
 * @see ServiceListener
 */
public class ServiceEvent extends EventObject
{
    private final ServiceInfo previousService;
    private final ServiceInfo currentService;

    /**
     * Creates a new ServiceEvent.
     *
     * @param source          the source of the event
     * @param previousService the service prior the registration, prior the update or prior the deregistration.
     * @param currentService  the service after the registration, after the update or after the deregistration.
     */
    public ServiceEvent(Object source, ServiceInfo previousService, ServiceInfo currentService)
    {
        super(source);
        this.previousService = previousService;
        this.currentService = currentService;
    }

    /**
     * In case or registration event, returns the service overwritten by the registration, or null if no previous
     * service existed.
     * In case of update event, returns the service prior the update.
     * In case of deregistration event, returns the service that has been deregistered.
     *
     * @return the service prior the event
     */
    public ServiceInfo getPreviousService()
    {
        return previousService;
    }

    /**
     * In case of registration event, returns the service that has been registered.
     * In case of update event, return the service after the update.
     * In case of deregistration event returns null.
     *
     * @return the service after the event
     */
    public ServiceInfo getCurrentService()
    {
        return currentService;
    }
}
