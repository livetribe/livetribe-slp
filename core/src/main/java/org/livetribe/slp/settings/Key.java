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

/**
 * Holds a pair represented by a string key, as one can find in properties files, and the class of the value
 * associated with this Key.
 *
 * @version $Revision$ $Date$
 */
public class Key<T>
{
    static
    {
        Editors.register(Boolean.class, BooleanEditor.class);
        Editors.register(Integer.class, IntegerEditor.class);
        Editors.register(String[].class, StringArrayEditor.class);
        Editors.register(int[].class, IntArrayEditor.class);
    }

    /**
     * Creates a new Key with the given key string and value class.
     *
     * @param key        the key string representing this Key
     * @param valueClass the class of the value associated with this Key
     * @return a new Key with the given key string and value class
     */
    public static <S> Key<S> from(String key, Class<? extends S> valueClass)
    {
        return new Key<S>(key, valueClass);
    }

    private final String key;
    private final Class<?> valueClass;

    /**
     * Creates a new Key.
     *
     * @param key        the key string
     * @param valueClass the value type
     */
    protected Key(String key, Class<?> valueClass)
    {
        this.key = key;
        this.valueClass = valueClass;
    }

    /**
     * @return the key string associated to this Key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * @return the class of the value associated to this Key
     */
    public Class<T> getValueClass()
    {
        return (Class<T>)valueClass;
    }

    /**
     * Converts the given value object into an object of the value class specified by this Key.
     *
     * @param value the value to convert
     * @return an object of the value class specified by this Key
     * @see #convertFromString(String)
     */
    public T convert(Object value)
    {
        T result = null;
        if (value != null)
        {
            if (getValueClass().isInstance(value))
                result = getValueClass().cast(value);
            else
                result = convertFromString(value.toString());
        }
        return result;
    }

    /**
     * Converts the given string value into an object of the value class specified by this Key using PropertyEditors.
     *
     * @param stringValue the string value to convert
     * @return an object of the value class specified by this Key
     * @see Editors#convertFromString(String, Class)
     */
    protected T convertFromString(String stringValue)
    {
        return Editors.convertFromString(stringValue, getValueClass());
    }
}
