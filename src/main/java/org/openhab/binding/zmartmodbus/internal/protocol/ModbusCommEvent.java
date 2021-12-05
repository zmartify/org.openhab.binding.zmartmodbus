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
package org.openhab.binding.zmartmodbus.internal.protocol;

/**
 * The ModbusCommEvent class contains the values returned by ModbusFunction functions 11(0x0B)
 * and 12(0x0C).
 * <ul>
 * <li>status : two-bytes status word, 0xFFFF if a busy condition exists, 0 otherwise
 * <li>eventCount : event counter incremented for each successful message completion
 * <li>messageCount : quantity of messages processed since last restart
 * <li>events[] : 0 to 64 bytes, each byte corresponding to the status of one ModbusFunction
 * </ul>
 * send or receive operation, byte 0 is the most recent event.
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
public class ModbusCommEvent {

    private int status;
    private int eventCount;
    private int messageCount;
    private int[] events;

    public ModbusCommEvent() {
        this.status = 0;
        this.eventCount = 0;
        this.messageCount = 0;
        this.events = null;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getEventCount() {
        return this.eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public int getMessageCount() {
        return this.messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int[] getEvents() {
        return this.events;
    }

    public void setEvents(int[] events) {
        this.events = events;
    }
}
