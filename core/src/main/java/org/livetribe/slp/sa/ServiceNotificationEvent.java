/*
 * Copyright 2006-2008 the original author or authors
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
 * The event that indicates that a service registration or deregistration notification has been emitted
 * following RFC 3082.
 * <br />
 * Service agents emit notification in absence of a directory agent deployed on the network, to inform
 * interested parties that a service has been registered or deregistered.
 */
public class ServiceNotificationEvent extends EventObject
{
    private final ServiceInfo service;
    private final boolean update;

    /**
     * Create a new ServiceNotificationEvent.
     *
     * @param source  the source of the event
     * @param service the service that caused the notification
     * @param update  whether the notification is an update of a service or not
     */
    public ServiceNotificationEvent(Object source, ServiceInfo service, boolean update)
    {
        super(source);
        this.service = service;
        this.update = update;
    }

    /**
     * @return the service that caused the notification
     */
    public ServiceInfo getService()
    {
        return service;
    }

    /**
     * @return whether the notification is an update of a service or not
     */
    public boolean isUpdate()
    {
        return update;
    }
}
