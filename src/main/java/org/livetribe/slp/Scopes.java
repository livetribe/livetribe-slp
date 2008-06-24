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
package org.livetribe.slp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Scopes are case insensitive string labels used to group together related services.
 * <br />
 * Both DirectoryAgent and ServiceAgent have assigned one or more scopes so that they can advertise services
 * belonging to the scopes they've been assigned. The scope assigned by default to DirectoryAgents and
 * UserAgents is the {@link #DEFAULT} scope.
 *
 * @version $Rev$ $Date$
 */
public class Scopes
{
    private static final char ESCAPE_PREFIX = '\\';
    private static final char[] reservedChars = new char[128];

    static
    {
        reservedChars['!'] = '!';
        reservedChars['('] = '(';
        reservedChars[')'] = ')';
        reservedChars['*'] = '*';
        reservedChars['+'] = '+';
        reservedChars[','] = ',';
        reservedChars[';'] = ';';
        reservedChars['<'] = '<';
        reservedChars['='] = '=';
        reservedChars['>'] = '>';
        reservedChars['\\'] = '\\';
        reservedChars['~'] = '~';
    }

    /**
     * The <code>DEFAULT</code> scope is just like any other scope, only that's used by DirectoryAgents and
     * ServiceAgents as their default scope.
     *
     * @see #NONE
     * @see #ANY
     */
    public static final Scopes DEFAULT = new Scopes(new String[]{"DEFAULT"}, true);

    /**
     * The {@link #NONE} scope is special, as it does not match any other scope, but any other scopes can match it.
     * It may be used during queries, when one wants to retrieve services registered in any other scopes.
     *
     * @see #ANY
     */
    public static final Scopes NONE = new Scopes(new String[0], true);

    /**
     * The {@link #ANY} scope is special, as it matches any other scope, but no other scope can match it.
     * It is the opposite of {@link #NONE}.
     */
    public static final Scopes ANY = new Scopes(new String[]{"*"}, false);

    /**
     * Creates a Scopes object from the given strings.
     *
     * @param scopes the scope strings
     * @return a new Scope instance
     */
    public static Scopes from(String... scopes)
    {
        if (scopes == null || scopes.length == 0) return NONE;
        return new Scopes(scopes, true);
    }

    private final List<String> scopes = new ArrayList<String>();

    /**
     * Creates a <code>Scopes</code> object containing the given scope strings.
     *
     * @param scopes the scope strings to be contained by this <code>Scopes</code> object
     * @param escape true if the strings must be escaped, false if the string must not be escaped
     */
    private Scopes(String[] scopes, boolean escape)
    {
        for (String scope : scopes)
        {
            scope = scope.toLowerCase();
            this.scopes.add(escape ? escape(scope) : scope);
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
     * <br />
     * If this <code>Scopes</code> is the {@link #NONE} scope, always returns false. <br />
     * If this <code>Scopes</code> is the {@link #ANY} scope, always returns true. <br />
     * If the given <code>Scopes</code> is the {@link #NONE} scope, always returns true. <br />
     * If the given <code>Scopes</code> is the {@link #ANY} scope, always returns false. <br />
     *
     * @param other the <code>Scopes</code> to match
     * @return true if all scopes specified by the given <code>Scopes</code> argument
     *         are also scopes of this <code>Scopes</code> object.
     * @see #weakMatch(Scopes)
     */
    public boolean match(Scopes other)
    {
        if (isNoneScope()) return false;
        if (isAnyScope()) return true;
        if (other == null || other.isNoneScope()) return true;
        if (other.isAnyScope()) return false;
        return scopes.containsAll(other.scopes);
    }

    /**
     * Matches the given <code>Scopes</code> argument against this <code>Scopes</code> object,
     * more weakly than {@link #match(Scopes)}.
     * <br />
     * If this <code>Scopes</code> is the {@link #NONE} scope, always returns false. <br />
     * If this <code>Scopes</code> is the {@link #ANY} scope, always returns true. <br />
     * If the given <code>Scopes</code> is the {@link #NONE} scope, always returns true. <br />
     * If the given <code>Scopes</code> is the {@link #ANY} scope, always returns false. <br />
     *
     * @param other the <code>Scopes</code> to match
     * @return true if at least one of the scopes specified by the given <code>Scopes</code> argument
     *         is also a scope of this <code>Scopes</code> object.
     * @see #match(Scopes)
     */
    public boolean weakMatch(Scopes other)
    {
        if (isNoneScope()) return false;
        if (isAnyScope()) return true;
        if (other == null || other.isNoneScope()) return true;
        if (other.isAnyScope()) return false;
        return !Collections.disjoint(scopes, other.scopes);
    }

    /**
     * @return the scope strings contained by this <code>Scopes</code> object.
     */
    public String[] asStringArray()
    {
        String[] strings = new String[scopes.size()];
        for (int i = 0; i < scopes.size(); ++i)
        {
            String string = scopes.get(i);
            strings[i] = unescape(string);
        }
        return strings;
    }

    /**
     * @return true if this <code>Scopes</code> object is the {@link #NONE} scope, false otherwise.
     * @see #NONE
     */
    public boolean isNoneScope()
    {
        return equals(NONE);
    }

    /**
     * @return true if this <code>Scopes</code> object is the {@link #ANY} scope, false otherwise.
     * @see #ANY
     */
    public boolean isAnyScope()
    {
        return equals(ANY);
    }

    /**
     * @return true if this <code>Scopes</code> object is the default scope, false otherwise.
     * @see #DEFAULT
     */
    public boolean isDefaultScope()
    {
        return equals(DEFAULT);
    }

    public String toString()
    {
        return Arrays.toString(asStringArray());
    }

    /**
     * Escapes the scope string following RFC 2608, 6.4.1
     *
     * @param value The scope string to escape
     * @return The escaped scope string
     * @see #unescape(String)
     */
    private String escape(String value)
    {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c < reservedChars.length && reservedChars[c] == c)
            {
                result.append(ESCAPE_PREFIX);
                int code = c & 0xFF;
                if (code < 16) result.append("0");
                result.append(Integer.toHexString(code));
            }
            else
            {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Unescapes the scope string following RFC 2608, 6.4.1
     *
     * @param value The scope string to unescape
     * @return The unescaped scope string
     * @see #escape(String)
     */
    private String unescape(String value)
    {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c == ESCAPE_PREFIX)
            {
                String codeString = value.substring(i + 1, i + 3);
                int code = Integer.parseInt(codeString, 16);
                if (code < reservedChars.length && reservedChars[code] == code)
                {
                    result.append(reservedChars[code]);
                    i += 2;
                }
                else
                {
                    throw new ServiceLocationException("Unknown escaped character " + ESCAPE_PREFIX + codeString + " at position " + (i + 1) + " of " + value, ServiceLocationException.PARSE_ERROR);
                }
            }
            else
            {
                result.append(c);
            }
        }
        return result.toString();
    }
}
