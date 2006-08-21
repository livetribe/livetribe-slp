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
 * Thrown when something goes wrong in Service Location Protocol operations.
 * Details about what happened are provided by a {@link #getErrorCode() error code}.
 * @version $Rev$ $Date$
 */
public class ServiceLocationException extends Exception
{
    /**
     * Error code indicating that DirectoryAgents or ServiceAgents have information
     * in other languages other than the one requested.
     */
    public static final int LANGUAGE_NOT_SUPPORTED = 1;

    /**
     * Error code indicating that a parse error has occurred while parsing attributes string,
     * filter expressions or SLP messages.
     */
    public static final int PARSE_ERROR = 2;

    /**
     * Error code indicating that a service registration failed because of invalid arguments.
     */
    public static final int INVALID_REGISTRATION = 3;

    /**
     * Error code indicating that the DirectoryAgent or ServiceAgent does not support the scope of the requested operation.
     */
    public static final int SCOPE_NOT_SUPPORTED = 4;

    /**
     * Error code indicating that an authentication has been requested, but the DirectoryAgent or ServiceAgent does not support it.
     */
    public static final int AUTHENTICATION_UNKNOWN = 5;

    /**
     * Error code indicating that an authentication was expected, but not received.
     */
    public static final int AUTHENTICATION_ABSENT = 6;

    /**
     * Error code indicating that the authentication failed.
     */
    public static final int AUTHENTICATION_FAILED = 7;

    /**
     * Error code indicating that the SLP version is not supported.
     */
    public static final int VERSION_NOT_SUPPORTED = 9;

    /**
     * Error code indicating that the ServiceAgent or DirectoryAgent cannot respond.
     */
    public static final int INTERNAL_ERROR = 10;

    /**
     * Error code indicating that the server is busy and that the client should retry.
     */
    public static final int BUSY_NOW = 11;

    /**
     * Error code indicating that a mandatory extension is not understood by the implementation.
     */
    public static final int OPTION_NOT_UNDERSTOOD = 12;

    /**
     * Error code indicating that a service update failed because of invalid arguments.
     */
    public static final int INVALID_UPDATE = 13;

    /**
     * Error code indicating that the SLP implementation does not support a message.
     */
    public static final int MESSAGE_NOT_SUPPORTED = 14;

    /**
     * Error code indicating that a ServiceAgent sent re-registrations to a DirectoryAgent more frequently than allowed.
     */
    public static final int REFRESH_REJECTED = 15;

    /**
     * Error code indicating that the API exists, but it's not implemented.
     */
    public static final int NOT_IMPLEMENTED = 16;

    /**
     * Error code indicating that the network initialization failed.
     */
    public static final int NETWORK_INIT_FAILED = 17;

    /**
     * Error code indicating that a unicast request timed out.
     */
    public static final int NETWORK_TIMED_OUT = 18;

    /**
     * Error code indicating that a generic network failure happened.
     */
    public static final int NETWORK_ERROR = 19;

    /**
     * Error code indicating that an internal, non-recoverable, error happened.
     */
    public static final int INTERNAL_SYSTEM_ERROR = 20;

    /**
     * Error code indicating that attributes specified during registration do no match those present in the service template.
     */
    public static final int TYPE_ERROR = 21;

    /**
     * Error code indicating that an outgoing request exceeded the max transmission unit.
     */
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

    /**
     * Returns the error code that specifies the problem that caused this exception.
     */
    public int getErrorCode()
    {
        return errorCode;
    }

    public String toString()
    {
        return super.toString() + ", error " + getErrorCode();
    }
}
