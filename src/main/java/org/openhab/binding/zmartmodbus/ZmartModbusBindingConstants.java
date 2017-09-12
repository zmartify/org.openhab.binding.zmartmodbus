/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;

import gnu.io.SerialPort;

/**
 * The {@link ZmartModbusBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Peter Kristensen
 */
public class ZmartModbusBindingConstants {

    public static final String BINDING_ID = "zmartmodbus";

    public static final String SERIAL_PORT = "serialbridge"; // Serial RS 485
    public static final String TCP_PORT = "tcpbridge"; // Serial TCP port
    public static final String RTU_PORT = "rtubridge"; // TCP/IP RTU

    public static final int MAX_NODE_COUNT = 232;

    // Meta data for transferring information to i.e. Watson
    public final static String META_THERMOSTATID = "thermostatId";
    public final static String META_HEATUNITID = "heatUnitId";

    // Controllers
    public final static ThingTypeUID CONTROLLER_SERIAL = new ThingTypeUID(BINDING_ID, SERIAL_PORT);
    public final static ThingTypeUID CONTROLLER_TCP = new ThingTypeUID(BINDING_ID, TCP_PORT);
    public final static ThingTypeUID CONTROLLER_RTU = new ThingTypeUID(BINDING_ID, RTU_PORT);

    public final static int CONTROLLER_NODE_ID = 0;

    /**
     * CONFIGURATION PARAMETERS
     */
    public final static String CONFIGURATION_PORT = "serialport"; // Serial port for RS 485
    public final static String CONFIGURATION_TCPPORT = "tcpport"; // IP port or serial port
    public final static String CONFIGURATION_HOST = "host"; // IP address of modbus transceiver

    // Serial port configuration parameters
    public final static String CONFIGURATION_CONNTYPE = "conntype";
    public final static String CONFIGURATION_BAUDRATE = "baudrate";
    public final static String CONFIGURATION_DATABITS = "databits";
    public final static String CONFIGURATION_STOPBITS = "stopbits";
    public final static String CONFIGURATION_PARITY = "parity";
    public final static String CONFIGURATION_TXMODE = "txmode";
    public final static String CONFIGURATION_SLOWPOLL = "slowpoll";
    public final static String CONFIGURATION_FASTPOLL = "fastpoll";
    public final static String CONFIGURATION_RESPTOUT = "resptout";

    public final static int RTU_MODE = 0;
    public final static int BIN_MODE = 1;
    public final static int ASCII_MODE = 2;

    public final static String DFAULT_CONNTYPE = SERIAL_PORT;
    public final static String DEFAULT_PORT = "/dev/AMA0";
    public final static int DEFAULT_TCPPORT = 12345;
    public final static int DEFAULT_BAUDRATE = 38400;
    public final static int DEFAULT_DATABITS = SerialPort.DATABITS_8;
    public final static int DEFAULT_STOPBITS = SerialPort.STOPBITS_1;
    public final static int DEFAULT_PARITY = 0; // 2 = even - 1 = odd
    public final static int DEFAULT_TXMODE = RTU_MODE;
    public final static int DEFAULT_POLLS = 10000;
    public final static int DEFAULT_RESPTOUT = 400;

    public final static int DEFAULT_LENGTH = 1;

    public static final String DEVICE_ID = "deviceId";
    public static final int ID_NOT_USED = 63;
    public static final int NODE_NOT_CONFIGURED = -1;

    public static final String SUB_TYPE = "subType";

    // List of all Bridge Type UIDs
    public static final String BRIDGE_TYPE_SERIAL_BRIDGE = SERIAL_PORT;
    public static final String BRIDGE_TYPE_TCP_BRIDGE = TCP_PORT;
    public static final String BRIDGE_TYPE_RTU_BRIDGE = RTU_PORT;
    public static final String BRIDGE_TYPE_UNKNOWN = "unknown";

    public final static String MODBUS_THING = BINDING_ID + ":device";
    public final static ThingTypeUID MODBUS_THING_UID = new ThingTypeUID(MODBUS_THING);

    public final static ThingTypeUID BRIDGE_SERIAL = new ThingTypeUID(BINDING_ID, BRIDGE_TYPE_SERIAL_BRIDGE);
    public final static ThingTypeUID BRIDGE_TCP = new ThingTypeUID(BINDING_ID, BRIDGE_TYPE_TCP_BRIDGE);
    public final static ThingTypeUID BRIDGE_RTU = new ThingTypeUID(BINDING_ID, BRIDGE_TYPE_RTU_BRIDGE);
    public final static ThingTypeUID BRIDGE_UNKNOWN = new ThingTypeUID(BINDING_ID, BRIDGE_TYPE_UNKNOWN);

    public final static String NODE_TITLE_FORMAT = "ModbusFunction Node %d";

    public final static String PROPERTY_NODEID = "modbus_nodeid";
    public final static String PROPERTY_NODECLASS = "modbus_nodeclass";
    public final static String PROPERTY_UNITADDRESS = "modbus_unitaddress";
    public final static String PROPERTY_CHANNELID = "modbus_channelid";
    public final static String PROPERTY_ELEMENTID = "modbus_elementid";

    // Indicated 'slave' set 'off' or does not exist in configuration
    public static final int SLAVE_UNAVAILABLE = 0;

    // List of all Slave(THING) Type UIDs
    public static final String THING_TYPE_JABLOTRON_AC116 = "jablotronac116";
    public static final String THING_TYPE_JABLOTRON_TP150 = "jablotrontp150";
    public static final String THING_TYPE_JABLOTRON_ACTUATOR = "jablotronactuator";
    public static final String THING_TYPE_NILAN_COMFORT_300 = "nilancomfort300";

    public static final ThingTypeUID THING_JABLOTRON_AC116 = new ThingTypeUID(BINDING_ID, THING_TYPE_JABLOTRON_AC116);
    public static final ThingTypeUID THING_JABLOTRON_TP150 = new ThingTypeUID(BINDING_ID, THING_TYPE_JABLOTRON_TP150);
    public static final ThingTypeUID THING_JABLOTRON_ACTUATOR = new ThingTypeUID(BINDING_ID,
            THING_TYPE_JABLOTRON_ACTUATOR);
    public static final ThingTypeUID THING_NILAN_COMFORT_300 = new ThingTypeUID(BINDING_ID,
            THING_TYPE_NILAN_COMFORT_300);

    // Offset to separate vendor specific ModbusFunction function codes
    public static final int CUSTOMCODE_STANDARD = 0x0000;
    public static final int CUSTOMCODE_JABLOTRON = 0x0100;
    public static final int CUSTOMCODE_NILAN = 0x0200;

    public static final String PROPERTY_CHANNELCFG_DATASET = "dataset";
    public static final String PROPERTY_CHANNELCFG_VALUETYPE = "valuetype";
    public static final String PROPERTY_CHANNELCFG_INDEX = "index";
    public static final String PROPERTY_CHANNELCFG_REPORTON = "reportOn";

    /**
     *
     * Presents all supported Slave types by ZmartifyMODBUS binding.
     *
     */
    public final static Set<String> SUPPORTED_SLAVES = ImmutableSet.of(THING_TYPE_JABLOTRON_AC116,
            THING_TYPE_NILAN_COMFORT_300, THING_TYPE_JABLOTRON_TP150, THING_TYPE_JABLOTRON_ACTUATOR);
    public final static Set<ThingTypeUID> SUPPORTED_SLAVE_THING_TYPES_UIDS = ImmutableSet.of(THING_JABLOTRON_AC116,
            THING_NILAN_COMFORT_300, THING_JABLOTRON_TP150, THING_JABLOTRON_ACTUATOR);
    public final static Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS = ImmutableSet.of(BRIDGE_SERIAL, BRIDGE_TCP,
            BRIDGE_RTU);

    public final static String CHANNEL_MESSAGE_COUNT = "message_count";
    public final static String CHANNEL_RESPTOUT_COUNT = "resptout_count";

    /**
     * Presents all supported Thing types by ZmartifyMODBUS binding.
     */
    public final static Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = ImmutableSet.of(THING_JABLOTRON_AC116,
            THING_NILAN_COMFORT_300);

    public final static Integer ACTION_CHECK_VALUE = new Integer(-232323);

    public final static Set<ThingTypeUID> SUPPORTED_BRIDGE_TYPES_UIDS = ImmutableSet.of(CONTROLLER_SERIAL,
            CONTROLLER_TCP, CONTROLLER_RTU);

}
