/**
 *
 * Copyright 2008 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.osgi.util;

import java.util.Dictionary;

import org.livetribe.slp.settings.Key;
import org.livetribe.slp.settings.Settings;


/**
 * A {@link Dictionary} based implementation of {@link Settings}.
 *
 * @version $Revision$ $Date$
 */
public class DictionarySettings implements Settings
{
    /**
     * Creates a new PropertiesSettings from the given {@link Dictionary} object.
     *
     * @param dictionary the properties object to read
     * @return a new PropertiesSettings
     */
    public static DictionarySettings from(Dictionary dictionary)
    {
        return new DictionarySettings(dictionary);
    }

    private final Dictionary dictionary;

    private DictionarySettings(Dictionary dictionary)
    {
        this.dictionary = dictionary;
    }

    /**
     * {@inheritDoc}
     */
    public <K> K get(Key<K> key)
    {
        Object value = dictionary.get(key.getKey());
        return key.convert(value);
    }

    /**
     * {@inheritDoc}
     */
    public <V> V get(Key<V> key, V defaultValue)
    {
        if (containsKey(key)) return get(key);
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Key<?> key)
    {
        return dictionary.get(key.getKey()) != null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public <V> void put(Key<? super V> key, V value)
    {
        dictionary.put(key.getKey(), value);
    }

    /**
     * {@inheritDoc}
     */
    public <K> K remove(Key<K> key)
    {
        Object value = dictionary.remove(key.getKey());
        return key.convert(value);
    }
}
