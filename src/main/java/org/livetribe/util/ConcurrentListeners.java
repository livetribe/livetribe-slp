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
package org.livetribe.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;

/**
 * @version $Rev$ $Date$
 */
public class ConcurrentListeners
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final List listeners = new ArrayList();
    private final Lock lock = new ReentrantLock();

    public void add(EventListener listener)
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

    public void remove(EventListener listener)
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

    public void notify(String methodName, EventObject event)
    {
        List copy = null;
        lock.lock();
        try
        {
            if (listeners.isEmpty()) return;
            copy = new ArrayList(listeners);
        }
        finally
        {
            lock.unlock();
        }

        for (int i = 0; i < copy.size(); ++i)
        {
            Object listener = copy.get(i);
            Method method = getMethod(listener, methodName, event);
            invoke(method, listener, event);
        }
    }

    private void invoke(Method method, Object listener, EventObject event)
    {
        try
        {
            method.setAccessible(true);
            method.invoke(listener, new Object[]{event});
        }
        catch (IllegalAccessException x)
        {
            throw new RuntimeException(x);
        }
        catch (InvocationTargetException x)
        {
            Throwable cause = x.getCause();
            if (cause instanceof Error) throw (Error)cause;
            if (cause instanceof RuntimeException)
            {
                if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Listener " + listener + " threw exception from method " + method + ", ignoring", cause);
            }
            else
            {
                throw new RuntimeException(cause);
            }
        }
    }

    private Method getMethod(Object listener, String methodName, EventObject event)
    {
        try
        {
            return listener.getClass().getMethod(methodName, new Class[]{event.getClass()});
        }
        catch (NoSuchMethodException x)
        {
            throw new RuntimeException(x);
        }
    }
}
