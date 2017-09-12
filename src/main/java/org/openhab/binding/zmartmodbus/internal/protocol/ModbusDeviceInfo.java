package org.openhab.binding.zmartmodbus.internal.protocol;

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
