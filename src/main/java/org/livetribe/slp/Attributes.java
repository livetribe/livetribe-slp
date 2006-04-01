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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * SLP attributes are defined in RFC 2608, 5.0.
 * This class encapsulates operations and parsing of SLP attributes.
 *
 * @version $Rev$ $Date$
 */
public class Attributes
{
    private static final char[] reservedChars = new char[128];

    static
    {
        reservedChars['!'] = '!';
        reservedChars['('] = '(';
        reservedChars[')'] = ')';
        reservedChars[','] = ',';
        reservedChars['<'] = '<';
        reservedChars['='] = '=';
        reservedChars['>'] = '>';
        reservedChars['\\'] = '\\';
        reservedChars['~'] = '~';
    }

    private final Map attributes = new HashMap();

    public Attributes()
    {
    }

    public Attributes(String attributeList) throws ServiceLocationException
    {
        parse(attributeList, attributes);
    }

    public void put(String tag)
    {
        attributes.put(tag, tag);
    }

    public void put(String tag, String value)
    {
        attributes.put(tag, value);
    }

    public void put(String tag, String[] values)
    {
        String[] copies = new String[values.length];
        System.arraycopy(values, 0, copies, 0, values.length);
        attributes.put(tag, copies);
    }

    public String getValue(String tag)
    {
        Object result = attributes.get(tag);
        if (result instanceof String[]) return ((String[])result)[0];
        return (String)result;
    }

    public String[] getValues(String tag)
    {
        Object result = attributes.get(tag);
        if (result instanceof String[]) return (String[])result;
        return new String[]{getValue(tag)};
    }

    private void parse(String attributeList, Map result) throws ServiceLocationException
    {
        if (attributeList == null) return;

        StringBuffer nonPairs = new StringBuffer();
        int start = 0;
        while (start < attributeList.length())
        {
            int open = attributeList.indexOf('(', start);
            if (open < 0)
            {
                String remaining = attributeList.substring(start);
                nonPairs.append(remaining);
                start += remaining.length();
            }
            else
            {
                int close = attributeList.indexOf(')', open);
                if (close < 0)
                    throw new ServiceLocationException("Missing ')' in attribute list " + attributeList, ServiceLocationException.PARSE_ERROR);
                nonPairs.append(attributeList.substring(start, open));
                String pair = attributeList.substring(open, close + 1);
                parseAttribute(pair, result, attributeList);
                start = close + 1;
            }
        }

        // Only tags, no pairs
        String[] attributes = nonPairs.toString().split(",", 0);
        for (int i = 0; i < attributes.length; ++i)
        {
            String attribute = attributes[i].trim();
            if (attribute.length() > 0) parseAttribute(attribute, result, attributeList);
        }
    }

    private void parseAttribute(String attribute, Map attributes, String attributeList) throws ServiceLocationException
    {
        if (attribute == null) return;
        attribute = attribute.trim();
        if (attribute.length() == 0) return;

        if (attribute.startsWith("("))
        {
            int closeParenthesis = attribute.indexOf(')');
            String pair = attribute.substring(1, closeParenthesis);

            int equals = pair.indexOf('=');
            if (equals < 0)
                throw new ServiceLocationException("Could not parse attributes " + attributeList + ", missing '=' in " + attribute, ServiceLocationException.PARSE_ERROR);

            String escapedTag = pair.substring(0, equals);
            String unescapedTag = checkAndUnescapeTag(escapedTag);

            String value = pair.substring(equals + 1).trim();
            if (value.indexOf(',') >= 0)
            {
                // It's a list of values
                String[] values = value.split(",", -1);
                if (values.length > 0)
                {
                    for (int i = 0; i < values.length; ++i) values[i] = checkAndUnescapeValue(values[i]);
                    attributes.put(unescapedTag, values);
                }
            }
            else
            {
                value = checkAndUnescapeValue(value);
                attributes.put(unescapedTag, value);
            }
        }
        else
        {
            attribute = checkAndUnescapeTag(attribute);
            attributes.put(attribute, attribute);
        }
    }

    private String checkAndUnescapeTag(String escapedTag) throws ServiceLocationException
    {
        // Check that the escaped tag does not contain reserved characters
        checkEscaped(escapedTag);

        // Check if the tag is unescapable; wrong escaping will be detected here
        String unescaped = unescape(escapedTag).trim();
        if (unescaped.length() == 0)
            throw new ServiceLocationException("Attribute tag could not be the empty string", ServiceLocationException.PARSE_ERROR);

        // Check disallowed characters in unescaped tag
        if (unescaped.indexOf('\t') >= 0 ||
                unescaped.indexOf('\r') >= 0 ||
                unescaped.indexOf('\n') >= 0 ||
                unescaped.indexOf('*') >= 0 ||
                unescaped.indexOf('_') >= 0)
            throw new ServiceLocationException("Illegal character in attribute tag " + unescaped, ServiceLocationException.PARSE_ERROR);

        return unescaped;
    }

    private String checkAndUnescapeValue(String escaped) throws ServiceLocationException
    {
        escaped = escaped.trim();

        // Special case: the value is opaque
        if (escaped.startsWith("\\FF")) return escaped;

        // Check that the escaped value does not contain reserved characters
        checkEscaped(escaped);

        // Check if the value is unescapable; wrong escaping will be detected here
        return unescape(escaped).trim();
    }

    /**
     * Checks that the given escaped string does not contain reserved characters.
     * For example, the string <code>foo,bar</code> will throw a ServiceLocationException (the comma is a reserved character),
     * while the string <code>foo\2cbar<code> will pass the check.
     *
     * @param escaped The string to check, it's assumed that's escaped.
     * @throws ServiceLocationException If a reserved character is contained in the string
     */
    private void checkEscaped(String escaped) throws ServiceLocationException
    {
        for (int i = 0; i < escaped.length(); ++i)
        {
            char ch = escaped.charAt(i);
            // The backslash is a reserved character, but is present in escaped strings, skip it
            if (ch != '\\' && ch < reservedChars.length && reservedChars[ch] == ch)
                throw new ServiceLocationException("Illegal character '" + ch + "' in " + escaped, ServiceLocationException.PARSE_ERROR);
        }
    }

    private String escape(String value)
    {
        if (value == null) return null;

        // Do not escape already escaped sequence of bytes
        if (value.startsWith("\\FF")) return value;

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c < reservedChars.length && reservedChars[c] == c)
            {
                result.append("\\");
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

    private String unescape(String value) throws ServiceLocationException
    {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < value.length(); ++i)
        {
            char c = value.charAt(i);
            if (c == '\\')
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
                    throw new ServiceLocationException("Unknown escaped character \\" + codeString + " at position " + (i + 1) + " of " + value, ServiceLocationException.PARSE_ERROR);
                }
            }
            else
            {
                result.append(c);
            }
        }
        return result.toString();
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final Attributes that = (Attributes)obj;
        // Could not compare the attributes Map, since it contains String[] that will make the comparison fail.
        return asString().equals(that.asString());
    }

    public int hashCode()
    {
        return attributes.hashCode();
    }

    public String asString()
    {
        TreeMap orderedAttributes = new TreeMap(attributes);

        StringBuffer result = new StringBuffer();
        for (Iterator entries = orderedAttributes.entrySet().iterator(); entries.hasNext();)
        {
            Map.Entry entry = (Map.Entry)entries.next();
            String tag = (String)entry.getKey();
            Object value = entry.getValue();
            if (tag == value)
            {
                result.append(escape(tag));
            }
            else
            {
                result.append("(").append(escape(tag)).append("=");
                if (value instanceof String[])
                {
                    String[] values = (String[])value;
                    for (int i = 0; i < values.length; ++i)
                    {
                        if (i > 0) result.append(",");
                        result.append(escape(values[i]));
                    }
                }
                else
                {
                    result.append(escape((String)value));
                }
                result.append(")");
            }
            if (entries.hasNext()) result.append(",");
        }
        return result.toString();
    }

    public String toString()
    {
        return asString();
    }

    public boolean isEmpty()
    {
        return attributes.isEmpty();
    }
}
