/*
 * Copyright 2005 the original author or authors
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
 * @version $Rev$ $Date$
 */
public class ServiceLocationException extends Exception
{
    public static final int LANGUAGE_NOT_SUPPORTED = 1;
    public static final int PARSE_ERROR = 2;
    public static final int INVALID_REGISTRATION = 3;
    public static final int SCOPE_NOT_SUPPORTED = 4;
    public static final int AUTHENTICATION_ABSENT = 6;
    public static final int AUTHENTICATION_FAILED = 7;
    public static final int INVALID_UPDATE = 13;
    public static final int REFRESH_REJECTED = 15;
    public static final int NOT_IMPLEMENTED = 16;
    public static final int NETWORK_INIT_FAILED = 17;
    public static final int NETWORK_TIMED_OUT = 18;
    public static final int NETWORK_ERROR = 19;
    public static final int INTERNAL_SYSTEM_ERROR = 20;
    public static final int TYPE_ERROR = 21;
    public static final int BUFFER_OVERFLOW = 22;

    private final int errorCode;

    public ServiceLocationException(int errorCode)
    {
        this(null, null, errorCode);
    }

    public ServiceLocationException(String message, int errorCode)
    {
        this(message, null, errorCode);
    }

    public ServiceLocationException(Throwable cause, int errorCode)
    {
        this(null, cause, errorCode);
    }

    public ServiceLocationException(String message, Throwable cause, int errorCode)
    {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode()
    {
        return errorCode;
    }
}
