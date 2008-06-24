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
package org.livetribe.slp.srv.sa;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.srv.da.DirectoryAgentInfo;

/**
 * A specialized ServiceInfo that holds the renewals that must be periodically issued.
 * Renewals are either issued to DirectoryAgents when they're present on the network,
 * or multicasted on the SLP notification port.
 */
public class SAServiceInfo extends ServiceInfo
{
    private final Map<DirectoryAgentInfo, ScheduledFuture> renewals = new HashMap<DirectoryAgentInfo, ScheduledFuture>();
    private ScheduledFuture notificationRenewal;

    public SAServiceInfo(ServiceInfo serviceInfo)
    {
        super(serviceInfo.getServiceType(), serviceInfo.getServiceURL(), serviceInfo.getLanguage(), serviceInfo.getScopes(), serviceInfo.getAttributes());
    }

    protected ServiceInfo clone(Scopes scopes, Attributes attributes)
    {
        ServiceInfo clone = super.clone(scopes, attributes);
        SAServiceInfo result = new SAServiceInfo(clone);
        result.renewals.putAll(renewals);
        result.notificationRenewal = notificationRenewal;
        return result;
    }

    public void putDirectoryAgentRenewal(DirectoryAgentInfo directoryAgent, ScheduledFuture renewal)
    {
        renewals.put(directoryAgent, renewal);
    }

    public void setNotificationRenewal(ScheduledFuture renewal)
    {
        this.notificationRenewal = renewal;
    }

    public void cancelRenewals()
    {
        for (ScheduledFuture renewal : renewals.values()) cancelRenewal(renewal);
        renewals.clear();
        if (notificationRenewal != null)
        {
            cancelRenewal(notificationRenewal);
            notificationRenewal = null;
        }
    }

    private void cancelRenewal(ScheduledFuture renewal)
    {
        renewal.cancel(true);
    }

    public void cancelDirectoryAgentRenewal(DirectoryAgentInfo directoryAgent)
    {
        ScheduledFuture renewal = renewals.get(directoryAgent);
        if (renewal != null) cancelRenewal(renewal);
    }
}
