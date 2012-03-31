/*
 * Copyright 2006-2008 the original author or authors
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
package org.livetribe.slp.spi.filter;

import java.util.List;

import org.livetribe.slp.Attributes;


/**
 *
 */
public class OrFilter implements Filter
{
    private final List<Filter> filters;

    public OrFilter(List<Filter> filters)
    {
        this.filters = filters;
    }

    public boolean matches(Attributes attributes)
    {
        boolean result = false;
        for (Filter filter : filters)
        {
            result = result | filter.matches(attributes);
            if (result) break;
        }
        return result;
    }

    public String asString()
    {
        StringBuilder builder = new StringBuilder("(|");
        for (Filter filter : filters) builder.append(filter.asString());
        builder.append(")");
        return builder.toString();
    }
}
