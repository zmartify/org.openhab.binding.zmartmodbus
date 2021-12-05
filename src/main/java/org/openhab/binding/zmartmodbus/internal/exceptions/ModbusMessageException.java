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
 */
@NonNullByDefault
public class ModbusMessageException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -2106654578826723533L;

    ModbusMessageException(String reason) {
        super(reason);
    }

    public ModbusMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
