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
package org.livetribe.slp.spi.msg;

import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class BytesBlockTest
{
    @Test
    public void testEscaping() throws Exception
    {
        String original = "A(B)C,D\\E!F<G=H>I~J;K*L+M\rN\nO\tP Q\u20AC";
        String escaped = BytesBlock.escape(original);
        assert "A\\28B\\29C\\2cD\\5cE\\21F\\3cG\\3dH\\3eI\\7eJ\\3bK\\2aL\\2bM\\0dN\\0aO\\09P Q\u20AC".equals(escaped);
    }

    @Test
    public void testEscapeUnescape() throws Exception
    {
        String original = "A(B)C,D\\E!F<G=H>I~J;K*L+M\rN\nO\tP Q\u20AC";
        String escaped = BytesBlock.escape(original);
        String unescaped = BytesBlock.unescape(escaped);
        assert original.equals(unescaped);
    }
}
