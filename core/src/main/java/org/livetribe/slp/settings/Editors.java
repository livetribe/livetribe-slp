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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is equivalent to {@link PropertyEditorManager} and it is used to avoid polluting PropertyEditorManager
 * with property editors used only by the LiveTribe SLP implementation.
 *
 * @version $Revision$ $Date$
 */
public class Editors
{
    private static final Map<Class<?>, Class<? extends PropertyEditor>> editors = new HashMap<Class<?>, Class<? extends PropertyEditor>>();

    private Editors()
    {
    }

    /**
     * Registers the given editor class to be used to edit values of the given type class.
     *
     * @param typeClass   the class of objects to be edited
     * @param editorClass the editor class
     */
    public static void register(Class<?> typeClass, Class<? extends PropertyEditor> editorClass)
    {
        synchronized (editors)
        {
            editors.put(typeClass, editorClass);
        }
    }

    /**
     * Converts the given text string to an object of the given type class.
     * <br />
     * If no editor is registered for the given type class, or if it fails the conversion, the default
     * property editor from {@link PropertyEditorManager} is tried.
     *
     * @param text      the text string to convert into an object
     * @param typeClass the class of the object to return
     * @return an object of the given type class converted from the given text string
     */
    public static <T> T convertFromString(String text, Class<T> typeClass)
    {
        Class<? extends PropertyEditor> editorClass = null;
        synchronized (editors)
        {
            editorClass = editors.get(typeClass);
        }
        if (editorClass != null)
        {
            PropertyEditor editor = newPropertyEditor(editorClass);
            if (editor != null)
            {
                try
                {
                    return (T)convertFromString(editor, text);
                }
                catch (IllegalArgumentException x)
                {
                    // Fallback to the default editors
                }
            }
        }

        PropertyEditor editor = PropertyEditorManager.findEditor(typeClass);
        if (editor != null) return (T)convertFromString(editor, text);

        return null;
    }

    private static PropertyEditor newPropertyEditor(Class<? extends PropertyEditor> editorClass)
    {
        try
        {
            return editorClass.newInstance();
        }
        catch (Exception x)
        {
            return null;
        }
    }

    private static Object convertFromString(PropertyEditor editor, String text) throws IllegalArgumentException
    {
        editor.setAsText(text);
        return editor.getValue();
    }
}
