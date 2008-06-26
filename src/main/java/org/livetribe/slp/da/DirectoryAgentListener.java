/*
 * Copyright 2007-2008 the original author or authors
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

import java.util.EventListener;

/**
 * The listener that receives DirectoryAgent events.
 *
 * @version $Revision$ $Date$
 * @see DirectoryAgentEvent
 */
public interface DirectoryAgentListener extends EventListener
{
    /**
     * Invoked when a DirectoryAgent is born.
     *
     * @param event the DirectoryAgent birth event
     */
    public void directoryAgentBorn(DirectoryAgentEvent event);

    /**
     * Invoked when a DirectoryAgent dies.
     *
     * @param event the DirectoryAgent death event
     */
    public void directoryAgentDied(DirectoryAgentEvent event);
}
