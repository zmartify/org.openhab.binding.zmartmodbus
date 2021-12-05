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

/**
 * This class represents a message which is used in modbus API interface to communicate with SlaveListener
 *
 * A ModbusFunction message frame is made up as follows
 *
 ***** TO BE DEFINED ******
 *
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
public class ModbusMessage {

    private int dataSetId;
    private Object payload = null;
    private boolean internal = true;

    /**
     * Constructor. Creates a new instance of the ModbusMessage class.
     */
    public ModbusMessage() {
    }

    public ModbusMessage(final int dataSetId, final Object payload, final boolean internal) {
        super();
        this.dataSetId = dataSetId;
        this.payload = payload;
        this.internal = internal;
    }

    public ModbusMessage(final int dataSetId, final Object payload) {
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

    public void setInternal(final boolean internal) {
        this.internal = internal;
    }

    public boolean isLast() {
        return false;
    }

    @Override
    public String toString() {
        return "ModbusMessage id = (" + dataSetId + ") " +(internal ? "internal" : "");
    }
}
