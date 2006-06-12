package org.livetribe.slp.api.sa;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPTestSupport;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.api.Configuration;
import org.livetribe.slp.api.da.DirectoryAgent;
import org.livetribe.slp.api.da.StandardDirectoryAgent;
import org.livetribe.slp.api.ua.StandardUserAgent;
import org.livetribe.slp.api.ua.UserAgent;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.net.SocketTCPConnector;
import org.livetribe.slp.spi.sa.StandardServiceAgentManager;

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
        Scopes scopes = new Scopes(new String[]{"scope1", "scope2"});

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(getDefaultConfiguration());
        da.setScopes(scopes);
        da.start();

        try
        {
            sleep(500);

            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(getDefaultConfiguration());
            sa.setScopes(scopes);

            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat1", ServiceURL.LIFETIME_MAXIMUM - 1);
            ServiceInfo service = new ServiceInfo(serviceURL, scopes, null, Locale.getDefault().getLanguage());
            sa.register(service);
            sa.start();

            try
            {
                sleep(500);

                StandardUserAgent ua = new StandardUserAgent();
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
    public void testDeregistration() throws Exception
    {
        Scopes scopes = new Scopes(new String[]{"scope1", "scope2"});

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(getDefaultConfiguration());
        da.setScopes(scopes);
        da.start();

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(getDefaultConfiguration());
            sa.setScopes(scopes);
            sa.start();

            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat2", 13);
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
        da.setConfiguration(getDefaultConfiguration());
        da.start();

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(getDefaultConfiguration());

            ServiceURL serviceURL = new ServiceURL("service:jmx:rmi:///jndi/rmi:///ssat3", ServiceURL.LIFETIME_MAXIMUM - 1);
            ServiceInfo service = new ServiceInfo(null, serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
            sa.register(service);
            sa.start();

            try
            {
                sleep(500);

                // Deregister what has been registered at startup
                sa.deregister(service);

                sa.register(service);

                StandardUserAgent ua = new StandardUserAgent();
                ua.setConfiguration(getDefaultConfiguration());
                ua.start();

                try
                {
                    List serviceInfos = ua.findServices(serviceURL.getServiceType(), null, null, null);

                    assert serviceInfos != null;
                    assert serviceInfos.size() == 1;
                    ServiceURL discoveredServiceURL = ((ServiceInfo)serviceInfos.get(0)).getServiceURL();
                    assert discoveredServiceURL != null;
                    assert serviceURL.equals(discoveredServiceURL);
                    assert serviceURL.getLifetime() == discoveredServiceURL.getLifetime();
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
    public void testRegistrationWithoutDA() throws Exception
    {
        StandardServiceAgent sa = new StandardServiceAgent();
        sa.setConfiguration(getDefaultConfiguration());
        ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://ssat4");
        ServiceInfo service1 = new ServiceInfo(serviceURL1, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
        sa.register(service1);

        Collection services = sa.getServices();
        assert services != null;
        assert services.size() == 1;
        assert service1.getServiceURL().equals(((ServiceInfo)services.iterator().next()).getServiceURL());

        sa.start();

        try
        {
            ServiceURL serviceURL2 = new ServiceURL("service:jmx:ws://ssat5");
            ServiceInfo service2 = new ServiceInfo(serviceURL2, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
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
            sa.register(new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage()));
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
        da.setConfiguration(configuration);
        da.start();

        try
        {
            sleep(500);

            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(configuration);
            // Discover the DAs immediately
            sa.setDirectoryAgentDiscoveryInitialWaitBound(0);
            ServiceURL serviceURL = new ServiceURL("service:http://ssat7", ServiceURL.LIFETIME_PERMANENT);
            sa.register(new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage()));
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
        StandardServiceAgent sa = new StandardServiceAgent();
        sa.setConfiguration(configuration);
        ServiceURL serviceURL = new ServiceURL("service:http://ssat8", ServiceURL.LIFETIME_PERMANENT);
        ServiceInfo serviceInfo = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, null);
        try
        {
            sa.register(serviceInfo);
            throw new AssertionError();
        }
        catch (ServiceLocationException x)
        {
            assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
        }
    }

    /**
     * @testng.test
     */
    public void testRegistrationFailureNoLifetime() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();
        StandardServiceAgent sa = new StandardServiceAgent();
        sa.setConfiguration(configuration);
        ServiceURL serviceURL = new ServiceURL("service:http://ssat9", 0);
        ServiceInfo serviceInfo = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
        try
        {
            sa.register(serviceInfo);
            throw new AssertionError();
        }
        catch (ServiceLocationException x)
        {
            assert x.getErrorCode() == ServiceLocationException.INVALID_REGISTRATION;
        }
    }

    /**
     * @testng.test
     */
    public void testRegistrationFailureNoScopesMatch() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();
        StandardServiceAgent sa = new StandardServiceAgent();
        sa.setConfiguration(configuration);
        sa.setScopes(new Scopes(new String[]{"sascope"}));
        sa.start();

        try
        {
            ServiceURL serviceURL = new ServiceURL("service:foo://bar");
            ServiceInfo serviceInfo = new ServiceInfo(serviceURL, new Scopes(new String[]{"unsupported"}), null, Locale.getDefault().getLanguage());
            sa.register(serviceInfo);
            throw new AssertionError();
        }
        catch (ServiceLocationException x)
        {
            assert x.getErrorCode() == ServiceLocationException.SCOPE_NOT_SUPPORTED;
        }
        finally
        {
            sa.stop();
        }
    }

    /**
     * @testng.test
     */
    public void testRegistrationOfMultipleServices() throws Exception
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
            ServiceInfo service1 = new ServiceInfo(serviceURL1, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
            sa.register(service1);
            ServiceURL serviceURL2 = new ServiceURL("service:jmx:http://ssat11", ServiceURL.LIFETIME_MAXIMUM);
            ServiceInfo service2 = new ServiceInfo(serviceURL2, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
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
                ServiceInfo service = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
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

    /**
     * @testng.test
     */
    public void testUnschedulingOfRegistrationRenewal() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(configuration);
        da.start();

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(configuration);
            sa.start();

            try
            {
                int lifetime = 10; // seconds
                ServiceURL serviceURL = new ServiceURL("service:foo:bar://baz", lifetime);
                ServiceInfo serviceInfo = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.getDefault().getLanguage());
                sa.register(serviceInfo);

                Collection services = da.getServices();
                assert services  != null;
                assert services.size() == 1;

                // Deregister immediately, and see if also the deregistration went fine
                sa.deregister(serviceInfo);

                services = da.getServices();
                assert services  != null;
                assert services.size() == 0;

                // Wait the whole lifetime
                sleep(lifetime * 1000L);

                services = da.getServices();
                assert services  != null;
                assert services.size() == 0;
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
    public void testServiceRequestMatching() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardServiceAgent sa = new StandardServiceAgent();
        StandardServiceAgentManager saManager = new StandardServiceAgentManager();
        sa.setServiceAgentManager(saManager);
        SocketTCPConnector unicastConnector = new SocketTCPConnector();
        unicastConnector.setTCPListening(true);
        saManager.setTCPConnector(unicastConnector);
        sa.setConfiguration(configuration);
        sa.setScopes(new Scopes(new String[]{"DEFAULT", "ws"}));
        sa.start();

        try
        {
            ServiceURL serviceURL1 = new ServiceURL("service:jmx:rmi://host");
            ServiceInfo serviceInfo1 = new ServiceInfo(serviceURL1, Scopes.DEFAULT, null, Locale.ENGLISH.getLanguage());
            sa.register(serviceInfo1);

            ServiceInfo serviceInfo2 = new ServiceInfo(serviceURL1, Scopes.DEFAULT, null, Locale.ITALIAN.getLanguage());
            sa.register(serviceInfo2);

            ServiceURL serviceURL2 = new ServiceURL("service:jmx:ws://host");
            Scopes wsScopes = new Scopes(new String[]{"ws"});
            Attributes wsAttributes = new Attributes("(port=80),(confidential=false)");
            ServiceInfo serviceInfo3 = new ServiceInfo(serviceURL2, wsScopes, wsAttributes, Locale.ENGLISH.getLanguage());
            sa.register(serviceInfo3);

            StandardUserAgent ua = new StandardUserAgent();
            ua.setConfiguration(configuration);
            ua.start();

            try
            {
                // Search for all services
                ServiceType abstractServiceType = new ServiceType("service:jmx");
                List allResult = ua.findServices(abstractServiceType, null, null, null);
                assert allResult.size() == 3;

                List rmiResult = ua.findServices(new ServiceType("jmx:rmi"), null, null, null);
                assert rmiResult.size() == 2;

                List wsResult = ua.findServices(serviceURL2.getServiceType(), null, null, null);
                assert wsResult.size() == 1;

                List wrongScopesResult = ua.findServices(abstractServiceType, new Scopes(new String[]{"wrong"}), null, null);
                assert wrongScopesResult.size() == 0;

                List wsScopesResult = ua.findServices(abstractServiceType, wsScopes, null, null);
                assert wsScopesResult.size() == 1;

                List attrsResult = ua.findServices(abstractServiceType, null, "(&(confidential=false)(port=80))", null);
                assert attrsResult.size() == 1;

                attrsResult = ua.findServices(abstractServiceType, null, "(port=80)", null);
                assert attrsResult.size() == 1;

                attrsResult = ua.findServices(abstractServiceType, null, "(port=81)", null);
                assert attrsResult.size() == 0;

                List langResult = ua.findServices(abstractServiceType, null, null, Locale.ENGLISH.getLanguage());
                assert langResult.size() == 2;

                langResult = ua.findServices(abstractServiceType, null, null, Locale.ITALIAN.getLanguage());
                assert langResult.size() == 1;

                langResult = ua.findServices(abstractServiceType, null, null, Locale.GERMAN.getLanguage());
                assert langResult.size() == 0;
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

    /**
     * @testng.test
     */
    public void testRenewalWithDA() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardDirectoryAgent da = new StandardDirectoryAgent();
        da.setConfiguration(configuration);
        da.start();

        try
        {
            StandardServiceAgent sa = new StandardServiceAgent();
            sa.setConfiguration(configuration);
            sa.setPeriodicDirectoryAgentDiscoveryEnabled(false);
            sa.setPeriodicServiceRenewalEnabled(false);
            sa.start();

            int lifetime = 5; // seconds
            ServiceURL serviceURL = new ServiceURL("service:foo:bar://host", lifetime);
            ServiceInfo serviceInfo = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.ENGLISH.getLanguage());

            try
            {
                sa.register(serviceInfo);

                // Check that the DA has it
                assert da.getServices().size() == 1;

                // Wait for the lifetime to expire
                sleep((lifetime + 1) * 1000L);

                // The renewal is disabled, the DA should be empty
                assert da.getServices().size() == 0;
            }
            finally
            {
                sa.stop();
            }

            sa.setPeriodicServiceRenewalEnabled(true);
            sa.start();

            try
            {
                sa.register(serviceInfo);

                // Check that the DA has it
                assert da.getServices().size() == 1;

                // Wait for the lifetime to expire
                sleep((lifetime + 1) * 1000L);

                // The renewal is enabled, the DA should have it
                assert da.getServices().size() == 1;
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
    public void testRenewalWithoutDA() throws Exception
    {
        Configuration configuration = getDefaultConfiguration();

        StandardServiceAgent sa = new StandardServiceAgent();
        sa.setConfiguration(configuration);
        sa.setPeriodicDirectoryAgentDiscoveryEnabled(false);
        sa.setPeriodicServiceRenewalEnabled(false);
        sa.start();

        int lifetime = 5; // seconds
        ServiceURL serviceURL = new ServiceURL("service:foo:bar://host", lifetime);
        ServiceInfo serviceInfo = new ServiceInfo(serviceURL, Scopes.DEFAULT, null, Locale.ENGLISH.getLanguage());

        try
        {
            sa.register(serviceInfo);

            // Check that the SA has it
            assert sa.getServices().size() == 1;

            // Wait for the lifetime to expire
            sleep((lifetime + 1) * 1000L);

            assert sa.getServices().size() == 1;
            ServiceInfo expired = (ServiceInfo)sa.getServices().iterator().next();
            assert expired.isExpiredAsOf(System.currentTimeMillis());
        }
        finally
        {
            sa.stop();
        }

        sa.setPeriodicServiceRenewalEnabled(true);
        sa.start();

        try
        {
            sa.register(serviceInfo);

            // Check that the SA has it
            assert sa.getServices().size() == 1;

            // Wait for the lifetime to expire
            sleep((lifetime + 1) * 1000L);

            assert sa.getServices().size() == 1;
            ServiceInfo renewed = (ServiceInfo)sa.getServices().iterator().next();
            assert !renewed.isExpiredAsOf(System.currentTimeMillis());
        }
        finally
        {
            sa.stop();
        }
    }
}
