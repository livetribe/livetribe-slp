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

import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.ArrayList;

/**
 * @version $Revision$ $Date$
 */
public class IntArrayEditor extends PropertyEditorSupport
{
    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        String[] values = text.split(",");
        List<Integer> intValues = new ArrayList<Integer>();
        for (String value : values)
        {
            Integer intValue = convert(value.trim());
            intValues.add(intValue);
        }
        int[] result = new int[intValues.size()];
        for (int i = 0; i < intValues.size(); i++)
        {
            Integer intValue = intValues.get(i);
            result[i] = intValue;
        }
        setValue(result);
    }

    private Integer convert(String value)
    {
        try
        {
            return Integer.valueOf(value);
        }
        catch (NumberFormatException x)
        {
            throw new IllegalArgumentException(x);
        }
    }
}
