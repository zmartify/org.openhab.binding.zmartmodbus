/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.protocol;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modbus global counters
 *
 * @author Peter Kristensen
 *
 */
public class ModbusCounters {

    private static AtomicInteger MessageCounter = new AtomicInteger(0);
    private static AtomicInteger TimeOutCounter = new AtomicInteger(0);

    /**
     *
     */
    private ModbusCounters() {
    }

    public static void clearCounters() {
        MessageCounter = new AtomicInteger(0);
        TimeOutCounter = new AtomicInteger(0);
    }

    public static int incrementMessageCounter() {
        return MessageCounter.incrementAndGet();
    }

    public static int incrementTimeOutCounter() {
        return TimeOutCounter.incrementAndGet();
    }

    public static int getMessageCounter() {
        return MessageCounter.get();
    }

    public static int getTimeOutCounter() {
        return TimeOutCounter.get();
    }
}
