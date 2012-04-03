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
package org.livetribe.slp.spi;

/**
 * Lifecycle interface for server objects that can be started and stopped.
 */
public interface Server
{
    /**
     * Starts this server.
     *
     * @return true if the server started, false if the server is already started or if it starting already
     * @see #isRunning()
     * @see #stop()
     */
    public boolean start();

    /**
     * @return whether this server has been successfully started and it is not stopped
     * @see #start()
     */
    public boolean isRunning();

    /**
     * Stops this server.
     *
     * @return true if the server stopped, false if the server is not running or if it is stopping already
     */
    public boolean stop();
}
