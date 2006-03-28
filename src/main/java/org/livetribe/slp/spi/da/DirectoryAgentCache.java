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
package org.livetribe.slp.spi.da;

import java.util.List;
import java.util.ArrayList;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @version $Rev$ $Date$
 */
public class DirectoryAgentCache
{
    private final Lock lock = new ReentrantLock();
    private final List cache = new ArrayList();

    public boolean add(DirectoryAgentInfo info)
    {
        lock.lock();
        try
        {
            return cache.add(info);
        }
        finally
        {
            lock.unlock();
        }
    }

    public List getByScopes(String[] scopes)
    {
        lock.lock();
        try
        {
            List scopesList = Arrays.asList(scopes);
            List result = new ArrayList();
            for (int i = 0; i < cache.size(); ++i)
            {
                DirectoryAgentInfo info = (DirectoryAgentInfo)cache.get(i);
                if (info.matchScopes(scopesList)) result.add(info);
            }
            return result;
        }
        finally
        {
            lock.unlock();
        }
    }

    public void addAll(List infos)
    {
        lock.lock();
        try
        {
            cache.addAll(infos);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void remove(DirectoryAgentInfo info)
    {
        lock.lock();
        try
        {
            cache.remove(info);
        }
        finally
        {
            lock.unlock();
        }
    }
}
