package org.livetribe.slp.api.sa;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.da.DirectoryAgent;
import org.livetribe.slp.api.da.StandardDirectoryAgent;
import org.livetribe.slp.api.ua.StandardUserAgent;
import org.livetribe.slp.api.ua.UserAgent;
import org.livetribe.slp.spi.da.StandardDirectoryAgentManager;
import org.livetribe.slp.spi.msg.Message;
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

            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat1", ServiceURL.LIFETIME_MAXIMUM - 1);
            Scopes scopes = new Scopes(new String[]{"scope1", "scope2"});
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
                    List serviceInfos = ua.findServices(serviceURL.getServiceType(), scopes, null, null);

                    assert serviceInfos != null;
                    assert serviceInfos.size() == 1;
                    ServiceURL discoveredServiceURL = ((ServiceInfo)serviceInfos.get(0)).getServiceURL();
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
        da.setConfiguration(getDefaultConfiguration());
        da.start();

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(getDefaultConfiguration());
            sa.start();

            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat2", 13);
            Scopes scopes = new Scopes(new String[]{"scope1", "scope2"});
            String language = Locale.getDefault().getLanguage();
            ServiceInfo service = new ServiceInfo(null, serviceURL, scopes, null, language);
            sa.register(service);

            try
            {
                sa.deregister(service);

                StandardUserAgent ua = new StandardUserAgent();
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceInfos = ua.findServices(serviceURL.getServiceType(), scopes, null, language);

                    assert serviceInfos != null;
                    assert serviceInfos.isEmpty();
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
    public void testRegistrationWithDA() throws Exception
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
            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat3", ServiceURL.LIFETIME_MAXIMUM - 1);
            Scopes scopes = new Scopes(new String[]{"scope1", "scope2"});
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
                    List serviceInfos = ua.findServices(serviceURL.getServiceType(), scopes, null, null);

                    assert serviceInfos != null;
                    assert serviceInfos.size() == 1;
                    ServiceURL discoveredServiceURL = ((ServiceInfo)serviceInfos.get(0)).getServiceURL();
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
    public void testRegistrationWithoutDA() throws Exception
    {
        StandardServiceAgent sa = new StandardServiceAgent();
        sa.setConfiguration(getDefaultConfiguration());
        ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://ssat4");
        Scopes scopes = new Scopes(new String[]{"scope1"});
        ServiceInfo service1 = new ServiceInfo(serviceURL1, scopes, null, Locale.getDefault().getLanguage());
        sa.register(service1);

        Collection services = sa.getServices();
        assert services != null;
        assert services.size() == 1;
        assert service1.getServiceURL().equals(((ServiceInfo)services.iterator().next()).getServiceURL());

        sa.start();

        try
        {
            ServiceURL serviceURL2 = new ServiceURL("service:jmx:ws://ssat5");
            ServiceInfo service2 = new ServiceInfo(serviceURL2, new Scopes(new String[]{"scope2"}), null, Locale.getDefault().getLanguage());
            sa.register(service2);

            services = sa.getServices();
            assert services != null;
            assert services.size() == 2;
        }
        finally
        {
            sa.stop();
        }
    }

    /**
     * @testng.test
     */
    public void testListenForDAAdverts() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(configuration);

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(configuration);
            ServiceURL serviceURL = new ServiceURL("service:http://ssat6", ServiceURL.LIFETIME_PERMANENT);
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
                assert das.isEmpty();

                da.start();

                // Allow unsolicited DAAdvert to arrive and SA to register with DA
                sleep(500);

                das = sa.getCachedDirectoryAgents(sa.getScopes());
                assert das != null;
                assert das.size() == 1;

                StandardUserAgent ua  = new StandardUserAgent();
                ua.setConfiguration(configuration);
                ua.start();

                try
                {
                    List serviceInfos = ua.findServices(serviceURL.getServiceType(), sa.getScopes(), null, null);
                    assert serviceInfos != null;
                    assert serviceInfos.size() == 1;
                    ServiceURL service = ((ServiceInfo)serviceInfos.get(0)).getServiceURL();
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
            ServiceURL serviceURL = new ServiceURL("service:http://ssat7", ServiceURL.LIFETIME_PERMANENT);
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
    public void testRegistrationWithNoLanguage() throws Exception
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
            ServiceURL serviceURL = new ServiceURL("service:http://ssat8", ServiceURL.LIFETIME_PERMANENT);

            sa.register(new ServiceInfo(serviceURL, null, null, null));
            sa.start();

            try
            {
                Collection services = sa.getServices();
                assert services != null;
                assert services.size() == 1;
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
            ServiceURL serviceURL = new ServiceURL("service:http://ssat9", 0);
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

            ServiceURL serviceURL1 = new ServiceURL("service:http://ssat10", ServiceURL.LIFETIME_DEFAULT);
            ServiceInfo service1 = new ServiceInfo(serviceURL1, null, null, Locale.getDefault().getLanguage());
            sa.register(service1);
            ServiceURL serviceURL2 = new ServiceURL("service:jmx:http://ssat11", ServiceURL.LIFETIME_MAXIMUM);
            ServiceInfo service2 = new ServiceInfo(serviceURL2, null, null, Locale.getDefault().getLanguage());
            sa.register(service2);
            sa.start();

            sleep(500);

            try
            {
                UserAgent ua = new StandardUserAgent();
                ua.setConfiguration(configuration);
                ua.start();

                try
                {
                    List serviceInfo = ua.findServices(serviceURL1.getServiceType(), null, null, null);
                    assert serviceInfo != null;
                    assert serviceInfo.size() == 1;
                    assert serviceURL1.equals(((ServiceInfo)serviceInfo.get(0)).getServiceURL());

                    serviceInfo = ua.findServices(serviceURL2.getServiceType(), null, null, null);
                    assert serviceInfo != null;
                    assert serviceInfo.size() == 1;
                    assert serviceURL2.equals(((ServiceInfo)serviceInfo.get(0)).getServiceURL());
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
    public void testEmitMulticastSrvRegSrvDeRegNotification() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        MulticastSocket socket = new MulticastSocket(configuration.getNotificationPort());
        socket.setTimeToLive(configuration.getMulticastTTL());
        socket.joinGroup(InetAddress.getByName(configuration.getMulticastAddress()));
        socket.setSoTimeout(1000);

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(configuration);
            sa.start();

            try
            {
                ServiceURL serviceURL = new ServiceURL("service:jmx:rmi://ssat12");
                Scopes scopes = new Scopes(new String[]{"scope1"});
                ServiceInfo service = new ServiceInfo(serviceURL, scopes, null, Locale.getDefault().getLanguage());
                sa.register(service);

                byte[] bytes = new byte[configuration.getMTU()];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                socket.receive(packet);

                assert packet.getLength() > 0;
                byte[] messageBytes = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), messageBytes, 0, messageBytes.length);
                Message srvReg = Message.deserialize(messageBytes);
                assert srvReg.getMessageType() == Message.SRV_REG_TYPE;
                assert srvReg.isMulticast();

                sa.deregister(service);

                bytes = new byte[configuration.getMTU()];
                packet = new DatagramPacket(bytes, bytes.length);
                socket.receive(packet);

                assert packet.getLength() > 0;
                messageBytes = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), messageBytes, 0, messageBytes.length);
                Message srvDeReg = Message.deserialize(messageBytes);
                assert srvDeReg.getMessageType() == Message.SRV_DEREG_TYPE;
                assert srvDeReg.isMulticast();
            }
            finally
            {
                sa.stop();
            }
        }
        finally
        {
            socket.close();
        }
    }
}
