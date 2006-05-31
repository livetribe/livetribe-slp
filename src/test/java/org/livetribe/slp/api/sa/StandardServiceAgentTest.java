package org.livetribe.slp.api.sa;

import java.util.List;
import java.util.Locale;

import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.da.DirectoryAgent;
import org.livetribe.slp.api.da.StandardDirectoryAgent;
import org.livetribe.slp.api.ua.StandardUserAgent;
import org.livetribe.slp.api.ua.UserAgent;
import org.livetribe.slp.spi.da.StandardDirectoryAgentManager;
import org.livetribe.slp.spi.net.SocketTCPConnector;
import org.livetribe.slp.spi.net.SocketUDPConnector;
import org.livetribe.slp.spi.sa.StandardServiceAgentManager;
import org.livetribe.slp.spi.ua.StandardUserAgentManager;

/**
 * @version $Rev$ $Date$
 */
public class StandardServiceAgentTest extends SLPTestSupport
{
    /**
     * @testng.configuration afterTestMethod="true"
     */
    protected void tearDown() throws Exception
    {
        sleep(500);
    }

    /**
     * @testng.test
     */
    public void testStartStop() throws Exception
    {
        StandardServiceAgent sa = new StandardServiceAgent();
        StandardServiceAgentManager saManager = new StandardServiceAgentManager();
        saManager.setUDPConnector(new SocketUDPConnector());
        saManager.setTCPConnector(new SocketTCPConnector());
        sa.setServiceAgentManager(saManager);
        sa.setConfiguration(getDefaultConfiguration());

        assert !sa.isRunning();
        sa.start();
        assert sa.isRunning();
        sa.stop();
        assert !sa.isRunning();
        sa.start();
        assert sa.isRunning();
        sa.stop();
        assert !sa.isRunning();
    }

    /**
     * @testng.test
     */
    public void testRegistrationOnStartup() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setUDPConnector(new SocketUDPConnector());
        daManager.setTCPConnector(new SocketTCPConnector());
        da.setConfiguration(getDefaultConfiguration());
        da.start();

        try
        {
            sleep(500);

            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setUDPConnector(new SocketUDPConnector());
            saManager.setTCPConnector(new SocketTCPConnector());
            sa.setConfiguration(getDefaultConfiguration());

            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_MAXIMUM - 1);
            String[] scopes = new String[]{"scope1", "scope2"};
            ServiceInfo service = new ServiceInfo(null, serviceURL, scopes, null, Locale.getDefault().getLanguage());
            sa.register(service);
            sa.start();

            try
            {
                sleep(500);

                StandardUserAgent ua = new StandardUserAgent();
                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                ua.setUserAgentManager(uaManager);
                uaManager.setUDPConnector(new SocketUDPConnector());
                uaManager.setTCPConnector(new SocketTCPConnector());
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceURLs = ua.findServices(serviceURL.getServiceType(), scopes, null, null);

                    assert serviceURLs != null;
                    assert serviceURLs.size() == 1;
                    ServiceURL discoveredServiceURL = (ServiceURL)serviceURLs.get(0);
                    assert discoveredServiceURL != null;
                    assert serviceURL.equals(discoveredServiceURL);
                    assert serviceURL.getLifetime() == discoveredServiceURL.getLifetime();
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

    /**
     * @testng.test
     */
    public void testDeregistration() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setUDPConnector(new SocketUDPConnector());
        daManager.setTCPConnector(new SocketTCPConnector());
        da.setConfiguration(getDefaultConfiguration());
        da.start();

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setUDPConnector(new SocketUDPConnector());
            saManager.setTCPConnector(new SocketTCPConnector());
            sa.setConfiguration(getDefaultConfiguration());
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", 13);
            String[] scopes = new String[]{"scope1", "scope2"};
            String language = Locale.getDefault().getLanguage();
            ServiceInfo service = new ServiceInfo(null, serviceURL, scopes, null, language);
            sa.register(service);
            sa.start();

            try
            {
                sa.deregister(service);

                StandardUserAgent ua = new StandardUserAgent();
                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                ua.setUserAgentManager(uaManager);
                uaManager.setUDPConnector(new SocketUDPConnector());
                uaManager.setTCPConnector(new SocketTCPConnector());
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceURLs = ua.findServices(serviceURL.getServiceType(), scopes, null, language);

                    assert serviceURLs != null;
                    assert serviceURLs.isEmpty();
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

    /**
     * @testng.test
     */
    public void testRegistration() throws Exception
    {
        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setUDPConnector(new SocketUDPConnector());
        daManager.setTCPConnector(new SocketTCPConnector());
        da.setConfiguration(getDefaultConfiguration());
        da.start();

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setUDPConnector(new SocketUDPConnector());
            saManager.setTCPConnector(new SocketTCPConnector());
            sa.setConfiguration(getDefaultConfiguration());
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///jmxrmi", ServiceURL.LIFETIME_MAXIMUM - 1);
            String[] scopes = new String[]{"scope1", "scope2"};
            ServiceInfo service = new ServiceInfo(null, serviceURL, scopes, null, Locale.getDefault().getLanguage());
            sa.register(service);
            sa.start();

            try
            {
                sleep(500);

                // Deregister what has been registered at startup
                sa.deregister(service);

                sa.register(service);

                StandardUserAgent ua = new StandardUserAgent();
                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                ua.setUserAgentManager(uaManager);
                uaManager.setUDPConnector(new SocketUDPConnector());
                uaManager.setTCPConnector(new SocketTCPConnector());
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceURLs = ua.findServices(serviceURL.getServiceType(), scopes, null, null);

                    assert serviceURLs != null;
                    assert serviceURLs.size() == 1;
                    ServiceURL discoveredServiceURL = (ServiceURL)serviceURLs.get(0);
                    assert discoveredServiceURL != null;
                    assert serviceURL.equals(discoveredServiceURL);
                    assert serviceURL.getLifetime() == discoveredServiceURL.getLifetime();
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

    /**
     * @testng.test
     */
    public void testListenForDAAdverts() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setUDPConnector(new SocketUDPConnector());
        daManager.setTCPConnector(new SocketTCPConnector());
        da.setConfiguration(configuration);

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setUDPConnector(new SocketUDPConnector());
            saManager.setTCPConnector(new SocketTCPConnector());
            sa.setConfiguration(configuration);
            ServiceURL serviceURL = new ServiceURL("service:http://host", ServiceURL.LIFETIME_PERMANENT);
            sa.register(new ServiceInfo(serviceURL, null, null, Locale.getDefault().getLanguage()));
            sa.start();

            try
            {
                // The multicast convergence should stop after 2 timeouts, but use 3 to be sure
                long[] timeouts = configuration.getMulticastTimeouts();
                long sleep = timeouts[0] + timeouts[1] + timeouts[2];
                sleep(sleep);

                List das = sa.getCachedDirectoryAgents(sa.getScopes());
                assert null != (das);
                assert (das.isEmpty());

                da.start();

                // Allow unsolicited DAAdvert to arrive and SA to register with DA
                sleep(500);

                das = sa.getCachedDirectoryAgents(sa.getScopes());
                assert das != null;
                assert das.size() == 1;

                StandardUserAgent ua  = new StandardUserAgent();
                StandardUserAgentManager uaManager = new StandardUserAgentManager();
                ua.setUserAgentManager(uaManager);
                uaManager.setUDPConnector(new SocketUDPConnector());
                uaManager.setTCPConnector(new SocketTCPConnector());
                ua.setConfiguration(configuration);
                ua.start();

                try
                {
                    List services = ua.findServices(serviceURL.getServiceType(), sa.getScopes(), null, null);
                    assert services != null;
                    assert services.size() == 1;
                    ServiceURL service = (ServiceURL)services.get(0);
                    assert serviceURL.equals(service);
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

    /**
     * @testng.test
     */
    public void testDADiscoveryOnStartup() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        StandardDirectoryAgentManager daManager = new StandardDirectoryAgentManager();
        da.setDirectoryAgentManager(daManager);
        daManager.setUDPConnector(new SocketUDPConnector());
        daManager.setTCPConnector(new SocketTCPConnector());
        da.setConfiguration(configuration);
        da.start();

        try
        {
            sleep(500);

            StandardServiceAgent sa = new StandardServiceAgent();
            StandardServiceAgentManager saManager = new StandardServiceAgentManager();
            sa.setServiceAgentManager(saManager);
            saManager.setUDPConnector(new SocketUDPConnector());
            saManager.setTCPConnector(new SocketTCPConnector());
            sa.setConfiguration(configuration);
            // Discover the DAs immediately
            sa.setDiscoveryStartWaitBound(0);
            ServiceURL serviceURL = new ServiceURL("service:http://host", ServiceURL.LIFETIME_PERMANENT);
            sa.register(new ServiceInfo(serviceURL, null, null, Locale.getDefault().getLanguage()));
            sa.start();

            try
            {
                // The multicast convergence should stop after 2 timeouts, but use 3 to be sure
                long[] timeouts = configuration.getMulticastTimeouts();
                long sleep = timeouts[0] + timeouts[1] + timeouts[2];
                sleep(sleep);

                List das = sa.getCachedDirectoryAgents(sa.getScopes());
                assert das != null;
                assert das.size() == 1;
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

    /**
     * @testng.test
     */
    public void testRegistrationFailureNoLanguage() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(configuration);
        da.start();

        try
        {
            sleep(500);

            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(configuration);
            ServiceURL serviceURL = new ServiceURL("service:http://host", ServiceURL.LIFETIME_PERMANENT);
            sa.register(new ServiceInfo(serviceURL, null, null, null));
            try
            {
                sa.start();
                throw new AssertionError();
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
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

    /**
     * @testng.test
     */
    public void testRegistrationFailureNoLifetime() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(configuration);
        da.start();

        try
        {
            sleep(500);

            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(configuration);
            ServiceURL serviceURL = new ServiceURL("service:http://host", 0);
            sa.register(new ServiceInfo(serviceURL, null, null, Locale.getDefault().getLanguage()));
            try
            {
                sa.start();
                throw new AssertionError();
            }
            catch (ServiceLocationException x)
            {
                assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
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

    /**
     * @testng.test
     */
    public void testRegisterMultipleServices() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        DirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(configuration);
        da.start();

        try
        {
            sleep(500);

            ServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(configuration);

            ServiceURL serviceURL1 = new ServiceURL("service:http://host", ServiceURL.LIFETIME_DEFAULT);
            ServiceInfo service1 = new ServiceInfo(serviceURL1, null, null, Locale.getDefault().getLanguage());
            sa.register(service1);
            ServiceURL serviceURL2 = new ServiceURL("service:jmx:http://host", ServiceURL.LIFETIME_MAXIMUM);
            ServiceInfo service2 = new ServiceInfo(serviceURL2, null, null, Locale.getDefault().getLanguage());
            sa.register(service2);
            sa.start();

            try
            {
                UserAgent ua = new StandardUserAgent();
                ua.setConfiguration(configuration);
                ua.start();

                try
                {
                    List result = ua.findServices(serviceURL1.getServiceType(), null, null, null);
                    assert result != null;
                    assert result.size() == 1;
                    assert serviceURL1.equals(result.get(0));

                    result = ua.findServices(serviceURL2.getServiceType(), null, null, null);
                    assert result != null;
                    assert result.size() == 1;
                    assert serviceURL2.equals(result.get(0));
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
}
