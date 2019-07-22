/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.exceptions;

/**
 *
 * @author Peter Kristensen
 *
 *         ProtocolErrorCode holds the enumeration of valid error codes for the exception message. For each defined enum
 *         value, a corresponding message should be defined in the properties bundle named:
 *         ProtocolExceptionMessagesBundle.properties.
 *
 *
 */
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
