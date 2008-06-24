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
package org.livetribe.slp.srv.filter;

import java.util.ArrayList;
import java.util.List;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceLocationException;

/**
 * A parser that parses a string with LDAPv3 syntax, and returns a {@link Filter} that
 * is used to match {@link Attributes} content.
 * <br />
 * This syntax is described in <a href="http://ietf.org/rfc/rfc2254.txt">RFC 2254</a>.
 * String comparisons are case insensitive.
 * <br />
 * Examples:
 * <br />
 * <ul>
 * <li><code>(a=10)</code> matches an attribute tag named <code>a</code> with value <code>10</code></li>
 * <li><code>(&(a&gt;10)(b=true))</code> matches <code>a&gt;10</code> and <code>b=true</code></li>
 * <li><code>(|(a&lt;=10)(b=true))</code> matches <code>a&lt;=10</code> or <code>b=true</code></li>
 * <li><code>(!(a&gt;=10))</code> matches <code>a&lt;10</code></li>
 * <li><code>(name=L*ve*b*)</code> matches <code>name=LiveTribe</code> but also <code>name=Loveboat</code></li>
 * <li><code>(foo=*)</code> matches the presence of the attribute tag named <code>foo</code></li>
 * @version $Rev:157 $ $Date:2006-06-05 23:29:25 +0200 (Mon, 05 Jun 2006) $
 */
public class FilterParser
{
    public Filter parse(String expression) throws ServiceLocationException
    {
        if (expression == null || expression.length() == 0) return new AlwaysMatchFilter();
        return createFilterFromExpression(expression);
    }

    private Filter createFilterFromExpression(String expression) throws ServiceLocationException
    {
        expression = expression.trim();

        if (expression.startsWith("!"))
        {
            String subExpression = expression.substring(1);
            return new NotFilter(createFilterFromExpression(subExpression));
        }
        else if (expression.startsWith("&"))
        {
            String subExpression = expression.substring(1);
            List<String> operands = parseOperands(subExpression);
            List<Filter> filters = new ArrayList<Filter>();
            for (String operand : operands)
            {
                filters.add(createFilterFromExpression(operand));
            }
            return new AndFilter(filters);
        }
        else if (expression.startsWith("|"))
        {
            String subExpression = expression.substring(1);
            List<String> operands = parseOperands(subExpression);
            List<Filter> filters = new ArrayList<Filter>();
            for (String operand : operands)
            {
                filters.add(createFilterFromExpression(operand));
            }
            return new OrFilter(filters);
        }
        else if (expression.startsWith("("))
        {
            return createFilterFromExpression(removeParenthesis(expression));
        }
        else
        {
            return createFilter(expression);
        }
    }

    private List<String> parseOperands(String expression) throws ServiceLocationException
    {
        expression = expression.trim();

        List<String> operands = new ArrayList<String>();
        int open = -1;
        int parenthesis = 0;
        for (int i = 0; i < expression.length(); ++i)
        {
            char ch = expression.charAt(i);
            switch (ch)
            {
                case '(':
                    if (open < 0)
                    {
                        open = i;
                    }
                    else
                    {
                        ++parenthesis;
                    }
                    break;
                case ')':
                    if (open >= 0)
                    {
                        if (parenthesis == 0)
                        {
                            String operand = expression.substring(open, i + 1);
                            operands.add(operand);
                            open = -1;
                        }
                        else
                        {
                            --parenthesis;
                        }
                    }
                    else
                    {
                        throw new ServiceLocationException("Invalid filter expression " + expression, ServiceLocationException.PARSE_ERROR);
                    }
                    break;
                default:
                    // Do nothing
            }
        }
        if (open >= 0) throw new ServiceLocationException("Invalid filter expression " + expression, ServiceLocationException.PARSE_ERROR);
        return operands;
    }

    private String removeParenthesis(String expression) throws ServiceLocationException
    {
        expression = expression.trim();

        int open = expression.indexOf('(');
        if (open < 0) throw new ServiceLocationException("Invalid filter expression " + expression + ": does not contain '('", ServiceLocationException.PARSE_ERROR);

        int close = expression.lastIndexOf(')');
        if (close < 0) throw new ServiceLocationException("Invalid filter expression " + expression + ": does not contain ')'", ServiceLocationException.PARSE_ERROR);

        return expression.substring(open + 1, close);
    }

    private Filter createFilter(String expression) throws ServiceLocationException
    {
        expression = expression.trim();

        int ge = expression.indexOf(ExpressionFilter.GE);
        if (ge > 0) return new ExpressionFilter(expression.substring(0, ge).trim(), ExpressionFilter.GE, expression.substring(ge + ExpressionFilter.GE.length()).trim());

        int le = expression.indexOf(ExpressionFilter.LE);
        if (le > 0) return new ExpressionFilter(expression.substring(0, le).trim(), ExpressionFilter.LE, expression.substring(le + ExpressionFilter.LE.length()).trim());

        int eq = expression.indexOf(ExpressionFilter.EQ);
        if (eq > 0) return new ExpressionFilter(expression.substring(0, eq).trim(), ExpressionFilter.EQ, expression.substring(eq + ExpressionFilter.EQ.length()).trim());

        throw new ServiceLocationException("Unknown expression " + expression, ServiceLocationException.PARSE_ERROR);
    }
}
