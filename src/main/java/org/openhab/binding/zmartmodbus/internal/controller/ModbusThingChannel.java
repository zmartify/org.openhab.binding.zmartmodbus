/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusDataType;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusValueClass;

/**
 *
 * @author Peter Kristensen
 *
 */
@NonNullByDefault
public class ModbusThingChannel {

    private ThingUID thingUID;
    private ChannelUID uid;
    private String dataSetKey;
    private int index;
    private ModbusValueClass valueClass;
    private ModbusDataType dataType;
    private State state;
    private State oldState;
    private ModbusReportOn reportOn;
    private boolean internal = false; // True if channel is only used internally by controller

    public ModbusThingChannel(ThingUID thingUID, ChannelUID uid, String dataSetKey, ModbusValueClass valueClass, int index,
            ModbusReportOn reportOn) {
        this.thingUID = thingUID;
        this.uid = uid;
        this.dataSetKey = dataSetKey;
        this.valueClass = valueClass;
        this.index = index;
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
        if ((state == null) || (oldState == null)) {
            return false;
        }
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
