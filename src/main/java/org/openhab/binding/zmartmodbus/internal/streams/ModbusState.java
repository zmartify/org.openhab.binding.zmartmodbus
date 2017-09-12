/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.streams;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.State;

/**
 *
 * @author Peter Kristensen
 *
 */
public class ModbusState {

    private ChannelUID uid;
    private State state;

    /**
     * Constructor. Creates a new instance of the ModbusMessage class.
     */
    public ModbusState() {
    }

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
        // TODO Auto-generated method stub
        return false;
    }
}
