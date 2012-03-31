/*
 * Copyright 2006-2011 the original author or authors
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceLocationException;


/**
 *
 */
public class ExpressionFilter implements Filter
{
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("(.+?)([<>]?=)(.+)");
    private static final Pattern OCTET_STRING_PATTERN = Pattern.compile("(\\\\[\\da-f]{2})+");

    static final String GE = ">=";
    static final String LE = "<=";
    static final String EQ = "=";
    private static final String ANY = "*";

    private final String lhs;
    private final String operator;
    private final String rhs;

    public ExpressionFilter(String lhs, String operator, String rhs) throws ServiceLocationException
    {
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;

        // Check if wildcard comparison is done properly (RFC 2608, 8.1)
        if (!EQ.equals(operator) && rhs.indexOf(ANY) >= 0)
            throw new ServiceLocationException("Invalid filter " + this + ": wildcard matching is only allowed with operator " + EQ, SLPError.PARSE_ERROR);
    }

    public static ExpressionFilter fromString(String expr) throws ServiceLocationException
    {
        if (expr == null)
        {
            throw new IllegalArgumentException("expr is null");
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(expr);
        if (matcher.matches())
        {
            return new ExpressionFilter(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3)
            );
        }

        throw new ServiceLocationException("Bad filter expression: " + expr, SLPError.PARSE_ERROR);
    }

    public boolean matches(Attributes attributes)
    {
        if (attributes == null) return false;

        Attributes.Value value = attributes.valueFor(lhs);
        if (value == null) return false;

        // Check for presence only
        if (value.isPresenceType() && EQ.equals(operator) && ANY.equals(rhs)) return true;

        return compare(value, operator, rhs);
    }

    private boolean compare(Attributes.Value attributeValue, String operator, String compare)
    {
        if (attributeValue.isStringType())
        {
            if (GE.equals(operator))
            {
                return compareStringValue((String)attributeValue.getValue(), compare) >= 0;
            }
            else if (LE.equals(operator))
            {
                return compareStringValue((String)attributeValue.getValue(), compare) <= 0;
            }
            else if (EQ.equals(operator))
            {
                boolean result = false;
                if (compare.indexOf(ANY) >= 0)
                {
                    // Wildcard comparison
                    String[] parts = compare.split("\\*", 0);
                    Object[] values = attributeValue.getValues();
                    for (Object value : values)
                    {
                        String stringValue = normalizeSpace((String)value).toLowerCase();
                        boolean match = true;
                        int start = 0;
                        for (String part : parts)
                        {
                            part = normalizeFilterValue(part).toLowerCase();
                            if (part.length() > 0)
                            {
                                int index = stringValue.indexOf(part, start);
                                match &= index >= 0;
                                start = index + 1;
                            }
                        }
                        result |= match;
                    }
                }
                else
                {
                    // Direct comparison
                    Object[] values = attributeValue.getValues();
                    for (Object value : values)
                    {
                        result |= compareStringValue((String)value, compare) == 0;
                    }
                }
                return result;
            }
            else
            {
                throw new AssertionError("Invalid operator " + operator);
            }
        }
        else if (attributeValue.isIntegerType())
        {
            try
            {
                int compareInteger = Integer.parseInt(unescape(compare.trim()));
                if (GE.equals(operator))
                {
                    Object[] values = attributeValue.getValues();
                    boolean result = false;
                    for (Object value : values)
                    {
                        Integer integerValue = (Integer)value;
                        result |= integerValue >= compareInteger;
                    }
                    return result;
                }
                else if (LE.equals(operator))
                {
                    Object[] values = attributeValue.getValues();
                    boolean result = false;
                    for (Object value : values)
                    {
                        Integer integerValue = (Integer)value;
                        result |= integerValue <= compareInteger;
                    }
                    return result;
                }
                else if (EQ.equals(operator))
                {
                    Object[] values = attributeValue.getValues();
                    boolean result = false;
                    for (Object value : values)
                    {
                        Integer integerValue = (Integer)value;
                        result |= integerValue == compareInteger;
                    }
                    return result;
                }
                else
                {
                    throw new AssertionError("Invalid operator " + operator);
                }
            }
            catch (NumberFormatException x)
            {
                return false;
            }
        }
        else if (attributeValue.isBooleanType())
        {
            if (!EQ.equals(operator)) return false;
            if (!"true".equalsIgnoreCase(unescape(compare)) && !"false".equalsIgnoreCase(unescape(compare))) return false;
            Boolean value = (Boolean)attributeValue.getValue();
            return value.equals(Boolean.valueOf(unescape(compare)));
        }
        else if (attributeValue.isOpaqueType())
        {
            if (!EQ.equals(operator)) return false;
            byte[] value = (byte[])attributeValue.getValue();

            return Arrays.equals(octetStringToBytes(compare), value);
        }
        else
        {
            return false;
        }
    }

    protected static int compareStringValue(String attributeValue, String filterValue)
    {
        return normalizeSpace(attributeValue).compareToIgnoreCase(
                normalizeFilterValue(filterValue)
        );
    }

    protected static String normalizeFilterValue(String value)
    {
        return normalizeSpace(unescape(value));
    }

    protected static String normalizeSpace(String s)
    {
        return s.trim().replaceAll("\\s+", "");
    }

    protected static String unescape(String escaped)
    {
        Matcher matcher = OCTET_STRING_PATTERN.matcher(escaped);

        StringBuffer unescaped = new StringBuffer();

        while (matcher.find())
        {
            matcher.appendReplacement(
                    unescaped, Matcher.quoteReplacement(octetString2UTF8(matcher.group(0)))
            );
        }
        matcher.appendTail(unescaped);

        return unescaped.toString().trim().replaceAll("\\s+", " ");
    }

    private static String octetString2UTF8(String ostr) throws ServiceLocationException
    {
        try
        {
            return new String(octetStringToBytes(ostr), "UTF-8");
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new ServiceLocationException("Error while decoding octet string: " + ostr, uee, SLPError.PARSE_ERROR);
        }
    }

    private static byte[] octetStringToBytes(String ostr) throws ServiceLocationException
    {
        if (ostr.length() == 0) return new byte[0];

        if (ostr.length() % 3 != 0)
        {
            throw new ServiceLocationException("Illegal escape sequence, number of characters is not multiple of 3",
                                               SLPError.PARSE_ERROR);
        }

        int len = ostr.length() / 3;

        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++)
        {
            if (ostr.charAt(i * 3) != '\\')
            {
                throw new ServiceLocationException("Escape sequence does not start with \\ at " + (3 * i) + ": " + ostr,
                                                   SLPError.PARSE_ERROR);
            }

            try
            {
                bytes[i] = (byte)(
                        Integer.parseInt(
                                ostr.substring(3 * i + 1, 3 * i + 3), 16
                        ) & 0xFF
                );
            }
            catch (NumberFormatException nfe)
            {
                throw new ServiceLocationException("Illegal escape sequence at " + (3 * i + 1) + ": " + ostr,
                                                   SLPError.PARSE_ERROR);
            }
        }

        return bytes;
    }

    public String asString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(lhs).append(operator).append(rhs).append(")");
        return builder.toString();
    }

}
