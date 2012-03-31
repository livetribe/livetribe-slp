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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;


/**
 * A {@link Properties} based implementation of {@link Settings}.
 */
public class PropertiesSettings implements Settings
{
    /**
     * Creates a new PropertiesSettings from the given properties file.
     *
     * @param file the properties file to read
     * @return a new PropertiesSettings
     * @throws IOException if the properties file cannot be read
     */
    public static PropertiesSettings from(File file) throws IOException
    {
        FileInputStream stream = new FileInputStream(file);
        return from(stream);
    }

    /**
     * Creates a new PropertiesSettings from the given resource in the classpath of the context ClassLoader.
     *
     * @param resource the resource to read
     * @return a new PropertiesSettings
     * @throws IOException if the resource cannot be read
     * @see #from(String, ClassLoader)
     */
    public static PropertiesSettings from(String resource) throws IOException
    {
        return from(resource, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a new PropertiesSettings from the given resource in the classpath of the given ClassLoader.
     *
     * @param resource    the resource to read
     * @param classLoader the ClassLoader to use to read the resource
     * @return a new PropertiesSettings
     * @throws IOException if the resource canno be read
     */
    public static PropertiesSettings from(String resource, ClassLoader classLoader) throws IOException
    {
        URL url = classLoader.getResource(resource);
        return from(url);
    }

    /**
     * Creates a new PropertiesSettings from the given URL.
     *
     * @param url the URL to read
     * @return a new PropertiesSettings
     * @throws IOException if the URL cannot be read
     */
    public static PropertiesSettings from(URL url) throws IOException
    {
        InputStream stream = null;
        try
        {
            stream = url.openStream();
            return from(stream);
        }
        finally
        {
            if (stream != null) stream.close();
        }
    }

    /**
     * Creates a new PropertiesSettings from the given input stream
     *
     * @param stream the stream to read
     * @return a new PropertiesSettings
     * @throws IOException if the stream cannot be read
     */
    public static PropertiesSettings from(InputStream stream) throws IOException
    {
        Properties properties = new Properties();
        properties.load(stream);
        return from(properties);
    }

    /**
     * Creates a new PropertiesSettings from the given {@link Properties} object.
     *
     * @param properties the properties object to read
     * @return a new PropertiesSettings
     */
    public static PropertiesSettings from(Properties properties)
    {
        return new PropertiesSettings(properties);
    }

    private final Properties properties;

    private PropertiesSettings(Properties properties)
    {
        this.properties = properties;
    }

    public <K> K get(Key<K> key)
    {
        Object value = properties.get(key.getKey());
        return key.convert(value);
    }

    public <V> V get(Key<V> key, V defaultValue)
    {
        if (containsKey(key)) return get(key);
        return defaultValue;
    }

    public boolean containsKey(Key<?> key)
    {
        return properties.containsKey(key.getKey());
    }

    public <V> void put(Key<? super V> key, V value)
    {
        properties.put(key.getKey(), value);
    }

    public <K> K remove(Key<K> key)
    {
        Object value = properties.remove(key.getKey());
        return key.convert(value);
    }
}
