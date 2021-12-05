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
package org.openhab.binding.zmartmodbus.internal.listener;

import java.util.concurrent.atomic.AtomicInteger;

import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 */

public abstract class StateListener {
    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private final int ID;

    public StateListener() {
        ID = COUNTER.getAndIncrement();
    }

    public abstract void modbusState(ModbusState event);

    public abstract void error(Throwable throwable);

    @Override
    public String toString() {
        return String.format("Listener ID:%d:%s", ID, super.toString());
    }
}
