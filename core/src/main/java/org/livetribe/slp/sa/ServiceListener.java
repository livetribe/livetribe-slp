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

import java.util.EventListener;


/**
 * The listener that receives ServiceEvents.
 *
 * @see ServiceEvent
 */
public interface ServiceListener extends EventListener
{
    /**
     * Invoked when a service is registered
     *
     * @param event the service registration event
     */
    public void serviceAdded(ServiceEvent event);

    /**
     * Invoked when a service is updated
     *
     * @param event the service update event
     */
    public void serviceUpdated(ServiceEvent event);

    /**
     * Invoked when a service is deregistered
     *
     * @param event the service deregistration event
     */
    public void serviceRemoved(ServiceEvent event);
}
