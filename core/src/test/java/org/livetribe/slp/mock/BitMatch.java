/**
 *
 * Copyright 2009 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.mock;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;


/**
 * @version $Revision$ $Date$
 */
public class BitMatch extends BaseMatcher<byte[]>
{
    private final int index;
    private final byte value;

    public BitMatch(int index, byte value)
    {
        this.index = index;
        this.value = value;
    }

    public boolean matches(Object o)
    {
        byte[] test = (byte[]) o;
        return test != null && test.length >= index && (test[index] & value) == value;
    }

    public void describeTo(Description description)
    {
        description.appendText("expecting bits set 0x" + Integer.toHexString(0xff & value) + " at " + index);
    }
}
