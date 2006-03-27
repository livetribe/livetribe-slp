package org.livetribe.slp.api.sa;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.SLPAPITestCase;
import org.livetribe.slp.api.da.StandardDirectoryAgent;
import org.livetribe.slp.api.ua.StandardUserAgent;
import org.livetribe.slp.spi.da.StandardDirectoryAgentManager;
import org.livetribe.slp.spi.net.SocketMulticastConnector;
import org.livetribe.slp.spi.net.SocketUnicastConnector;
import org.livetribe.slp.spi.sa.StandardServiceAgentManager;
import org.livetribe.slp.spi.ua.StandardUserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgentTest extends SLPAPITestCase
{
    public void testStartStop() throws Exception
    {
        StandardServiceAgent sa = new StandardServiceAgent();
        StandardServiceAgentManager saManager = new StandardServiceAgentManager();
        saManager.setMulticastConnector(new SocketMulticastConnector());
        saManager.setUnicastConnector(new SocketUnicastConnector());
        sa.setServiceAgentManager(saManager);
        sa.setConfiguration(getDefaultConfiguration());

        assertFalse(sa.isRunning());
        sa.start();
        assertTrue(sa.isRunning());
        sa.stop();
        assertFalse(sa.isRunning());
        sa.start();
        assertTrue(sa.isRunning());
        sa.stop();
        assertFalse(sa.isRunning());
    }

    public void testRegisterServices() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        daManager.setUnicastConnector(new SocketUnicastConnector());
        da.setConfiguration(getDefaultConfiguration());
        da.start();

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setMulticastConnector(new SocketMulticastConnector());
            saManager.setUnicastConnector(new SocketUnicastConnector());
            sa.setConfiguration(getDefaultConfiguration());
            sa.start();

            try
            {
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", 13);
                String[] scopes = new String[]{"scope1", "scope2"};
                sa.registerService(serviceURL.getServiceType(), serviceURL, scopes, null, Locale.getDefault().getCountry());

                StandardUserAgent ua = new StandardUserAgent();
                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                ua.setUserAgentManager(uaManager);
                uaManager.setMulticastConnector(new SocketMulticastConnector());
                uaManager.setUnicastConnector(new SocketUnicastConnector());
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceURLs = ua.findServices(serviceURL.getServiceType(), scopes, null);

                    assertNotNull(serviceURLs);
                    assertEquals(1, serviceURLs.size());
                    ServiceURL service = (ServiceURL)serviceURLs.get(0);
                    assertNotNull(service);
                    assertEquals(serviceURL, service);
                    assertEquals(serviceURL.getLifetime(), service.getLifetime());
                }
                finally
                {
                    uaManager.stop();
                }
            }
            finally
            {
                saManager.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }
}
