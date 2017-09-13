/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.listener;

import java.util.concurrent.atomic.AtomicInteger;

import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;

/**
 *
 * @author Peter Kristensen
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
