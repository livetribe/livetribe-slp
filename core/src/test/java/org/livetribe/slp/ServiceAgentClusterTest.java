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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.sa.ServiceEvent;
import org.livetribe.slp.sa.ServiceListener;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.MapSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.net.NetUtils;
import org.livetribe.slp.ua.UserAgent;
import org.testng.annotations.Test;

/**
 * Purpose of this test is to test how SLP can be used to implement cluster nodes
 * that are aware of each other.
 * Each cluster node advertise one service, and each node must have the list of all
 * services advertised by other nodes in the cluster.
 * The service may just be a unique node name.
 * SLP ServiceAgents are used to advertise the service, while SLP UserAgents are used to
 * listen for services advertised by ServiceAgents. An additional helper class acts as a
 * service cache, since UserAgents do not cache services but only give a way to actively
 * find them.
 *
 * @version $Rev$ $Date$
 */
public class ServiceAgentClusterTest
{
    private Settings newSettings()
    {
        Settings settings = new MapSettings();
        settings.put(PORT_KEY, 4427);
        return settings;
    }

    @Test
    public void testClusterWithOneNode() throws Exception
    {
        TestClusterNode node = new TestClusterNode("node", newSettings());
        node.start();
        try
        {
            Thread.sleep(500);
            assert node.getNodes().size() == 1;
            assert node.getNodes().contains(node.getNodeName());
        }
        finally
        {
            node.stop();
            Thread.sleep(500);
        }
    }

    @Test

    public void testClusterWithTwoNodes() throws Exception
    {
        TestClusterNode node1 = new TestClusterNode("node1", newSettings());
        node1.start();
        Thread.sleep(500);

        try
        {
            TestClusterNode node2 = new TestClusterNode("node2", newSettings());
            node2.start();
            Thread.sleep(500);

            try
            {
                assert node1.getNodes().size() == 2;
                assert node1.getNodes().contains(node1.getNodeName());
                assert node1.getNodes().contains(node2.getNodeName());

                assert node2.getNodes().size() == 2;
                assert node2.getNodes().contains(node2.getNodeName());
                assert node2.getNodes().contains(node1.getNodeName());
            }
            finally
            {
                node2.stop();
                Thread.sleep(500);
            }

            assert node1.getNodes().size() == 1;
            assert node1.getNodes().contains(node1.getNodeName());
        }
        finally
        {
            node1.stop();
            Thread.sleep(500);
        }
    }

    private static class ClusterNode implements ServiceListener
    {
        private static final ServiceType SERVICE_TYPE = new ServiceType("service:cluster:slp");
        private static final int LIFETIME = 10;
        private static final String LANGUAGE = Locale.ENGLISH.getLanguage();
        private static final Scopes SCOPES = Scopes.DEFAULT;

        private final String nodeName;
        private final AtomicBoolean running = new AtomicBoolean();
        private MatchingServiceInfoCache nodes;
        private ServiceAgent serviceAgent;
        private UserAgent userAgent;

        public ClusterNode(String nodeName, Settings settings)
        {
            this.nodeName = nodeName;
            this.nodes = new MatchingServiceInfoCache(SERVICE_TYPE, LANGUAGE, SCOPES, null);
            this.serviceAgent = SLP.newServiceAgent(settings);
            this.userAgent = SLP.newUserAgent(settings);
        }

        public String getNodeName()
        {
            return nodeName;
        }

        public void start()
        {
            if (running.compareAndSet(false, true))
            {
                // Notifies this node for changes in the nodes cache
                nodes.addServiceListener(this);

                // Notifies the nodes cache when nodes born or die
                userAgent.addServiceNotificationListener(nodes);
                userAgent.start();

                // Fill the nodes cache with the initial nodes
                List<ServiceInfo> nodesPresent = userAgent.findServices(SERVICE_TYPE, LANGUAGE, SCOPES, null);
                nodes.addAll(nodesPresent);

                StringBuilder url = new StringBuilder();
                url.append(SERVICE_TYPE.asString()).append("://").append(NetUtils.getLocalhost().getHostAddress());
                url.append("/").append(nodeName);
                ServiceURL serviceURL = new ServiceURL(url.toString(), LIFETIME);
                ServiceInfo serviceInfo = new ServiceInfo(serviceURL, LANGUAGE, SCOPES, null);
                serviceAgent.register(serviceInfo);
                serviceAgent.start();
            }
        }

        public void stop() throws Exception
        {
            if (running.compareAndSet(true, false))
            {
                if (serviceAgent != null) serviceAgent.stop();
                if (userAgent != null) userAgent.stop();
                nodes.removeServiceListener(this);
            }
        }

        protected void nodeBorn(String nodeName)
        {
        }

        protected void nodeDead(String nodeName)
        {
        }

        public void serviceAdded(ServiceEvent event)
        {
            ServiceInfo service = event.getCurrentService();
            String registeredNodeName = service.getServiceURL().getURLPath();
            if (registeredNodeName.startsWith("/")) registeredNodeName = registeredNodeName.substring(1);
            nodeBorn(registeredNodeName);
        }

        public void serviceUpdated(ServiceEvent event)
        {
            // This node never updates the service it exposes
        }

        public void serviceRemoved(ServiceEvent event)
        {
            ServiceInfo service = event.getPreviousService();
            String deregisteredNodeName = service.getServiceURL().getURLPath();
            if (deregisteredNodeName.startsWith("/")) deregisteredNodeName = deregisteredNodeName.substring(1);
            nodeDead(deregisteredNodeName);
        }
    }

    private static class TestClusterNode extends ClusterNode
    {
        private final List<String> nodes = new ArrayList<String>();

        public TestClusterNode(String nodeName, Settings settings)
        {
            super(nodeName, settings);
        }

        protected void nodeBorn(String nodeName)
        {
            nodes.add(nodeName);
        }

        protected void nodeDead(String nodeName)
        {
            nodes.remove(nodeName);
        }

        public List<String> getNodes()
        {
            return nodes;
        }
    }
}
