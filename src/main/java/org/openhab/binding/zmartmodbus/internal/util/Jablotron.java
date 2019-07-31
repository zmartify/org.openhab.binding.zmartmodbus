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
package org.openhab.binding.zmartmodbus.internal.util;

/**
 * @author Peter Kristensen - Initial contribution
 *
 *         Used to calculate the CRC-16 (cyclical redundancy check) for an array of bytes.
 */

public class Jablotron {

    private Jablotron() {
    };

    /*
     * Special address conversion for Jablotron
     *
     */
    public static byte getPage(int address) {
        return (byte) ((address & 0xE000) >>> 13);
    }

    public static byte getCategory(int address) {
        return (byte) ((address & 0x1F80) >>> 7);
    }

    public static byte getIndex(int address) {
        return (byte) (address & 0x007F);
    }

    public static int getAddress(int category, int index, int page) {
        return ((page << 13) | (category << 7) | index);
    }

    public static enum ChannelTimerEvent {
        NO_EVENT,
        IDLE_TIMER,
        CUT_OFF,
        CONTROL_BYPASS,
        START_DELAY_TIMER,
        OUTPUT_OVERCURRENT,
        OVERRIDE_OUTPUT_OFF,
        OUTPUT_ON,
        FREEZING,
        DHW_OUTPUT_ON,
        STOP_DELAY_TIMER;
    }

}
