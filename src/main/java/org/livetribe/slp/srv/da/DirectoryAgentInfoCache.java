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
package org.livetribe.slp.srv.da;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.livetribe.slp.Scopes;
import org.livetribe.slp.da.DirectoryAgentEvent;
import org.livetribe.slp.da.DirectoryAgentInfo;
import org.livetribe.slp.da.DirectoryAgentListener;
import org.livetribe.slp.srv.filter.Filter;
import org.livetribe.util.Listeners;

/**
 * A thread-safe class that caches {@link DirectoryAgentInfo}.
 * <br />
 *
 * @version $Rev: 157 $ $Date: 2006-06-05 23:29:25 +0200 (Mon, 05 Jun 2006) $
 */
public class DirectoryAgentInfoCache
{
    private final Lock lock = new ReentrantLock();
    private final Map<DirectoryAgentInfo.Key, DirectoryAgentInfo> cache = new HashMap<DirectoryAgentInfo.Key, DirectoryAgentInfo>();
    private final Listeners<DirectoryAgentListener> listeners = new Listeners<DirectoryAgentListener>();

    public boolean add(DirectoryAgentInfo directoryAgent)
    {
        lock.lock();
        try
        {
            return cache.put(directoryAgent.getKey(), directoryAgent) == null;
        }
        finally
        {
            lock.unlock();
        }
    }

    public void addAll(List<DirectoryAgentInfo> directoryAgents)
    {
        lock.lock();
        try
        {
            for (DirectoryAgentInfo directoryAgent : directoryAgents) add(directoryAgent);
        }
        finally
        {
            lock.unlock();
        }
    }

    public boolean remove(DirectoryAgentInfo.Key key)
    {
        lock.lock();
        try
        {
            return cache.remove(key) != null;
        }
        finally
        {
            lock.unlock();
        }
    }

    public void removeAll()
    {
        lock.lock();
        try
        {
            for (Map.Entry<DirectoryAgentInfo.Key, DirectoryAgentInfo> entry : cache.entrySet()) remove(entry.getKey());
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Returns the DirectoryAgentInfo that match the given scopes and attributes filter.
     *
     * @param scopes The scopes to match, or null for any scopes
     * @param filter The filter to match, or null for any attributes
     */
    public List<DirectoryAgentInfo> match(Scopes scopes, Filter filter)
    {
        List<DirectoryAgentInfo> result = new ArrayList<DirectoryAgentInfo>();
        lock.lock();
        try
        {
            for (DirectoryAgentInfo info : cache.values())
            {
                if (info.matchScopes(scopes) && info.matchFilter(filter)) result.add(info);
            }
            return result;
        }
        finally
        {
            lock.unlock();
        }
    }

    public void addDirectoryAgentListener(DirectoryAgentListener listener)
    {
        listeners.add(listener);
    }

    public void removeDirectoryAgentListener(DirectoryAgentListener listener)
    {
        listeners.remove(listener);
    }

    public void handle(DirectoryAgentInfo directoryAgent)
    {
        if (directoryAgent.isShuttingDown())
        {
            boolean removed = remove(directoryAgent.getKey());
            if (removed) notifyDirectoryAgentDied(directoryAgent);
        }
        else
        {
            boolean added = add(directoryAgent);
            if (added) notifyDirectoryAgentBorn(directoryAgent);
        }
    }

    private void notifyDirectoryAgentBorn(DirectoryAgentInfo directoryAgent)
    {
        DirectoryAgentEvent event = new DirectoryAgentEvent(this, directoryAgent);
        for (DirectoryAgentListener listener : listeners) listener.directoryAgentBorn(event);
    }

    private void notifyDirectoryAgentDied(DirectoryAgentInfo directoryAgent)
    {
        DirectoryAgentEvent event = new DirectoryAgentEvent(this, directoryAgent);
        for (DirectoryAgentListener listener : listeners) listener.directoryAgentDied(event);
    }
}
