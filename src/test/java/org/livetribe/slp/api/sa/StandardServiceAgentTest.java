package org.livetribe.slp.api.sa;

import java.util.List;
import java.util.Locale;

import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
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
    protected void tearDown() throws Exception
    {
        sleep(500);
    }

    public void testStartStop() throws Exception
    {
        StandardServiceAgent sa = new StandardServiceAgent();
        StandardServiceAgentManager saManager = new StandardServiceAgentManager();
        saManager.setMulticastConnector(new SocketMulticastConnector());
        saManager.setUnicastConnector(new SocketUnicastConnector());
        sa.setServiceAgentManager(saManager);
        sa.setConfiguration(getDefaultConfiguration());
        sa.setServiceURL(new ServiceURL("http://host", ServiceURL.LIFETIME_DEFAULT));

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

    public void testRegistrationOnStartup() throws Exception
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
            sleep(500);

            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setMulticastConnector(new SocketMulticastConnector());
            saManager.setUnicastConnector(new SocketUnicastConnector());
            sa.setConfiguration(getDefaultConfiguration());
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_MAXIMUM - 1);
            String[] scopes = new String[]{"scope1", "scope2"};
            sa.setServiceURL(serviceURL);
            sa.setScopes(scopes);
            sa.setLanguage(Locale.getDefault().getLanguage());
            sa.start();

            try
            {
                sleep(500);

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

    public void testDeregistration() throws Exception
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
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", 13);
            String[] scopes = new String[]{"scope1", "scope2"};
            String language = Locale.getDefault().getLanguage();
            sa.setServiceURL(serviceURL);
            sa.setScopes(scopes);
            sa.setLanguage(language);
            sa.start();

            try
            {
                sa.deregister();

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
                    assertEquals(0, serviceURLs.size());
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
        finally
        {
            da.stop();
        }
    }

    public void testRegistration() throws Exception
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
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_MAXIMUM - 1);
            String[] scopes = new String[]{"scope1", "scope2"};
            sa.setServiceURL(serviceURL);
            sa.setScopes(scopes);
            sa.setLanguage(Locale.getDefault().getLanguage());
            sa.start();

            try
            {
                sleep(500);

                // Deregister what has been registered at startup
                sa.deregister();

                sa.register();

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

    public void testListenForDAAdverts() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        daManager.setUnicastConnector(new SocketUnicastConnector());
        da.setConfiguration(configuration);

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setMulticastConnector(new SocketMulticastConnector());
            saManager.setUnicastConnector(new SocketUnicastConnector());
            sa.setConfiguration(configuration);
            ServiceURL serviceURL = new ServiceURL("service:http://host", ServiceURL.LIFETIME_PERMANENT);
            sa.setServiceURL(serviceURL);
            sa.start();

            try
            {
                // The multicast convergence should stop after 2 timeouts, but use 3 to be sure
                long[] timeouts = configuration.getMulticastTimeouts();
                long sleep = timeouts[0] + timeouts[1] + timeouts[2];
                sleep(sleep);

                List das = sa.getCachedDirectoryAgents(sa.getScopes());
                assertNotNull(das);
                assertTrue(das.isEmpty());

                da.start();

                // Allow unsolicited DAAdvert to arrive and SA to register with DA
                sleep(500);

                das = sa.getCachedDirectoryAgents(sa.getScopes());
                assertNotNull(das);
                assertEquals(1, das.size());

                StandardUserAgent ua  = new StandardUserAgent();
                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                ua.setUserAgentManager(uaManager);
                uaManager.setMulticastConnector(new SocketMulticastConnector());
                uaManager.setUnicastConnector(new SocketUnicastConnector());
                ua.setConfiguration(configuration);
                ua.start();

                try
                {
                    List services = ua.findServices(sa.getServiceURL().getServiceType(), sa.getScopes(), null);
                    assertNotNull(services);
                    assertEquals(1, services.size());
                    ServiceURL service = (ServiceURL)services.get(0);
                    assertEquals(serviceURL, service);
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
        finally
        {
            // Stop DA last, so that the SA can deregister during stop()
            da.stop();
        }
    }

    public void testDADiscoveryOnStartup() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setMulticastConnector(new SocketMulticastConnector());
        daManager.setUnicastConnector(new SocketUnicastConnector());
        da.setConfiguration(configuration);
        da.start();

        try
        {
            sleep(500);

            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setMulticastConnector(new SocketMulticastConnector());
            saManager.setUnicastConnector(new SocketUnicastConnector());
            sa.setConfiguration(configuration);
            // Discover the DAs immediately
            sa.setDiscoveryStartWaitBound(0);
            ServiceURL serviceURL = new ServiceURL("service:http://host", ServiceURL.LIFETIME_PERMANENT);
            sa.setServiceURL(serviceURL);
            sa.start();

            try
            {
                // The multicast convergence should stop after 2 timeouts, but use 3 to be sure
                long[] timeouts = configuration.getMulticastTimeouts();
                long sleep = timeouts[0] + timeouts[1] + timeouts[2];
                sleep(sleep);

                List das = sa.getCachedDirectoryAgents(sa.getScopes());
                assertNotNull(das);
                assertEquals(1, das.size());
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            da.stop();
        }
    }
}
