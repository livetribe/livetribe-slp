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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version $Revision$ $Date$
 */
public class Editors
{
    private static final Map<Class<?>, List<Class<? extends PropertyEditor>>> editors = new HashMap<Class<?>, List<Class<? extends PropertyEditor>>>();

    private Editors()
    {
    }

    public static void register(Class<?> typeClass, Class<? extends PropertyEditor> editorClass)
    {
        List<Class<? extends PropertyEditor>> editorClasses = editors.get(typeClass);
        if (editorClasses == null)
        {
            editorClasses = new ArrayList<Class<? extends PropertyEditor>>();
            editors.put(typeClass, editorClasses);
        }
        editorClasses.add(0, editorClass);
    }

    public static <T> T convertFromString(Class<T> typeClass, String text)
    {
        T result = null;
        List<Class<? extends PropertyEditor>> editorClasses = editors.get(typeClass);
        if (editorClasses != null)
        {
            for (Class<? extends PropertyEditor> editorClass : editorClasses)
            {
                PropertyEditor editor = newPropertyEditor(editorClass);
                if (editor != null)
                {
                    try
                    {
                        result = (T)convertFromString(editor, text);
                        return result;
                    }
                    catch (IllegalArgumentException x)
                    {
                        // Continue with the next editor
                    }
                }
            }
        }

        // Fallback to standard editors
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
