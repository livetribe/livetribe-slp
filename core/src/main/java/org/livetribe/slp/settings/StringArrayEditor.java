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

import java.beans.PropertyEditorSupport;


/**
 * PropertyEditor for java.lang.String[].
 */
public class StringArrayEditor extends PropertyEditorSupport
{
    private static final String[] EMPTY_STRINGS = new String[0];

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        if (text == null || text.trim().length() == 0)
            setValue(EMPTY_STRINGS);
        else
            setValue(text.split(","));
    }
}
