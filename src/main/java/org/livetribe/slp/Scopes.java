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
 * Scopes are case insensitive string labels used to group together related services.
 * Both DirectoryAgent and ServiceAgent have assigned one or more scopes so that they can advertise services
 * belonging to the scopes they've been assigned. The scope assigned by default to DirectoryAgents and
 * UserAgents is the {@link #DEFAULT} scope.
 * @version $Rev$ $Date$
 */
public class Scopes
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * The <code>DEFAULT</code> scope is just like any other scope, only that's used by DirectoryAgents and
     * ServiceAgents as their default scope.
     * @see #WILDCARD
     */
    public static final Scopes DEFAULT = new Scopes(new String[]{"DEFAULT"});

    /**
     * The WILDCARD scope is special, as it does not match any scope, and all other scopes will match it.
     * It may be used during queries, when one wants to retrieve services registered in all scopes.
     * @see #DEFAULT
     */
    public static final Scopes WILDCARD = new Scopes(null);

    private final String[] originalScopes;
    private final List scopes = new ArrayList();

    /**
     * Creates a <code>Scopes</code> object containing the given scope strings.
     * @param scopes The scope strings to be contained by this <code>Scopes</code> object
     */
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
     * Matches the given <code>Scopes</code> argument against this <code>Scopes</code> object.
     * If this <code>Scopes</code> is the wildcard scope, always returns false.
     * If the given <code>Scopes</code> is the wildcard scope, always returns true.
     * @param other The <code>Scopes</code> to match
     * @return True if all scopes specified by the given <code>Scopes</code> argument
     * are also scopes of this <code>Scopes</code> object.
     * @see #weakMatch(Scopes)
     */
    public boolean match(Scopes other)
    {
        if (isWildcardScope()) return false;
        if (other == null || other.isWildcardScope()) return true;
        return scopes.containsAll(other.scopes);
    }

    /**
     * Matches the given <code>Scopes</code> argument against this <code>Scopes</code> object,
     * more weakly than {@link #match(Scopes)}.
     * If this <code>Scopes</code> is the wildcard scope, always returns false.
     * If the given <code>Scopes</code> is the wildcard scope, always returns true.
     * @param other The <code>Scopes</code> to match
     * @return True if at least one of the scopes specified by the given <code>Scopes</code> argument
     * is also a scope of this <code>Scopes</code> object.
     * @see #match(Scopes)
     */
    public boolean weakMatch(Scopes other)
    {
        if (isWildcardScope()) return false;
        if (other == null || other.isWildcardScope()) return true;
        return !Collections.disjoint(scopes, other.scopes);
    }

    /**
     * Returns the scope strings contained by this <code>Scopes</code> object.
     */
    public String[] asStringArray()
    {
        return originalScopes;
    }

    /**
     * Returns true if this <code>Scopes</code> object is the wildcard scope, false otherwise.
     * @see #WILDCARD
     */
    public boolean isWildcardScope()
    {
        return equals(WILDCARD);
    }

    /**
     * Returns true if this <code>Scopes</code> object is the default scope, false otherwise.
     * @see #DEFAULT
     */
    public boolean isDefaultScope()
    {
        return equals(DEFAULT);
    }

    public String toString()
    {
        return scopes.toString();
    }
}
