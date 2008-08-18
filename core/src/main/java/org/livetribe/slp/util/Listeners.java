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
package org.livetribe.slp.util;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @version $Revision$ $Date$
 */
public class Listeners<T extends EventListener> implements Iterable<T>
{
    private final List<T> listeners = new ArrayList<T>();
    private final Lock lock = new ReentrantLock();

    public void add(T listener)
    {
        lock.lock();
        try
        {
            listeners.add(listener);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void remove(T listener)
    {
        lock.lock();
        try
        {
            listeners.remove(listener);
        }
        finally
        {
            lock.unlock();
        }
    }

    public Iterator<T> iterator()
    {
        lock.lock();
        try
        {
            return new ArrayList<T>(listeners).iterator();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void clear()
    {
        lock.lock();
        try
        {
            listeners.clear();
        }
        finally
        {
            lock.unlock();
        }
    }
}
