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
package org.openhab.binding.zmartmodbus.internal.streams;

import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 */

public class ModbusState {

    private ChannelUID uid;
    private State state;

    /**
     * Constructor. Creates a new instance of the ModbusMessage class.
     */
    public ModbusState(ChannelUID uid, State state) {
        super();
        this.uid = uid;
        this.state = state;
    }

    public ChannelUID getUid() {
        return uid;
    }

    public State getState() {
        return state;
    }

    public boolean isLast() {
        return false;
    }
}
