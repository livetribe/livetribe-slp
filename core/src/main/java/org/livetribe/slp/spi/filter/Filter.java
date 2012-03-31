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

import org.livetribe.slp.Attributes;


/**
 * A LDAPv3 expression used to match {@link Attributes}.
 * <br />
 * Instances of this class are created via {@link FilterParser}.
 */
public interface Filter
{
    /**
     * @param attributes The attributes to match against this filter.
     * @return true if the given attributes matches the expression represented by this filter.
     */
    public boolean matches(Attributes attributes);

    /**
     * @return a string representation of this filter
     * @see FilterParser#parse(String)
     */
    public String asString();
}
