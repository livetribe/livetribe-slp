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

import java.io.IOException;

import org.livetribe.slp.ServiceLocationException;

/**
 * Gives read-only access to the default configuration values and default configuration objects.
 *
 * @version $Revision$ $Date$
 */
public class Defaults
{
    private static volatile Settings defaults;

    static
    {
        try
        {
            defaults = PropertiesSettings.from("livetribe-slp.properties");
        }
        catch (IOException x)
        {
            throw new ServiceLocationException("Could not read default slp configuration", x, ServiceLocationException.Error.INTERNAL_SYSTEM_ERROR);
        }
    }

    /**
     * @param key the configuration key
     * @return the default value for the given configuration key
     */
    public static <T> T get(Key<T> key)
    {
        return defaults.get(key);
    }

    private Defaults()
    {
    }
}
