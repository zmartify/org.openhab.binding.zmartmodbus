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
 * Exceptions thrown from the serial interface.
 *
 * @author Chris Jackson
 * @author Jan-Willem Spuij
 */
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
