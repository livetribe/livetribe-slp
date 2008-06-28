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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        reload();
    }

    /**
     * @param key the configuration key
     * @return the default value for the given configuration key
     */
    public static <T> T get(Key<T> key)
    {
        return defaults.get(key);
    }

    /**
     * Reloads the default configuration, reinitializing all configuration values and configuration objects.
     * <br />
     * Reloading the configuration is needed because the default configuration contains {@link ExecutorService} objects
     * that may be {@link ExecutorService#shutdown()}. It is not possible to restart such executors after they have
     * been shut down.
     * <br />
     * Reloading the configuration ensures that all configuration objects are properly reinitialized.
     */
    public static void reload()
    {
        try
        {
            Settings settings = PropertiesSettings.from("livetribe-slp.properties");
            settings.put(Keys.EXECUTOR_SERVICE_KEY, Executors.newCachedThreadPool());
            settings.put(Keys.SCHEDULED_EXECUTOR_SERVICE_KEY, Executors.newSingleThreadScheduledExecutor());
            // Safe publication via volatile reference
            defaults = settings;
        }
        catch (IOException x)
        {
            throw new ServiceLocationException("Could not read default slp configuration", x, ServiceLocationException.INTERNAL_SYSTEM_ERROR);
        }
    }

    private Defaults()
    {
    }
}
