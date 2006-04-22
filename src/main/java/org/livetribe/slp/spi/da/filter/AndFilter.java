/*
 * Copyright 2006 the original author or authors
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
package org.livetribe.slp.spi.da.filter;

import java.util.List;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Rev$ $Date$
 */
public class AndFilter implements Filter
{
    private final List filters;

    public AndFilter(List filters)
    {
        this.filters = filters;
    }

    public boolean match(Attributes attributes) throws ServiceLocationException
    {
        boolean result = true;
        for (int i = 0; i < filters.size(); ++i)
        {
            Filter filter = (Filter)filters.get(i);
            result &= filter.match(attributes);
            if (!result) break;
        }
        return result;
    }
}
