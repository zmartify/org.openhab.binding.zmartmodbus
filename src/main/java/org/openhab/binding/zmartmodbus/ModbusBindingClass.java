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
package org.openhab.binding.zmartmodbus;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.CUSTOMCODE_JABLOTRON;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_JABLOTRON_AC116;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_JABLOTRON_ACTUATOR;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_JABLOTRON_TP150;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_NILAN_COMFORT300;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunction;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunctionJablotron;

/**
 * supported modbus commands
 *
 * @author matt.demaree - Initial contribution
 * @author Peter Kristensen - Adopted and extended for ZmartModbus
 *
 */

@NonNullByDefault
public class ModbusBindingClass {

    private ModbusBindingClass() {
    };

    public static final int READ_COIL_STATUS = 1;
    public static final int READ_INPUT_STATUS = 2;
    public static final int READ_HOLDING_REGS = 3;
    public static final int READ_INPUT_REGS = 4;
    public static final int FORCE_SINGLE_COIL = 5;
    public static final int PRESET_SINGLE_REG = 6;
    public static final int READ_EXCEPTION_STATUS = 7;
    public static final int GET_COMM_EVENT_COUNTER = 11;
    public static final int GET_COMM_EVENT_LOG = 12;
    public static final int REPORT_SLAVE_ID = 17;
    public static final int READ_DEVICE_IDENTIFICATION = 43;
    public static final int FORCE_MULTIPLE_COILS = 15;
    public static final int PRESET_MULTIPLE_REGS = 16;

    /**
     * Special Functions blok 1
     * HighByte set to blok # = equals special functions
     *
     * Jablotron AC-116 ModbusFunction function codes
     *
     */
    public static final int ENUMERATION = CUSTOMCODE_JABLOTRON | 0x6D;
    public static final int READ_REGISTER_FROM_INDEX = CUSTOMCODE_JABLOTRON | 0x43;
    public static final int WRITE_REGISTER_TO_INDEX = CUSTOMCODE_JABLOTRON | 0x44;
    public static final int WRITE_REGISTER_TO_ADDRESS = CUSTOMCODE_JABLOTRON | 0x42;
    public static final int WRITE_REGISTER_MASKED_TO_INDEX = CUSTOMCODE_JABLOTRON | 0x45;
    public static final int WRITE_REGISTER_MASKED_TO_ADDRESS = CUSTOMCODE_JABLOTRON | 0x46;

    /**
     * Defines the byte representation of the coil state <b>on</b>.
     */
    public static final int COIL_ON = (byte) 255;

    /**
     * Defines the byte representation of the coil state <b>pos</b>.
     */
    public static final int COIL_OFF = 0;

    /**
     * Defines the word representation of the coil state <b>on</b>.
     */
    public static final byte[] COIL_ON_BYTES = { (byte) COIL_ON, (byte) COIL_OFF };

    /**
     * Defines the word representation of the coil state <b>pos</b>.
     */
    public static final byte[] COIL_OFF_BYTES = { (byte) COIL_OFF, (byte) COIL_OFF };

    /**
     * Defines the maximum number of bits in multiple read/write
     * of input discretes or coils (<b>2000</b>).
     */
    public static final int MAX_BITS = 2000;

    /**
     * Defines the ModbusFunction slave exception offset that is added to the
     * function code, to flag an exception.
     */
    public static final int EXCEPTION_OFFSET = 128; // the last valid function code is 127

    /**
     * Defines the ModbusFunction slave exception type <tt>illegal function</tt>.
     * This exception code is returned if the slave:
     * <ul>
     * <li>does not implement the function code <b>or</b></li>
     * <li>is not in a state that allows it to process the function</li>
     * </ul>
     */
    public static final int ILLEGAL_FUNCTION_EXCEPTION = 1;

    /**
     * Defines the ModbusFunction slave exception type <tt>illegal data address</tt>.
     * This exception code is returned if the reference:
     * <ul>
     * <li>does not exist on the slave <b>or</b></li>
     * <li>the combination of reference and length exceeds the bounds
     * of the existing registers.
     * </li>
     * </ul>
     */
    public static final int ILLEGAL_ADDRESS_EXCEPTION = 2;

    /**
     * Defines the ModbusFunction slave exception type <tt>illegal data value</tt>.
     * This exception code indicates a fault in the structure of the data values
     * of a complex request, such as an incorrect implied length.<br>
     * <b>This code does not indicate a problem with application specific validity
     * of the value.</b>
     */
    public static final int ILLEGAL_VALUE_EXCEPTION = 3;

    /**
     * Defines the default port number of ModbusFunction
     * (=<tt>502</tt>).
     */
    public static final int DEFAULT_PORT = 502;

    /**
     * Defines the maximum message length in bytes
     * (=<tt>256</tt>).
     */
    public static final int MAX_MESSAGE_LENGTH = 256;

    /**
     * Defines the default transaction identifier (=<tt>0</tt>).
     */
    public static final int DEFAULT_TRANSACTION_ID = 0;

    /**
     * Defines the default protocol identifier (=<tt>0</tt>).
     */
    public static final int DEFAULT_PROTOCOL_ID = 0;

    /**
     * Defines the default unit identifier (=<tt>0</tt>).
     */
    public static final int DEFAULT_UNIT_ID = 0;

    /**
     * Defines the default setting for validity checking
     * in transactions (=<tt>true</tt>).
     */
    public static final boolean DEFAULT_VALIDITYCHECK = true;

    /**
     * Defines the default setting for I/O operation timeouts
     * in milliseconds (=<tt>3000</tt>).
     */
    public static final int DEFAULT_TIMEOUT = 3000;

    /**
     * Defines the default reconnecting setting for
     * transactions (=<tt>false</tt>).
     */
    public static final boolean DEFAULT_RECONNECTING = false;

    /**
     * Defines the default amount of retires for opening
     * a connection (=<tt>3</tt>).
     */
    public static final int DEFAULT_RETRIES = 3;

    /**
     * Defines the default number of msec to delay before transmission
     * (=<tt>50</tt>).
     */
    public static final int DEFAULT_TRANSMIT_DELAY = 0;

    /**
     * Defines the maximum value of the transaction identifier.
     */
    public static final int MAX_TRANSACTION_ID = (Short.MAX_VALUE * 2) - 1;
    
    public enum DataType {
        DecimalType,
        HSBType,
        IncreaseDecreaseType,
        OnOffType,
        OpenClosedType,
        PercentType,
        StringType,
        DateTimeType,
        UpDownType,
        QuantityType,
        StopMoveType;
    }

    public enum SerialEncoding {
        Ascii("ascii"),
        RTU("rtu"),
        BIN("bin"),
        Default("default");

        private SerialEncoding(final String text) {
            this.text = text;
        }

        private final String text;

        public String getLabel() {
            return text;
        }

        public static SerialEncoding fromString(String text) {
            if (text != null) {
                for (SerialEncoding c : SerialEncoding.values()) {
                    if (text.equalsIgnoreCase(c.name())) {
                        return c;
                    }
                }
            }
            return Default;
        }

    }

    /**
     * ModbusFunction message type enumeration. Indicates whether the message is a request or a response.
     *
     */
    public static enum ModbusActionClass {
        Read, // 0x00
        Write, // 0x01
        Status // 0x02
    }

    public static enum ModbusReportOn {
        Allways("allways"),
        Change("change"),
        Never("never"),
        Unknown("unknown");

        private ModbusReportOn(final String text) {
            this.text = text;
        }

        private final String text;

        public String getLabel() {
            return text;
        }

        public static ModbusReportOn fromString(String text) {
            if (text != null) {
                for (ModbusReportOn c : ModbusReportOn.values()) {
                    if (text.equalsIgnoreCase(c.name())) {
                        return c;
                    }
                }
            }
            // Set 'Change' as default
            return Change;
        }

    }

    public static enum ModbusFeedRepeat {
        Once("once"),
        Slow("slow"),
        Fast("fast");

        private ModbusFeedRepeat(final String text) {
            this.text = text;
        }

        private final String text;

        public String getLabel() {
            return text;
        }

        public static ModbusFeedRepeat fromString(String text) {
            if (text != null) {
                for (ModbusFeedRepeat c : ModbusFeedRepeat.values()) {
                    if (text.equalsIgnoreCase(c.name())) {
                        return c;
                    }
                }
            }
            // Set 'Slow' as default
            return Slow;
        }

    }

    public static enum ModbusNodeClass {
        Master("Master", null, false), // Modbus Master i.e. the controller
        Slave("Slave", ModbusFunction.class, false), // Standard Modbus slave
        JablotronActuator(THING_JABLOTRON_ACTUATOR, ModbusFunctionJablotron.class, false), // Jablotron actuator
        JablotronTP150(THING_JABLOTRON_TP150, ModbusFunctionJablotron.class, false), // Jablotron thermostat
        JablotronAC116(THING_JABLOTRON_AC116, ModbusFunctionJablotron.class, true), // Jablotron Modbus slave
        Nilan(THING_NILAN_COMFORT300, ModbusFunction.class, false), // Nilan Modbus slave
        Unknown("Unknown", null, false);

        private ModbusNodeClass(final String text, Class<?> c, boolean discovery) {
            this.text = text;
            this.c = c;
            this.discovery = discovery;
        }

        private final String text;
        private final Class<?> c;
        private boolean discovery;

        public String getLabel() {
            return text;
        }

        public Class<?> getFunctionClass() {
            return c;
        }

        public boolean supportDiscovery() {
            return discovery;
        }

        public static ModbusNodeClass fromString(String text) {
            if (text != null) {
                for (ModbusNodeClass c : ModbusNodeClass.values()) {
                    if (text.equalsIgnoreCase(c.text)) {
                        return c;
                    }
                }
            }
            return Unknown;
        }
    }

    public static enum WeekDayClass {
        MONDAY("monday"),
        TUESDAY("tuesday"),
        WEDNESDAY("wednesday"),
        THURSDAY("thursday"),
        FRIDAY("friday"),
        SATURDAY("saturday"),
        SUNDAY("sunday");

        private WeekDayClass(final String day) {
            this.day = day;
        }

        private final String day;

        public String getDay() {
            return day;
        }

        public static WeekDayClass fromString(String day) {
            if (day != null) {
                for (WeekDayClass c : WeekDayClass.values()) {
                    if (day.equalsIgnoreCase(c.name())) {
                        return c;
                    }
                }
            }
            return null;
        }
    }

    public static enum ModbusDataSetClass {
        SmartHome("smarthome"), // DataSet for SmartHome - available to user
        Discovery("discovery"), // DataSet for discovery
        Internal("internal"), // DataSet for internal management
        Unknown("unknown");

        private ModbusDataSetClass(final String text) {
            this.text = text;
        }

        private final String text;

        public String getLabel() {
            return text;
        }

        public static ModbusDataSetClass fromString(String text) {
            if (text != null) {
                for (ModbusDataSetClass c : ModbusDataSetClass.values()) {
                    if (text.equalsIgnoreCase(c.name())) {
                        return c;
                    }
                }
            }
            return Unknown;
        }
    }

    public static enum ModbusMessageClass {
        Coil("coil"), // Read/write Coil Status
        Discrete("discrete"), // Read Input Status (readonly)
        Holding("holding"), // Read/write Holding Registers
        Input("input"), // Read Input Registers (readonly)
        GetCommEventCounter("getcommeventcounter"),
        GetCommEventLog("getcommeventlog"),
        ReadExceptionStatus("readexceptionstatus"),
        GetDeviceInfo("getdeviceinfo"),
        SetLogicalAddress("setlogicaladdress"),
        Unknown("unknown");

        private ModbusMessageClass(final String text) {
            this.text = text;
        }

        private final String text;

        public String getLabel() {
            return text;
        }

        public static ModbusMessageClass fromString(String text) {
            if (text != null) {
                for (ModbusMessageClass c : ModbusMessageClass.values()) {
                    if (text.equalsIgnoreCase(c.name())) {
                        return c;
                    }
                }
            }
            return Unknown;
        }
    }

    public static enum ModbusFunctionClass {
        ReadCoils,
        ReadDiscreteInputs,
        WriteSingleCoil,
        WriteMultipleCoils,
        ReadHoldingRegisters,
        ReadInputRegisters,
        WriteSingleRegister,
        WriteMultipleRegisters,
        ReadExceptionStatus,
        GetCommEventCounter,
        GetCommEventLog,
        GetDeviceInfo,
        SetLogicalAddress,

        // Jablotron AC-116 / Wavin AM 900 special functions
        JablotronReadRegisterFromIndex,
        JablotronWriteRegisterToIndex,
        JablotronWriteRegisterToAddress,
        JablotronWriteRegisterMaskedToIndex,
        JablotronWriteRegisterMaskedToAddress;
    }

    public static enum ModbusDataType {
        Decimal("decimal"),
        HSB("HSB"),
        IncreaseDecrease("increasedecrease"),
        OnOff("onoff"),
        OpenClosed("openclosed"),
        Percent("percent"),
        String("string"),
        DateTime("datetime"),
        UpDown("updown"),
        StopMove("stopmove"),
        Unknown("unknown");

        private ModbusDataType(final String text) {
            this.text = text;
        }

        private final String text;

        public String getLabel() {
            return text;
        }

        public static ModbusDataType fromString(String text) {
            if (text != null) {
                for (ModbusDataType c : ModbusDataType.values()) {
                    if (text.equalsIgnoreCase(c.name())) {
                        return c;
                    }
                }
            }
            return Unknown;
        }

    }

    /**
     * Value type, primary for "input" type
     */
    public static enum ModbusValueClass {
        Bit("bit", 1),
        Int8("int8", 1),
        Int16("int16", 2), // Big Endian byte order
        Int16dec("int16dec", 2), // - value in 10th -> divided by 10 when reading and multiplyed by 10 when writing
        Uint16("uint16", 2), // Big Endian byte order
        Int32("int32", 4),
        Uint32("uint32", 4),
        Float32("float32", 4),
        Int32_swap("int32_swap", 4),
        Uint32_swap("uint32_swap", 4),
        Float32_swap("float32_swap", 4),
        /*
         * Custom value class - used for special conversion
         */

        // JABLOTRON AC-116
        Custom16_power("custom16_power", 2), // 16-bit register Current consumption P = 24 x Int16 / 0,54 [mW]
        Custom32_power("custom32_power", 4), // 32-bit register Current consumption P = 24 x Int32 / 0,54 [mW]
        Custom8_4bit("custom8_4bit", 1), // 8-bit register, use only first 4 bits
        Custom8_5bit("custom8_5bit", 1), // 8-bit register, use only first 5 bits
        Custom8_6bit("custom8_6bit", 1), // 8-bit register, use only first 6 bits
        Jablotron_RSSI("jablotron_rssi", 1), // 8-bit register, for signal strength in 0.5 dBm - base -74 dBm
        Jablotron_battery("jablotron_battery", 1), // 4-bit register, for battery level in 10% units
        Jablotron_discovery("jablotron_discovery", 2), // Used for Jablotron auto-discovery
        Jablotron_schedule("jablotron_schedule", 22),
        Jablotron_elementChangeFlags("jablotron_elementChangeFlags", 8),
        Jablotron_channelChangeFlags("jablotron_channelChangeFlags", 4),
        Jablotron_packetdataChangeFlags("jablotron_packetdataChangeFlags", 4),
        Unknown("unknown", 0);

        private ModbusValueClass(final String text, final int byteSize) {
            this.text = text;
            this.byteSize = byteSize;
        }

        private final String text;
        private final int byteSize;

        public String getLabel() {
            return text;
        }

        public int size() {
            return byteSize;
        }

        public static ModbusValueClass fromString(String text) {
            if (text != null) {
                for (ModbusValueClass c : ModbusValueClass.values()) {
                    if (text.equalsIgnoreCase(c.name())) {
                        return c;
                    }
                }
            }
            return Unknown;
        }
    }

}
