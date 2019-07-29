/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.streams;

import static org.openhab.binding.zmartmodbus.ModbusBindingClass.DEFAULT_RETRIES;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusMessageClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ModbusAction {

    private Logger logger = LoggerFactory.getLogger(ModbusAction.class);

    private ThingUID thingUID;
    private int dataSetId;
    private ModbusMessageClass messageClass;
    private ModbusActionClass actionClass;  // (read or write)
    private ModbusFeedRepeat feedRepeat;    // One time or repeated modbus message
    private int start;                      // Start address
    private int length = 0;                 // Address length in words or coils
    private Object payload = null;          // Object used for writing to Modbus

    // Offset to be used with custom addressing of coils
    // - start = register address, offset = bit
    // - index = offset within register)
    private int offset = 0;
    private ModbusReportOn reportOn; // Only report on change
    private boolean internal = false;

    private AtomicInteger retryCount = new AtomicInteger(0);

    /**
     * Constructor. Creates a new instance of the ModbusMessage class.
     */
    public ModbusAction() {
        this.retryCount.set(0);
        this.payload = null;
    }

    public ModbusAction(ThingUID thingUID, int dataSetId, ModbusMessageClass messageClass, ModbusActionClass actionClass,
            ModbusFeedRepeat feedRepeat, int start, int length, int offset, ModbusReportOn reportOn) {
        super();
        this.retryCount.set(0);
        this.thingUID = thingUID;
        this.dataSetId = dataSetId;
        this.messageClass = messageClass;
        this.actionClass = actionClass;
        this.feedRepeat = feedRepeat;
        this.start = start;
        this.length = length;
        this.offset = offset;
        this.reportOn = reportOn;
    }

    public ModbusAction(ModbusDataSet dataSet, ModbusActionClass actionClass) {
        this(dataSet.getThingUID(),dataSet.getDataSetId(), dataSet.getMessageClass(), actionClass, dataSet.getFeedRepeat(),
        dataSet.getStart(), dataSet.getLength(), dataSet.getOffset(), dataSet.getReportOn());
    }

    public ModbusAction(ModbusDataSet dataSet, ModbusActionClass actionClass, ModbusFeedRepeat feedRepeat) {
        this(dataSet, actionClass);
        this.feedRepeat = feedRepeat;
    }

    public ModbusAction(ModbusDataSet dataSet, int index, ModbusActionClass actionClass, ModbusFeedRepeat feedRepeat,
            Object payload) {
        // Used for creating ModbusMessage of Action write
        this(dataSet, actionClass, feedRepeat);
        this.offset = dataSet.getOffset() + index;
        this.payload = payload;
    }

    /**
     * Get the node id - internal reference
     * 
     * @return
     */
    public ThingUID getThingUID() {
        return thingUID;
    }

    public int getDataSetId() {
        return dataSetId;
    }

    /**
     * Get start address of dataset
     * 
     * @return the address
     */
    public int getStart() {
        return start;
    }

    public int getOffset() {
        return offset;
    }

    /**
     * Get length of dataset (in words or coils)
     * 
     * @return the length
     */
    public int getLength() {
        return length;
    }

    public ModbusReportOn getReportOn() {
        return reportOn;
    }

    /**
     * Gets the message type (Request / Response).
     *
     * @return the message type
     */
    public ModbusActionClass getActionClass() {
        return actionClass;
    }

    public Object getPayload() {
        return payload;
    }

    public ModbusFeedRepeat getFeedRepeat() {
        return feedRepeat;
    }

    public void setFeedRepeat(ModbusFeedRepeat feedRepeat) {
        this.feedRepeat = feedRepeat;
    }

    public ModbusMessageClass getMessageClass() {
        return messageClass;
    }

    public boolean isLast() {
        return false;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean retry() {
        if (retryCount.getAndIncrement() < DEFAULT_RETRIES) {
            logger.debug("Thing {}: Retry message {} {} {}", thingUID, dataSetId, messageClass, actionClass);
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "modbusAction = " + thingUID.getAsString();
    }
}
