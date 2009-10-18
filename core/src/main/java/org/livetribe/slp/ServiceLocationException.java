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
package org.livetribe.slp;

/**
 * Thrown when something goes wrong in Service Location Protocol operations.
 * Details about what happened are provided by an {@link #getSLPError() error}.
 * This exception is normally unrecoverable, so it extends {@link RuntimeException}.
 *
 * @version $Rev$ $Date$
 */
public class ServiceLocationException extends RuntimeException
{
    private final SLPError error;

    public ServiceLocationException(SLPError error)
    {
        this(null, null, error);
    }

    public ServiceLocationException(String message, SLPError error)
    {
        this(message, null, error);
    }

    public ServiceLocationException(Throwable cause, SLPError error)
    {
        this(null, cause, error);
    }

    public ServiceLocationException(String message, Throwable cause, SLPError error)
    {
        super(message, cause);
        this.error = error;
    }

    /**
     * @return the error that specifies the problem that caused this exception.
     */
    public SLPError getSLPError()
    {
        return error;
    }

    public String toString()
    {
        return super.toString() + ", " + getSLPError();
    }
}
