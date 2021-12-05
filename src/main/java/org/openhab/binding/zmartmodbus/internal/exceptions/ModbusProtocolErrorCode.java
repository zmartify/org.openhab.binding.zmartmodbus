/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.zmartmodbus.internal.exceptions;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 *         ProtocolErrorCode holds the enumeration of valid error codes for the exception message. For each defined enum
 *         value, a corresponding message should be defined in the properties bundle named:
 *         ProtocolExceptionMessagesBundle.properties.
 *
 *
 */
@NonNullByDefault
public enum ModbusProtocolErrorCode {
    INVALID_CONFIGURATION,
    INVALID_DATA_ADDRESS,
    INVALID_DATA_TYPE,
    INVALID_DATA_LENGTH,
    METHOD_NOT_SUPPORTED,
    NOT_AVAILABLE,
    NOT_CONNECTED,
    SERIAL_INUSE,
    SERIAL_UNSUPPORTED,
    CONNECTION_FAILURE,
    TRANSACTION_FAILURE,
    RESPONSE_TIMEOUT;
}
