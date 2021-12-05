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
 * Exceptions thrown from the serial interface.
 *
 * @author Chris Jackson - Initial contribution
 * @author Jan-Willem Spuij - Added functionality
 * 
 */
@NonNullByDefault
public class ModbusInterfaceException extends Exception {

    private static final long serialVersionUID = 8852643957484264124L;

    /**
     * Constructor. Creates a new instance of ModbusInterfaceException.
     */
    public ModbusInterfaceException() {
    }

    /**
     * Constructor. Creates a new instance of ModbusInterfaceException.
     *
     * @param message the detail message.
     */
    public ModbusInterfaceException(String message) {
        super(message);
    }

    /**
     * Constructor. Creates a new instance of ModbusInterfaceException.
     *
     * @param cause the cause. (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ModbusInterfaceException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor. Creates a new instance of ModbusInterfaceException.
     *
     * @param message the detail message.
     * @param cause the cause. (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ModbusInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
