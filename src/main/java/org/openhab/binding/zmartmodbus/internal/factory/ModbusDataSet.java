/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.factory;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusDataSetClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusMessageClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusNodeClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;

/**
 * @author Peter Kristensen
 *
 */
public class ModbusDataSet {

    private Object payload;
    private int nodeId;
    private int dataSetId;

    private int elementId = ID_NOT_USED;
    private int channelId = ID_NOT_USED;

    private ModbusDataSetClass dataSetClass = ModbusDataSetClass.Unknown;
    private ModbusMessageClass messageClass = ModbusMessageClass.Unknown;
    private ModbusNodeClass nodeClass = ModbusNodeClass.Unknown;
    private int start;
    private int length = 0;
    private int offset = 0; // Offset to be used with custom addressing of coils
    private ModbusReportOn reportOn = ModbusReportOn.Unknown; // Only report on change
    private ModbusFeedRepeat feedRepeat = ModbusFeedRepeat.Fast;
    private boolean internal = false;

    private CopyOnWriteArrayList<ChannelUID> channels = new CopyOnWriteArrayList<>();

    public ModbusDataSet() {
        super();
    }

    public ModbusDataSet(int nodeId, ModbusMessageClass messageClass, int start, int length, int offset, int channelId,
            int elementId, ModbusReportOn reportOn, ModbusFeedRepeat feedRepeat, ModbusDataSetClass dataSetClass,
            ModbusNodeClass nodeClass) {
        this.nodeId = nodeId;
        this.dataSetId = -1; // Must be set late and before dataSet is used
        this.dataSetClass = dataSetClass;
        this.messageClass = messageClass;
        this.nodeClass = nodeClass;
        this.start = start;
        this.length = length;
        this.offset = offset;
        this.channelId = channelId;
        this.elementId = elementId;
        this.reportOn = reportOn;
        this.feedRepeat = feedRepeat;

        // Define and initialize payload
        switch (messageClass) {
        case Coil:
        case Discrete:
            this.payload = new BitVector(length);
            break;
        case Holding:
        case Input:
            this.payload = new byte[length * 2];
            Arrays.fill((byte[]) this.payload, (byte) 0);
            break;
        default:
            this.payload = null;
            break;
        }
    }

    /**
     * @param messageClass
     * @param start
     * @param length
     * @param offset
     * @param reportOn
     */
    public ModbusDataSet(int nodeId, ModbusMessageClass messageClass, int start, int length, int offset,
            ModbusReportOn reportOn, ModbusFeedRepeat feedRepeat) {
        this.nodeId = nodeId;
        this.dataSetId = -1; // Must be set late and before dataSet is used
        this.dataSetClass = ModbusDataSetClass.SmartHome;
        this.messageClass = messageClass;
        this.nodeClass = ModbusNodeClass.Unknown;
        this.start = start;
        this.length = length;
        this.offset = offset;
        this.reportOn = reportOn;
        this.feedRepeat = feedRepeat;

        // Define and initialize payload
        switch (messageClass) {
        case Coil:
        case Discrete:
            this.payload = new BitVector(length);
            break;
        case Holding:
        case Input:
            this.payload = new byte[length * 2];
            Arrays.fill((byte[]) this.payload, (byte) 0);
            break;
        default:
            this.payload = null;
            break;
        }
    }

    /**
     * @return the payload
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * @param payload the payload to set
     */
    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(int dataSetId) {
        this.dataSetId = dataSetId;
    }

    public int getElementId() {
        return elementId;
    }

    public void setElementId(int elementId) {
        this.elementId = elementId;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }


    /**
     * Add a channel to the list of channels serviced by this dataset
     * 
     * @param uid
     * @return true on channelUID added successfully
     */
    public boolean addChannel(ChannelUID uid) {
        return this.channels.add(uid);
    }

    /**
     * Remove a channel from the list of channels serviced by this dataset
     * 
     * @param uid
     * @return true on channelUID removed successfully
     */
    public boolean removeChannel(ChannelUID uid) {
        return this.channels.remove(uid);
    }

    /**
     * Get the list of channels
     * 
     * @return
     */
    public CopyOnWriteArrayList<ChannelUID> getChannels() {
        return channels;
    }

    /**
     * @return the messageClass
     */
    public ModbusMessageClass getMessageClass() {
        return messageClass;
    }

    /**
     * @return the messageClass
     */
    public ModbusDataSetClass getDataSetClass() {
        return dataSetClass;
    }

    public ModbusNodeClass getNodeClass() {
        return nodeClass;
    }

    /**
     * @return the start
     */
    public int getStart() {
        return start;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }

    /**
     * @return the reportOn
     */
    public ModbusReportOn getReportOn() {
        return reportOn;
    }

    public ModbusFeedRepeat getFeedRepeat() {
        return feedRepeat;
    }
}
