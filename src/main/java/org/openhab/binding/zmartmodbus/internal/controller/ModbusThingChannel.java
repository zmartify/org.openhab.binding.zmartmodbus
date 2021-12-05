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
package org.openhab.binding.zmartmodbus.internal.controller;

import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusDataType;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusUnitsOfMeasure;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusValueClass;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
public class ModbusThingChannel {

    private ThingUID thingUID;
    private ChannelUID uid;
    private String dataSetKey;
    private int index;
    private ModbusValueClass valueClass;
    private ModbusDataType dataType;
    private ModbusUnitsOfMeasure unitsOfMeasure;
    private int scale;
    private State state = null;
    private State oldState = null;
    private ModbusReportOn reportOn;
    private boolean internal = false; // True if channel is only used internally by controller

    public ModbusThingChannel(ThingUID thingUID, ChannelUID uid, String dataSetKey, ModbusValueClass valueClass, int index,
            ModbusUnitsOfMeasure unitsOfMeasure, int scale, ModbusReportOn reportOn) {
        this.thingUID = thingUID;
        this.uid = uid;
        this.dataSetKey = dataSetKey;
        this.valueClass = valueClass;
        this.dataType = ModbusDataType.Unknown;
        this.index = index;
        this.unitsOfMeasure = unitsOfMeasure;
        this.scale = scale;
        this.state = UnDefType.UNDEF;
        this.oldState = UnDefType.UNDEF;
        this.reportOn = reportOn;
        this.internal = false;
    }

    /**
     * @return the ThingUID
     */
    public ThingUID getThingUID() {
        return thingUID;
    }

    /**
     * @return the uid
     */
    public ChannelUID getUID() {
        return uid;
    }

    /**
     * @return the dataSetId
     */
    public String getDataSetKey() {
        return dataSetKey;
    }

    public ModbusDataType getDataType() {
        return dataType;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    public State getState() {
        return state;
    }

    public State getOldState() {
        return oldState;
    }

    public void updateState(State state) {
        this.oldState = this.state;
        this.state = state;
    }

    public boolean stateChanged() {
        if (oldState.equals(state)) {
            return false;
        } else {
            return true;
        }
    }

    public ModbusReportOn getReportOn() {
        return reportOn;
    }

    public void setReportOn(ModbusReportOn reportOn) {
        this.reportOn = reportOn;
    }

    /**
     * @return the valueClass
     */
    public ModbusValueClass getValueClass() {
        return valueClass;
    }

    public int getScale() {
        return scale;
    }

    public ModbusUnitsOfMeasure getUnitsOfMeasure() {
        return unitsOfMeasure;
    }

    public boolean isLast() {
        return false;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }
}
