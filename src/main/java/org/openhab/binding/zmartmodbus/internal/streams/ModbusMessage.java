/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.streams;

/**
 * This class represents a message which is used in modbus API interface to communicate with SlaveListener
 *
 * A ModbusFunction message frame is made up as follows
 *
 ***** TO BE DEFINED ******
 *
 *
 * @author Peter Kristensen
 *
 */
public class ModbusMessage {

    private int dataSetId;
    private Object payload = null;
    private boolean internal = false;

    /**
     * Constructor. Creates a new instance of the ModbusMessage class.
     */
    public ModbusMessage() {
    }

    public ModbusMessage(int dataSetId, Object payload, boolean internal) {
        super();
        this.dataSetId = dataSetId;
        this.payload = payload;
        this.internal = internal;
    }

    public ModbusMessage(int dataSetId, Object payload) {
        this(dataSetId, payload, false);
    }

    public int getDataSetId() {
        return dataSetId;
    }

    public Object getPayload() {
        return payload;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isLast() {
        // TODO Auto-generated method stub
        return false;
    }
}
