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
package org.livetribe.slp.spi;

import java.io.IOException;

import org.livetribe.slp.spi.net.MessageListener;

/**
 * @version $Rev$ $Date$
 */
public interface AgentManager
{
    public void start() throws IOException;

    public boolean isRunning();

    public void stop() throws IOException;

    public void addMessageListener(MessageListener listener, boolean udp);

    public void removeMessageListener(MessageListener listener, boolean udp);
}
