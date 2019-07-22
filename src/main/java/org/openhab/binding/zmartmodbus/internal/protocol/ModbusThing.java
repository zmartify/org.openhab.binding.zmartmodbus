/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.protocol;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;

import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusNodeClass;

/**
 * Modbus node class. Represents a node in the Modbus network.
 *
 * @author Peter Kristensen
 */
public class ModbusThing {

    private ModbusNodeClass nodeClass;
    private ModbusFunction modbusFunction = null;
    private boolean listening = false; // i.e. sleeping

    private int id = 0;

    // Special configuration parameters needed for jablotron
    private int channelId = ID_NOT_USED; // Used for Jablotron special addressing
    private int elementId = ID_NOT_USED; // Used for Jablotron special addressing

    private boolean configured = false;

    private ModbusDeviceInfo deviceInfo = new ModbusDeviceInfo("", "", "", "");

    /**
     * Constructor. Creates a new instance of the ModbusNode class.
     *
     * @param nodeId     the node ID to use.
     * @param controller the modbus controller instance
     */
    public ModbusThing(ModbusNodeClass nodeClass, ModbusFunction modbusFunction, int id, int channelId, int elementId) {
        this.nodeClass = nodeClass;
        this.modbusFunction = modbusFunction;
        this.id = id;
        this.channelId = channelId;
        this.elementId = elementId;
        setListening(false);
    }

    public int getId() {
        return id;
    }
    /**
     * @return the channelId
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * @return the elementId
     */
    public int getElementId() {
        return elementId;
    }

    /**
     * Gets whether the node is listening.
     *
     * @return boolean indicating whether the node is listening or not.
     */
    public boolean isListening() {
        return listening;
    }

    /**
     * Sets whether the node is listening.
     *
     * @param listening
     */
    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public ModbusNodeClass getNodeClass() {
        return nodeClass;
    }

    public ModbusFunction getModbusFunction() {
        return modbusFunction;
    }

    public void setDeviceInfo(ModbusDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public ModbusDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean state) {
        configured = state;
    }
}
