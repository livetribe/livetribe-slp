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
package org.livetribe.slp;

import java.util.Arrays;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @version $Rev$ $Date$
 */
public class Scopes
{
    private static final String DEFAULT_SCOPE = "DEFAULT";
    public static final Scopes DEFAULT = new Scopes(new String[]{DEFAULT_SCOPE});

    private final List scopes;

    public Scopes(String[] scopes)
    {
        this.scopes = Arrays.asList(scopes);
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final Scopes that = (Scopes)obj;
        return scopes.equals(that.scopes);
    }

    public int hashCode()
    {
        return scopes.hashCode();
    }

    public boolean match(Scopes other)
    {
        if (containsDefaultScope()) return true;
        if (other == null) return false;
        return !Collections.disjoint(scopes, other.scopes);
    }

    public String[] asStringArray()
    {
        return (String[])scopes.toArray(new String[scopes.size()]);
    }

    private boolean containsDefaultScope()
    {
        for (int i = 0; i < scopes.size(); ++i)
        {
            String scope = (String)scopes.get(i);
            if (scope.equalsIgnoreCase(DEFAULT_SCOPE)) return true;
        }
        return false;
    }
}
