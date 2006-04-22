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

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Rev$ $Date$
 */
public class ExpressionFilter implements Filter
{
    static final String GE = ">=";
    static final String LE = "<=";
    static final String EQ = "=";
    private static final String ANY = "*";

    private final String lhs;
    private final String operator;
    private final String rhs;

    public ExpressionFilter(String lhs, String operator, String rhs)
    {
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    public boolean match(Attributes attributes) throws ServiceLocationException
    {
        Attributes.Entry entry = attributes.getEntry(lhs);
        if (entry == null) return false;

        // Check for presence only
        if (EQ.equals(operator) && ANY.equals(rhs)) return true;

        // Check if wildcard comparison is done properly (RFC 2608, 8.1)
        if (!EQ.equals(operator) && rhs.indexOf(ANY) >= 0) throw new ServiceLocationException("Invalid filter " + this + ": wildcard matching is only allowed with operator " + EQ, ServiceLocationException.PARSE_ERROR);

        return compare(entry, operator, rhs);
    }

    private boolean compare(Attributes.Entry entry, String operator, String compare) throws ServiceLocationException
    {
        if (entry.isStringType())
        {
            if (GE.equals(operator))
            {
                String value = (String)entry.getValue();
                return value.compareToIgnoreCase(compare) >= 0;
            }
            else if (LE.equals(operator))
            {
                String value = (String)entry.getValue();
                return value.compareToIgnoreCase(compare) <= 0;
            }
            else if (EQ.equals(operator))
            {
                boolean result = false;
                if (compare.indexOf(ANY) >= 0)
                {
                    // Wildcard comparison
                    String[] parts = compare.split("\\*", 0);
                    Object[] values = entry.getValues();
                    for (int i = 0; i < values.length; ++i)
                    {
                        String value = ((String)values[i]).toLowerCase();
                        boolean match = true;
                        int start = 0;
                        for (int j = 0; j < parts.length; ++j)
                        {
                            String part = parts[j].trim().toLowerCase();
                            if (part.length() > 0)
                            {
                                int index = value.indexOf(part, start);
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
                    Object[] values = entry.getValues();
                    for (int i = 0; i < values.length; ++i)
                    {
                        String value = (String)values[i];
                        result |= value.compareToIgnoreCase(compare) == 0;
                    }
                }
                return result;
            }
            else
            {
                throw new AssertionError("Invalid operator " + operator);
            }
        }
        else if (entry.isLongType())
        {
            try
            {
                long compareLong = Long.parseLong(compare);
                if (GE.equals(operator))
                {
                    Object[] values = entry.getValues();
                    boolean result = false;
                    for (int i = 0; i < values.length; ++i)
                    {
                        Long value = (Long)values[i];
                        result |= value.longValue() >= compareLong;
                    }
                    return result;
                }
                else if (LE.equals(operator))
                {
                    Object[] values = entry.getValues();
                    boolean result = false;
                    for (int i = 0; i < values.length; ++i)
                    {
                        Long value = (Long)values[i];
                        result |= value.longValue() <= compareLong;
                    }
                    return result;
                }
                else if (EQ.equals(operator))
                {
                    Object[] values = entry.getValues();
                    boolean result = false;
                    for (int i = 0; i < values.length; ++i)
                    {
                        Long value = (Long)values[i];
                        result |= value.longValue() == compareLong;
                    }
                    return result;
                }
                else
                {
                    throw new AssertionError("Invalid operator " + operator);
                }
            }
            catch (NumberFormatException e)
            {
                throw new ServiceLocationException("Invalid natural number value " + compare, ServiceLocationException.PARSE_ERROR);
            }
        }
        else if (entry.isBooleanType())
        {
            if (!EQ.equals(operator)) throw new ServiceLocationException("Invalid operator " + operator + ": for boolean values is only allowed operator " + EQ, ServiceLocationException.PARSE_ERROR);
            if (!"true".equalsIgnoreCase(compare) && !"false".equalsIgnoreCase(compare)) throw new ServiceLocationException("Invalid boolean value " + compare, ServiceLocationException.PARSE_ERROR);
            Boolean value = (Boolean)entry.getValue();
            return value.equals(Boolean.valueOf(compare));
        }
        else if (entry.isOpaqueType())
        {
            if (!EQ.equals(operator)) throw new ServiceLocationException("Invalid operator " + operator + ": for opaque values is only allowed operator " + EQ, ServiceLocationException.PARSE_ERROR);
            String value = (String)entry.getValue();
            return compare.equals(value);
        }
        else
        {
            throw new AssertionError("Invalid entry type " + entry);
        }
    }
}
