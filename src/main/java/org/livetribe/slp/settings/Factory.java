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
 * Creates new instances of classes whose full qualified name is specified in configuration settings under
 * a certain key.
 *
 * @version $Revision$ $Date$
 */
public class Factory
{
    /**
     * Creates a new instance of a class whose full qualified name is specified under the given key.
     * <br />
     * If the given settings is null, or it does not contain the specified key, the default value of the key
     * is taken from the {@link Defaults defaults}.
     *
     * @param settings the configuration settings that may specify the full qualified name of the class to create,
     *                 overriding the default value
     * @param key      the key under which the full qualified name of the class is specified
     * @return a new instance of the class specified by the given key
     * @throws ServiceLocationException if the instance cannot be created
     */
    public static <T> T newInstance(Settings settings, Key<String> key) throws ServiceLocationException
    {
        Class<?> klass = loadClass(settings, key);
        // Workaround for compiler bug (#6302954)
        return Factory.<T>newInstance(klass);
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

    private Factory()
    {
    }
}
