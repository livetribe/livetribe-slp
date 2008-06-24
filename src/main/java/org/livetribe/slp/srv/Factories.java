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
package org.livetribe.slp.srv;

import org.livetribe.slp.settings.Key;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.settings.Defaults;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Revision$ $Date$
 */
public class Factories
{
    public static <T> T newInstance(Settings settings, Key<Class<T>> key)
    {
        Class<T> klass = settings == null ? Defaults.get(key) : settings.get(key, Defaults.get(key));
        if (klass == null) return null;
        return newInstance(klass);
    }

    private static <T> T newInstance(Class<T> klass)
    {
        try
        {
            return klass.newInstance();
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
