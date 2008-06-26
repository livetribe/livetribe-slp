/*
 * Copyright 2005-2008 the original author or authors
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
package org.livetribe.slp.settings;

import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Revision$ $Date$
 */
public class Factories
{
    public static <T> T newInstance(Settings settings, Key<String> key)
    {
        Class<?> klass = loadClass(settings, key);
        // Workaround for compiler bug (#6302954)
        return Factories.<T>newInstance(klass);
    }

    private static Class<?> loadClass(Settings settings, Key<String> key)
    {
        String className = settings == null ? Defaults.get(key) : settings.get(key, Defaults.get(key));
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try
        {
            return loader.loadClass(className);
        }
        catch (ClassNotFoundException x)
        {
            throw new ServiceLocationException("Could not instantiate " + className, ServiceLocationException.INTERNAL_SYSTEM_ERROR);
        }
    }

    private static <T> T newInstance(Class<?> klass)
    {
        try
        {
            return (T)klass.newInstance();
        }
        catch (Exception x)
        {
            throw new ServiceLocationException("Could not instantiate " + klass, ServiceLocationException.INTERNAL_SYSTEM_ERROR);
        }
    }

    private Factories()
    {
    }
}
