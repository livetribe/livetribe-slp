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
package org.livetribe.slp;

import java.util.List;
import java.util.Locale;

import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.ua.UserAgent;


/**
 * This class tests that the SLP implementation is compatible with <a href="http://openslp.org">OpenSLP</a>.
 * <br />
 * It assumes OpenSLP has been configured as DirectoryAgent, and this test will behave as a service agent
 * (registering a service and deregistering it), and will behave as a user agent (discovering services).
 * <br />
 * In this configuration, the messages tested are:
 * <ul>
 * <li>SrvRqst</li>
 * <li>DAAdvert</li>
 * <li>SrvReg</li>
 * <li>SrvAck</li>
 * <li>SrvDeReg</li>
 * <li>SrvRply</li>
 * </ul>
 * Since OpenSLP runs on the standard SLP port (427), this class must be run as superuser in Unix-like
 * operative systems.
 */
public class OpenSLPInteroperability
{
    public static void main(String[] args) throws Exception
    {
        new OpenSLPInteroperability().test();
    }

    public void test() throws Exception
    {
        ServiceAgent sa = SLP.newServiceAgent(null);
        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi://host/path", ServiceURL.LIFETIME_DEFAULT);
        ServiceInfo service = new ServiceInfo(serviceURL, Locale.ITALY.getLanguage(), Scopes.DEFAULT, Attributes.from("present,(a=1,2),(b=true),(c=A description),(d=\\FF\\00)"));
        sa.start();
        System.out.println("Service Agent: discovered DA");
        try
        {
            sa.register(service);
            System.out.println("Service Agent: registered service " + serviceURL);

            UserAgent ua = SLP.newUserAgent(null);
            ua.start();
            System.out.println("User Agent: discovered DA");
            try
            {
                ServiceType serviceType = serviceURL.getServiceType();
                System.out.println("User Agent: finding service of type " + serviceType);
                List<ServiceInfo> services = ua.findServices(serviceType, service.getLanguage(), service.getScopes(), null);
                System.out.println("User Agent: found services " + services);
                if (services.isEmpty()) throw new AssertionError("Expected at least one service registered");
                ServiceURL registered = services.get(0).getServiceURL();
                if (!registered.equals(serviceURL))
                    throw new AssertionError("Expecting " + serviceURL + " got instead " + registered);

                // OpenSLP does not seem to handle attribute addition and removal, so we skip these

//                sa.addAttributes(service.getServiceURL(), service.getLanguage(), Attributes.from("(a=3),(e=1)"));
//                System.out.println("Service Agent: added service attributes");

//                sa.removeAttributes(service.getServiceURL(), service.getLanguage(), Attributes.from("c,d"));
//                System.out.println("Service Agent: removed service attributes");

                sa.deregister(service.getServiceURL(), service.getLanguage());
                System.out.println("Service Agent Client: deregistered service " + serviceURL);

                System.out.println("Interoperability with OpenSLP successful");
            }
            finally
            {
                ua.stop();
            }
        }
        finally
        {
            sa.stop();
        }
    }
}
