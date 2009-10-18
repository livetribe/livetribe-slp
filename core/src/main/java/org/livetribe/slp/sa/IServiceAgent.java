/*
 * Copyright 2007-2008 the original author or authors
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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;

/**
 * Common interface for SLP service agents.
 * <br />
 * A service agent advertise services on behalf of the provider of the services.
 * <br />
 * For example, an application can expose a JMXConnectorServer in order to provide management to remote clients;
 * without SLP, a remote client either knows beforehand how to connect to the JMXConnectorServer, or it has no
 * clue of how to connect.
 * <br />
 * With SLP, instead, the application exposes the JMXConnectorServer as a service via SLP, by starting a service agent
 * and by registering the JMXConnectorServer service in the service agent.
 * A remote client uses an SLP user agent to issue a service request for JMXConnectorServer services; the service
 * agent replies to this request sending the information necessary for the client to connect to the JMXConnectorServer.
 * At this point SLP goes out of the games, and the remote client can use the JMX API to connect to the
 * JMXConnectorServer.
 *
 * @version $Revision$ $Date$
 */
public interface IServiceAgent
{
    /**
     * Registers the given service.
     * <br />
     * It is necessary that the given service defines the {@link ServiceURL}, the language and that the Scopes of this
     * service agent {@link Scopes#match(Scopes) match} the Scopes of the service.
     *
     * @param service The service to register
     * @throws ServiceLocationException if the service cannot be registered
     * @see #deregister(ServiceURL, String)
     */
    public void register(ServiceInfo service) throws ServiceLocationException;

    /**
     * Updates the Attributes of the service registered with the given ServiceURL and language adding the given Attributes.
     * <br />
     * A registered service with Attributes (A=1),(B=2),(C=3) that is updated adding the Attributes (C=30),(D=40) will
     * have Attributes (A=1),(B=2),(C=30),(D=40)
     *
     * @param serviceURL the ServiceURL of the registered service
     * @param language   the language of the registered service
     * @param attributes the Attributes to add
     * @throws ServiceLocationException if the update fails
     * @see #removeAttributes(ServiceURL, String, Attributes)
     */
    public void addAttributes(ServiceURL serviceURL, String language, Attributes attributes) throws ServiceLocationException;

    /**
     * Updates the Attributes of the service registered with the given ServiceURL and language removing the given Attributes.
     * <br />
     * A registered service with Attributes (A=1),(B=2),(C=3) that is updated removing the Attributes (B=20),D will
     * have Attributes (A=1),(C=3).
     * Only the tags (and not the values) in the given Attributes are used to remove the corrispondent attributes in
     * the registered service.
     *
     * @param serviceURL the ServiceURL of the registered service
     * @param language   the language of the registered service
     * @param attributes the Attributes to remove
     * @throws ServiceLocationException if the update fails
     * @see #addAttributes(ServiceURL, String, Attributes)
     */
    public void removeAttributes(ServiceURL serviceURL, String language, Attributes attributes) throws ServiceLocationException;

    /**
     * Deregister the service with the given ServiceURL and language.
     *
     * @param serviceURL the ServiceURL of the service to remove
     * @param language   the language of the service to remove
     * @throws ServiceLocationException if the deregistration fails
     * @see #register(ServiceInfo)
     */
    public void deregister(ServiceURL serviceURL, String language) throws ServiceLocationException;
}
