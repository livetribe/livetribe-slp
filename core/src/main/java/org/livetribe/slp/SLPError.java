/*
 * Copyright 2008-2008 the original author or authors
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

import java.util.HashMap;
import java.util.Map;

/**
 *
 */

/**
 * Enumeration of possible error codes of the Service Location Protocol.
 */
public enum SLPError
{
    /**
     * Indicates absence of errors.
     */
    NO_ERROR(0),

    /**
     * Error indicating that DirectoryAgents or ServiceAgents have information
     * in other languages other than the one requested.
     */
    LANGUAGE_NOT_SUPPORTED(1),

    /**
     * Error indicating that a parse error has occurred while parsing attributes string,
     * filter expressions or SLP messages.
     */
    PARSE_ERROR(2),

    /**
     * Error indicating that a service registration failed because of invalid arguments.
     */
    INVALID_REGISTRATION(3),

    /**
     * Error indicating that the DirectoryAgent or ServiceAgent does not support the scope of the requested operation.
     */
    SCOPE_NOT_SUPPORTED(4),

    /**
     * Error indicating that an authentication has been requested, but the DirectoryAgent or ServiceAgent does not support it.
     */
    AUTHENTICATION_UNKNOWN(5),

    /**
     * Error indicating that an authentication was expected, but not received.
     */
    AUTHENTICATION_ABSENT(6),

    /**
     * Error indicating that the authentication failed.
     */
    AUTHENTICATION_FAILED(7),

    /**
     * Error indicating that the SLP version is not supported.
     */
    VERSION_NOT_SUPPORTED(9),

    /**
     * Error indicating that the ServiceAgent or DirectoryAgent cannot respond.
     */
    INTERNAL_ERROR(10),

    /**
     * Error indicating that the server is busy and that the client should retry.
     */
    BUSY_NOW(11),

    /**
     * Error indicating that a mandatory extension is not understood by the implementation.
     */
    OPTION_NOT_UNDERSTOOD(12),

    /**
     * Error indicating that a service update failed because of invalid arguments.
     */
    INVALID_UPDATE(13),

    /**
     * Error indicating that the SLP implementation does not support a message.
     */
    MESSAGE_NOT_SUPPORTED(14),

    /**
     * Error indicating that a ServiceAgent sent re-registrations to a DirectoryAgent more frequently than allowed.
     */
    REFRESH_REJECTED(15),

    /**
     * Error indicating that the API exists, but it's not implemented.
     */
    NOT_IMPLEMENTED(16),

    /**
     * Error indicating that the network initialization failed.
     */
    NETWORK_INIT_FAILED(17),

    /**
     * Error indicating that a unicast request timed out.
     */
    NETWORK_TIMED_OUT(18),

    /**
     * Error indicating that a generic network failure happened.
     */
    NETWORK_ERROR(19),

    /**
     * Error indicating that an internal, non-recoverable, error happened.
     */
    INTERNAL_SYSTEM_ERROR(20),

    /**
     * Error indicating that attributes specified during registration do no match those present in the service template.
     */
    TYPE_ERROR(21),

    /**
     * Error indicating that an outgoing request exceeded the max transmission unit.
     */
    BUFFER_OVERFLOW(22);

    /**
     * Factory method that returns the Error enum constant with the given code.
     *
     * @param code the code of the Error
     * @return the Error enum constant with the given code
     */
    public static SLPError from(int code)
    {
        return Errors.errors.get(code);
    }

    private final int code;

    private SLPError(int code)
    {
        this.code = code;
        Errors.errors.put(code, this);
    }

    /**
     * @return the code of this Error
     */
    public int getCode()
    {
        return code;
    }

    /**
     * Helper class that works around a limitation of enums, that cannot access static members
     * because enum constants are initialized before static members.
     * See http://java.sun.com/docs/books/jls/third_edition/html/classes.html#301020
     */
    private static class Errors
    {
        private static final Map<Integer, SLPError> errors = new HashMap<Integer, SLPError>();
    }
}
