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
 * Exception for when RFXCOM messages have a value that we don't understand.
 *
 * @author James Hewitt-Thomas - Initial contribution
 */
public class ModbusUnsupportedValueException extends ModbusException {

    private static final long serialVersionUID = 402781611495845169L;

    public ModbusUnsupportedValueException(Class<?> enumeration, String value) {
        super("Unsupported value '" + value + "' for " + enumeration.getSimpleName());
    }

    public ModbusUnsupportedValueException(Class<?> enumeration, int value) {
        this(enumeration, String.valueOf(value));
    }
}
