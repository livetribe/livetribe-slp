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
package org.livetribe.slp.spi.msg;

import java.util.Set;

/**
 * @version $Rev$ $Date$
 */
public abstract class Rqst extends Message
{
    private Set previousResponders;

    /**
     * A comma separated list of IPv4 addresses of previous responders
     * in case this message has been multicasted.
     */
    public Set getPreviousResponders()
    {
        return previousResponders;
    }

    public void setPreviousResponders(Set previousResponders)
    {
        this.previousResponders = previousResponders;
    }
}
