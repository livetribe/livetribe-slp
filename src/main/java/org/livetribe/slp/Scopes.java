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

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @version $Rev$ $Date$
 */
public class Scopes
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String DEFAULT_SCOPE = "DEFAULT";

    /**
     * The DEFAULT scope is just like any other scope, only that's used by DA and SA as their default.
     */
    public static final Scopes DEFAULT = new Scopes(new String[]{DEFAULT_SCOPE});

    /**
     * The WILDCARD scope is special, as it does not match any scope, and all other scopes will match it.
     * This is used during queries, when one wants to retrieve services registered in all scopes.
     */
    public static final Scopes WILDCARD = new Scopes(null);

    private final String[] originalScopes;
    private final List scopes = new ArrayList();

    public Scopes(String[] scopes)
    {
        this.originalScopes = scopes == null ? EMPTY_STRING_ARRAY : scopes;
        if (scopes != null && scopes.length > 0)
        {
            for (int i = 0; i < scopes.length; ++i)
            {
                String scope = scopes[i];
                this.scopes.add(scope.toLowerCase());
            }
        }
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

    /**
     * Matches the given Scopes argument against this Scopes object.
     * If this Scopes is the wildcard scope, always returns false.
     * If the given Scopes is the wildcard scope, always returns true.
     * @param other The scopes to match
     * @return True if all scopes specified by the given Scopes argument are also scopes of this Scopes object.
     */
    public boolean match(Scopes other)
    {
        if (other == null || other.isWildcardScope()) return true;
        if (isWildcardScope()) return false;
        return scopes.containsAll(other.scopes);
    }

    /**
     * Matches the given Scopes argument against this Scopes object, more weakly than {@link #match(Scopes)}.
     * If this Scopes is the wildcard scope, always returns false.
     * If the given Scopes is the wildcard scope, always returns true.
     * @param other The scopes to match
     * @return True if at least one of the scopes specified by the given Scopes argument is also a scope of this Scopes object.
     */
    public boolean weakMatch(Scopes other)
    {
        if (other == null || other.isWildcardScope()) return true;
        if (isWildcardScope()) return false;
        return !Collections.disjoint(scopes, other.scopes);
    }

    public String[] asStringArray()
    {
        return originalScopes;
    }

    private boolean isWildcardScope()
    {
        return scopes.isEmpty();
    }

    public String toString()
    {
        return scopes.toString();
    }
}
