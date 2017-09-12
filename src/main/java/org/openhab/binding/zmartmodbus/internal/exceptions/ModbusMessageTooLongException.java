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
 * Exception for when RFXCOM messages are too long for the spec.
 *
 * @author James Hewitt-Thomas - Initial contribution
 */
public class ModbusMessageTooLongException extends ModbusException {

    private static final long serialVersionUID = -3352067410289719335L;

    public ModbusMessageTooLongException() {
        super();
    }

    public ModbusMessageTooLongException(String message) {
        super(message);
    }

    public ModbusMessageTooLongException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModbusMessageTooLongException(Throwable cause) {
        super(cause);
    }

}
