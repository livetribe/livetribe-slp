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

import java.util.Map;


/**
 * The configuration settings used throughout LiveTribe SLP.
 * <br />
 * Each configuration setting is made of a {@link Key} and a value.
 * The interface of this class is similar to that of {@link Map} except
 * that each key defines the type of the corrispondent value.
 *
 * @see Defaults
 * @see Keys
 */
public interface Settings
{
    /**
     * Gets the value associated with the given key.
     *
     * @param key the key whose associated value is returned
     * @return the value associated to the given key, or null if there is no presence of the given key
     */
    public <V> V get(Key<V> key);

    /**
     * Gets the value associated with the given key, or the given default value if
     * there is no association for the given key.
     *
     * @param key          the key whose associated value is returned if the key is present
     * @param defaultValue the value returned if the key is not present
     * @return the value associated to the given key, or the given default value if
     *         the given key is not present
     */
    public <V> V get(Key<V> key, V defaultValue);

    /**
     * @param key the key to test for presence
     * @return whether the given key is present
     */
    public boolean containsKey(Key<?> key);

    /**
     * Associates the given value to the given key.
     *
     * @param key   the key to associate the value with
     * @param value the value to be associated with the key
     */
    public <V> void put(Key<? super V> key, V value);

    /**
     * Removes the given key and the associated value.
     *
     * @param key the key to remove
     * @return the value associated with the given key
     */
    public <V> V remove(Key<V> key);
}
