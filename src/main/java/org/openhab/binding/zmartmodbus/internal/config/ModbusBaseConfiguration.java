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
package org.openhab.binding.zmartmodbus.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ModbusBaseConfiguration} is the base class for configuration
 * information held by the device
 *
 * @author Sami Salonen - Initial contribution
 * @author Peter Kristensen - Modified for ZmartModbus
 *
 */
@NonNullByDefault
public class ModbusBaseConfiguration {
    private int slowPoll = 40000;
    private int fastPoll = 4000;

    public int getSlowPoll() {
        return slowPoll;
    }

    public void setSlowPoll(int slowPoll) {
        this.slowPoll = slowPoll;
    }

    public int getFastPoll() {
        return fastPoll;
    }

    public void setFastPoll(int fastPoll) {
        this.fastPoll = fastPoll;
    }

}
