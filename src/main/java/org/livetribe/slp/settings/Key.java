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

/**
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
        Editors.register(Class.class, ClassEditor.class);
    }

    public static <S> Key<S> forType(String key, Class<? extends S> type)
    {
        return new Key<S>(key, type);
    }

    public static <S> Key<Class<S>> forClass(String key, Class<? extends S> type)
    {
        return new Key<Class<S>>(key, type.getClass());
    }

    private final String key;
    private final Class<?> type;

    protected Key(String key, Class<?> type)
    {
        this.key = key;
        this.type = type;
    }

    public String getKey()
    {
        return key;
    }

    public Class<T> getType()
    {
        return (Class<T>)type;
    }

    public T convert(Object value)
    {
        T result = null;
        if (value != null)
        {
            if (getType().isInstance(value))
                result = getType().cast(value);
            else
                result = convertFromString(value.toString());
        }
        return result;
    }

    protected T convertFromString(String value)
    {
        return Editors.convertFromString(getType(), value);
    }
}
