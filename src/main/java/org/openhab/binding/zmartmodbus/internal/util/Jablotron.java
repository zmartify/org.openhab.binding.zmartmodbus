/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.openhab.binding.zmartmodbus.internal.util;

/**
 * Used to calculate the CRC-16 (cyclical redundancy check) for an array of bytes.
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
