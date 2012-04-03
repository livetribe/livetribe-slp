/*
 * Copyright 2005-2008 the original author or authors
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
package org.livetribe.slp.da;

import java.util.EventObject;

import org.livetribe.slp.sa.ServiceAgent;
import org.livetribe.slp.ua.UserAgent;


/**
 * The event that indicates that a DirectoryAgent has either born or died.
 * <br />
 * {@link ServiceAgent}s and {@link UserAgent}s listen for directory agent advertisements and forward these events
 * to interested listeners.
 *
 * @see DirectoryAgentListener
 */
public class DirectoryAgentEvent extends EventObject
{
    private final DirectoryAgentInfo directoryAgent;

    /**
     * Creates a new DirectoryAgentEvent.
     *
     * @param source         the source of the event
     * @param directoryAgent the DirectoryAgent born or died
     */
    public DirectoryAgentEvent(Object source, DirectoryAgentInfo directoryAgent)
    {
        super(source);
        this.directoryAgent = directoryAgent;
    }

    /**
     * @return the DirectoryAgent born or died
     */
    public DirectoryAgentInfo getDirectoryAgent()
    {
        return directoryAgent;
    }
}
