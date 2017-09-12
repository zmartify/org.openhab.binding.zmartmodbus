/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.exceptions;

/**
 * Exception for RFXCOM errors.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class ModbusException extends Exception {

    private static final long serialVersionUID = 2975102966905930260L;

    public ModbusException() {
        super();
    }

    public ModbusException(String message) {
        super(message);
    }

    public ModbusException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModbusException(Throwable cause) {
        super(cause);
    }

}
