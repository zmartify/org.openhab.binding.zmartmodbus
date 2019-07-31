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
package org.openhab.binding.zmartmodbus.internal.protocol;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
@NonNullByDefault
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

    @Override
    public String toString() {
        return String.format("%s H/W %s S/W %s S/N %s", deviceName, HWVersion, SWVersion, serialNumber);
    }
}
