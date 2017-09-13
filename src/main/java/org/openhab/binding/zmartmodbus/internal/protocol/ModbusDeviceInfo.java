/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.protocol;

/**
 *
 * @author Peter Kristensen
 *
 */
public class ModbusDeviceInfo {

    private String serialNumber;
    private String HWVersion;
    private String SWVersion;
    private String deviceName;

    /**
     * Creates a new record with device information from the Modbus Device
     *
     * @param serialNumber
     * @param hWVersion
     * @param sWVersion
     * @param deviceName
     */

    public ModbusDeviceInfo(String serialNumber, String hWVersion, String sWVersion, String deviceName) {
        super();
        this.serialNumber = serialNumber;
        HWVersion = hWVersion;
        SWVersion = sWVersion;
        this.deviceName = deviceName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getHWVersion() {
        return HWVersion;
    }

    public String getSWVersion() {
        return SWVersion;
    }

    public String getDeviceName() {
        return deviceName;
    }
}
