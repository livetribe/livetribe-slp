/*
 * Copyright 2005 the original author or authors
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

import java.util.HashMap;
import java.util.Map;

/**
 * @version $Revision$ $Date$
 */
public class MapSettings implements Settings
{
    private Map<Key<?>, Object> properties = new HashMap<Key<?>, Object>();

    public <V> V get(Key<V> key)
    {
        return (V)properties.get(key);
    }

    public <V> V get(Key<V> key, V defaultValue)
    {
        if (containsKey(key)) return get(key);
        return defaultValue;
    }

    public boolean containsKey(Key<?> key)
    {
        return properties.containsKey(key);
    }

    public <V> void put(Key<? super V> key, V value)
    {
        properties.put(key, value);
    }

    public <K> K remove(Key<K> key)
    {
        return (K)properties.remove(key);
    }
}
