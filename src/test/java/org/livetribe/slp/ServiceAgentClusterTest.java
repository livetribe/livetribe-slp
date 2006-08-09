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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.livetribe.slp.api.MatchingServiceInfoCache;
import org.livetribe.slp.api.ServiceRegistrationEvent;
import org.livetribe.slp.api.ServiceRegistrationListener;
import org.livetribe.slp.api.sa.ServiceAgent;
import org.livetribe.slp.api.sa.StandardServiceAgent;
import org.livetribe.slp.api.ua.StandardUserAgent;
import org.livetribe.slp.api.ua.UserAgent;

/**
 * Purpose of this test is to start N ServiceAgents that expose one service each, simulating a cluster.
 * SAs can be notified of when other SAs come online and when SAs shutdown, thus
 * keeping the cluster always up-to-date.
 *
 * @version $Rev$ $Date$
 */
public class ServiceAgentClusterTest extends SLPTestSupport
{
    /**
     * @testng.test
     */
    public void testClusterWithOneNode() throws Exception
    {
        int port = getPort();
        int lifetime = 15;
        TestClusterNode node1 = new TestClusterNode(port, "/node1", lifetime);
        node1.start();
        sleep(500);

        try
        {
            assert node1.getNodes().size() == 1;
            assert node1.getNodes().contains(node1.getNodeName());
        }
        finally
        {
            node1.stop();
        }
    }

    /**
     * @testng.test
     */
    public void testClusterWithTwoNodes() throws Exception
    {
        int port = getPort();
        int lifetime = 15;

        TestClusterNode node1 = new TestClusterNode(port, "/node1", lifetime);
        node1.start();
        sleep(500);

        try
        {
            TestClusterNode node2 = new TestClusterNode(port, "/node2", lifetime);
            node2.start();
            sleep(500);

            try
            {
                assert node1.getNodes().size() == 2;
                assert node1.getNodes().contains(node1.getNodeName());
                assert node1.getNodes().contains(node2.getNodeName());

                assert node2.getNodes().size() == 2;
                assert node2.getNodes().contains(node2.getNodeName());
                assert node2.getNodes().contains(node1.getNodeName());

                node2.stop();
                sleep(500);

                assert node1.getNodes().size() == 1;
                assert node1.getNodes().contains(node1.getNodeName());
            }
            finally
            {
                node2.stop();
            }
        }
        finally
        {
            node1.stop();
        }
    }

    private static class ClusterNode implements ServiceRegistrationListener, Runnable
    {
        private static final ServiceType SERVICE_TYPE = new ServiceType("service:cluster:slp");
        private static final Scopes SCOPES = new Scopes(new String[]{"scope"});
        private static final String LANGUAGE = Locale.ENGLISH.getLanguage();

        private final int port;
        private final String nodeName;
        private final int lifetime;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final AtomicBoolean running = new AtomicBoolean();
        private MatchingServiceInfoCache agents;
        private ServiceAgent serviceAgent;
        private UserAgent userAgent;


        public ClusterNode(int port, String nodeName, int lifetime)
        {
            this.port = port;
            this.nodeName = nodeName;
            this.lifetime = lifetime;
        }

        public String getNodeName()
        {
            return nodeName;
        }

        public void start() throws Exception
        {
            if (running.compareAndSet(false, true))
            {
                agents = new MatchingServiceInfoCache(SERVICE_TYPE, SCOPES, null, LANGUAGE);
                agents.addServiceRegistrationListener(this);
                StandardServiceAgent sa = new StandardServiceAgent();
                sa.setPort(port);
                sa.setScopes(SCOPES);
                serviceAgent = sa;

                StringBuffer url = new StringBuffer().append(SERVICE_TYPE).append("://").append(InetAddress.getLocalHost().getHostAddress());
                url.append(":").append(1427).append(nodeName);
                ServiceURL serviceURL = new ServiceURL(url.toString(), lifetime);
                ServiceInfo serviceInfo = new ServiceInfo(serviceURL, SCOPES, null, LANGUAGE);
                serviceAgent.register(serviceInfo);
                serviceAgent.start();

                StandardUserAgent ua = new StandardUserAgent();
                ua.setPort(port);
                ua.addMessageRegistrationListener(agents);
                userAgent = ua;
                userAgent.start();

                // Fill the cache with an initial query
                List agentsPresent = userAgent.findServices(SERVICE_TYPE, SCOPES, null, LANGUAGE);
                agents.putAll(agentsPresent);

                schedulePeriodicPurge();
            }
        }

        public void stop() throws Exception
        {
            if (running.compareAndSet(true, false))
            {
                scheduler.shutdown();

                if (serviceAgent != null) serviceAgent.stop();
                if (userAgent != null) userAgent.stop();
                agents.removeServiceRegistrationListener(this);
                agents.clear();
            }
        }

        protected void schedulePeriodicPurge()
        {
            scheduler.scheduleWithFixedDelay(this, 0L, 1L, TimeUnit.SECONDS);
        }

        public void run()
        {
            agents.purge();
        }

        protected void nodeBorn(String nodeName)
        {
        }

        protected void nodeDead(String nodeName)
        {
        }

        public void serviceRegistered(ServiceRegistrationEvent event)
        {
            ServiceInfo serviceInfo = event.getCurrentServiceInfo();
            String registeredNodeName = serviceInfo.getServiceURL().getURLPath();
            nodeBorn(registeredNodeName);
        }

        public void serviceUpdated(ServiceRegistrationEvent event)
        {
            // This node never updates the service it exposes
        }

        public void serviceDeregistered(ServiceRegistrationEvent event)
        {
            ServiceInfo serviceInfo = event.getPreviousServiceInfo();
            String deregisteredNodeName = serviceInfo.getServiceURL().getURLPath();
            nodeDead(deregisteredNodeName);
        }

        public void serviceExpired(ServiceRegistrationEvent event)
        {
            serviceDeregistered(event);
        }
    }

    private static class TestClusterNode extends ClusterNode
    {
        private final List nodes = new ArrayList();

        public TestClusterNode(int port, String nodeName, int lifetime)
        {
            super(port, nodeName, lifetime);
        }

        protected void nodeBorn(String nodeName)
        {
            nodes.add(nodeName);
        }

        protected void nodeDead(String nodeName)
        {
            nodes.remove(nodeName);
        }

        public List getNodes()
        {
            return nodes;
        }
    }
}
