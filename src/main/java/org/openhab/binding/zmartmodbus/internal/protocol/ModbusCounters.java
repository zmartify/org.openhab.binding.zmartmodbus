/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.zmartmodbus.internal.protocol;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modbus global counters
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
public class ModbusCounters {

    private AtomicInteger MessageCounter = new AtomicInteger(0);
    private AtomicInteger TimeOutCounter = new AtomicInteger(0);
    private AtomicInteger FailedCounter = new AtomicInteger(0);

    /**
     *
     */
    public ModbusCounters() {
    }

    public void clearCounters() {
        MessageCounter = new AtomicInteger(0);
        TimeOutCounter = new AtomicInteger(0);
        FailedCounter = new AtomicInteger(0);
    }

    public int incrementMessageCounter() {
        return MessageCounter.incrementAndGet();
    }

    public int incrementTimeOutCounter() {
        return TimeOutCounter.incrementAndGet();
    }

    public int incrementFailedCounter() {
        return FailedCounter.incrementAndGet();
    }

    public int getMessageCounter() {
        return MessageCounter.get();
    }

    public int getTimeOutCounter() {
        return TimeOutCounter.get();
    }

    public int getFailedCounter() {
        return FailedCounter.get();
    }
}
