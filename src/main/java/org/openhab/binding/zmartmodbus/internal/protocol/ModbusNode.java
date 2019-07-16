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

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusNodeClass;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modbus node class. Represents a node in the Modbus network.
 *
 * @author Peter Kristensen
 */
public class ModbusNode {

    private Logger logger = LoggerFactory.getLogger(ModbusNode.class);

    private ModbusController controller;
    private ModbusNodeClass nodeClass;
    private ModbusFunction modbusFunction = null;
    private boolean listening = false; // i.e. sleeping

    private int nodeId = 0;
    private int unitAddress = 0;
    private int channelId = 0; // Used for Jablotron special addressing
    private int elementId = 0; // Used for Jablotron special addressing
    private ThingTypeUID thingTypeUID = null;
    private boolean configured = false;

    private ModbusDeviceInfo deviceInfo = new ModbusDeviceInfo("", "", "", "");

    /**
     * Constructor. Creates a new instance of the ModbusNode class.
     *
     * @param nodeId the node ID to use.
     * @param controller the modbus controller instance
     */
    public ModbusNode(int nodeId, ModbusController controller) {
        logger.info("NODE {}: Creating a new modbus node", nodeId);
        this.nodeId = nodeId;
        this.nodeClass = ModbusNodeClass.Unknown;
        this.channelId = ID_NOT_USED;
        this.elementId = ID_NOT_USED;
        this.controller = controller;
        this.configured = false;
        setListening(false);
    }

    /**
     * Gets the node ID.
     *
     * @return the node id
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * @return the channelId
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * @param channelId the channelId to set
     */
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    /**
     * @return the elementId
     */
    public int getElementId() {
        return elementId;
    }

    /**
     * @param elementId the elementId to set
     */
    public void setElementId(int elementId) {
        this.elementId = elementId;
    }

    /**
     * @return the unitAddress
     */
    public int getUnitAddress() {
        return unitAddress;
    }

    /**
     * @param unitAddress the unitAddress to set
     */
    public void setUnitAddress(int unitAddress) {
        this.unitAddress = unitAddress;
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

    /*
     * Set callback controller
     *
     * @param controller
     */
    public void setController(ModbusController controller) {
        this.controller = controller;
    }

    /*
     * Get callback controller
     *
     * @return controller
     *
     */
    public ModbusController getController() {
        return controller;
    }

    /**
     * Get the thingTypeUID
     *
     * @return the thingTypeUID
     */
    public ThingTypeUID getThingTypeUID() {
        return thingTypeUID;
    }

    /**
     * Set the ThingTypeUID
     *
     * @param thingTypeUID
     */
    public void setThingTypeUID(ThingTypeUID thingTypeUID) {
        this.thingTypeUID = thingTypeUID;
    }

    public ModbusNodeClass getNodeClass() {
        return nodeClass;
    }

    public void setNodeClass(ModbusNodeClass nodeClass) {
        switch (nodeClass) {
            case JablotronAC116:
            case JablotronActuator:
            case JablotronTP150:
                modbusFunction = new ModbusFunctionJablotron(controller.getBridgeHandler());
                break;
            default:
                modbusFunction = new ModbusFunction(controller.getBridgeHandler());
                break;
        }
        logger.debug("New slave created {}", modbusFunction.getClass());

        this.nodeClass = nodeClass;
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

    @Override
    public String toString() {
        return String.format("Node %d. ThingTypeUID %s", nodeId, thingTypeUID);
    }

    public boolean getApplicationUpdateReceived() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean state) {
        configured = state;
    }
}
