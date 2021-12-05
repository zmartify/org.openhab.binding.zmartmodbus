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
 * Exception for RFXCOM errors.
 *
 * @author Pauli Anttila - Initial contribution
 */
@NonNullByDefault
public class ModbusMessageNotImplementedException extends ModbusException {

    private static final long serialVersionUID = 5958462009164173495L;

    public ModbusMessageNotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModbusMessageNotImplementedException(String message) {
        super(message);
    }
}
