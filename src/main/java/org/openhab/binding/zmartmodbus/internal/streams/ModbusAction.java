/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.streams;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.DEFAULT_RETRIES;

import java.util.concurrent.atomic.AtomicInteger;

import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusActionClass;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusMessageClass;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusReportOn;
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

    private int nodeId;
    private int dataSetId;
    private ModbusMessageClass messageClass;
    private ModbusActionClass actionClass; // (read or write)
    private ModbusFeedRepeat feedRepeat;
    private int start;
    private int length = 0;
    private Object payload = null; // Object used for writing to Modbus

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
    }

    public ModbusAction(int nodeId, int dataSetId, ModbusMessageClass messageClass, ModbusActionClass actionClass,
            ModbusFeedRepeat feedRepeat, int start, int length, int offset, ModbusReportOn reportOn) {
        super();
        this.retryCount.set(0);
        this.nodeId = nodeId;
        this.dataSetId = dataSetId;
        this.messageClass = messageClass;
        this.actionClass = actionClass;
        this.feedRepeat = feedRepeat;
        this.start = start;
        this.length = length;
        this.offset = offset;
        this.reportOn = reportOn;
        this.payload = null;
    }

    public ModbusAction(ModbusDataSet dataSet, ModbusActionClass actionClass) {
        super();
        this.retryCount.set(0);
        this.nodeId = dataSet.getNodeId();
        this.dataSetId = dataSet.getDataSetId();
        this.messageClass = dataSet.getMessageClass();
        this.actionClass = actionClass;
        this.feedRepeat = dataSet.getFeedRepeat();
        this.start = dataSet.getStart();
        this.length = dataSet.getLength();
        this.offset = dataSet.getOffset();
        this.reportOn = dataSet.getReportOn();
        this.payload = null;
        this.internal = dataSet.isInternal();
    }

    public ModbusAction(ModbusDataSet dataSet, ModbusActionClass actionClass, ModbusFeedRepeat feedRepeat) {
        super();
        this.retryCount.set(0);
        this.nodeId = dataSet.getNodeId();
        this.dataSetId = dataSet.getDataSetId();
        this.messageClass = dataSet.getMessageClass();
        this.actionClass = actionClass;
        this.feedRepeat = feedRepeat;
        this.start = dataSet.getStart();
        this.length = dataSet.getLength();
        this.offset = dataSet.getOffset();
        this.reportOn = dataSet.getReportOn();
        this.payload = null;
        this.internal = dataSet.isInternal();
    }

    public ModbusAction(ModbusDataSet dataSet, int index, ModbusActionClass actionClass, ModbusFeedRepeat feedRepeat,
            Object payload) {
        // Used for creating ModbusMessage of Action write
        super();
        this.retryCount.set(0);
        this.nodeId = dataSet.getNodeId();
        this.dataSetId = dataSet.getDataSetId();
        this.messageClass = dataSet.getMessageClass();
        this.actionClass = actionClass;
        this.feedRepeat = feedRepeat;
        this.start = dataSet.getStart(); // Advance to the first bit / register
        this.length = dataSet.getLength();
        this.offset = dataSet.getOffset() + index;
        this.reportOn = dataSet.getReportOn();
        this.payload = payload;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getDataSetId() {
        return dataSetId;
    }

    public int getStart() {
        return start;
    }

    public int getOffset() {
        return offset;
    }

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
            logger.debug("NODE {}: Retry message {} {} {}", nodeId, dataSetId, messageClass, actionClass);
            return true;
        } else {
            return false;
        }
    }
}
