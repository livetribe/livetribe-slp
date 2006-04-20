/*
 * Copyright 2006 the original author or authors
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

import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.sa.ServiceAgent;
import org.livetribe.slp.api.sa.ServiceInfo;
import org.livetribe.slp.api.sa.StandardServiceAgent;
import org.livetribe.slp.api.ua.StandardUserAgent;
import org.livetribe.slp.api.ua.UserAgent;

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
 *
 * @version $Rev$ $Date$
 */
public class OpenSLPInteroperability
{
    public static void main(String[] args) throws Exception
    {
        new OpenSLPInteroperability().test();
    }

    public void test() throws Exception
    {
        Configuration configuration = new Configuration();
        configuration.setMulticastTimeouts(new long[]{3000L, 3000L, 3000L, 3000L, 3000L});

        ServiceURL serviceURL = new ServiceURL("service:jmx:rmi://host/path", ServiceURL.LIFETIME_DEFAULT);

        System.out.println("Service Agent: discovering DA and registering service " + serviceURL);
        ServiceAgent sa = new StandardServiceAgent();
        sa.setConfiguration(configuration);
        ServiceInfo service = new ServiceInfo(null, serviceURL, null, null, Locale.ITALY.getLanguage());
        sa.register(service);
        sa.start();
        System.out.println("Service Agent: registered service " + serviceURL);

        try
        {
            System.out.println("User Agent: discovering DA");
            UserAgent ua = new StandardUserAgent();
            ua.setConfiguration(configuration);
            ua.start();

            try
            {
                ServiceType serviceType = serviceURL.getServiceType();
                System.out.println("User Agent: finding service of type " + serviceType);
                List serviceURLs = ua.findServices(serviceType, new String[]{"default"}, null, null);
                if (serviceURLs.isEmpty()) throw new AssertionError("Expected at least one service registered");

                System.out.println("User Agent: found services " + serviceURLs);
                ServiceURL registered = (ServiceURL)serviceURLs.get(0);
                if (!registered.equals(serviceURL)) throw new AssertionError("Expecting " + serviceURL + " got instead " + registered);

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
