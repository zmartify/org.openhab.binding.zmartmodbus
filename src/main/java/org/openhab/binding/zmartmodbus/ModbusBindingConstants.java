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

import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link ModbusBinding} class defines some constants public that might be
 * used from other bundles as well.
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
@NonNullByDefault
public class ModbusBindingConstants {

        public static final String BINDING_ID = "zmartmodbus";

        public static final String SERIAL_PORT = "serialbridge"; // Serial RS 485
        public static final String TCP_PORT = "tcpbridge"; // Serial TCP port
        public static final String RTU_PORT = "rtubridge"; // TCP/IP RTU

        // List of all Thing Type UIDs for controllers (i.e. bridge)
        public static final ThingTypeUID BRIDGE_TYPE_TCP = new ThingTypeUID(BINDING_ID, TCP_PORT);
        public static final ThingTypeUID BRIDGE_TYPE_RTU = new ThingTypeUID(BINDING_ID, RTU_PORT);
        public static final ThingTypeUID BRIDGE_TYPE_SERIAL = new ThingTypeUID(BINDING_ID, SERIAL_PORT);

        public static Set<ThingTypeUID> SUPPORTED_BRIDGE_TYPES_UIDS = Stream
                        .of(BRIDGE_TYPE_SERIAL, BRIDGE_TYPE_TCP, BRIDGE_TYPE_RTU).collect(toSet());

        // List of all Thing Type UIDs for slaves (i.e. modbus devices and their virtual
        // subslaves)
        public static final String MODBUS_THING = BINDING_ID + ":device";
        public static final ThingTypeUID MODBUS_THING_UID = new ThingTypeUID(MODBUS_THING);

        public static final ThingTypeUID THING_TYPE_MODBUS_SLAVE = new ThingTypeUID(BINDING_ID, "slave");
        public static final ThingTypeUID THING_TYPE_MODBUS_SUBSLAVE = new ThingTypeUID(BINDING_ID, "subslave");
        // List of all Slave(THING) Type UIDs

        public static final String THING_JABLOTRON_AC116 = "jablotron_ac116";
        public static final String THING_JABLOTRON_TP150 = "jablotron_tp150";
        public static final String THING_JABLOTRON_ACTUATOR = "jablotron_actuator";
        public static final String THING_NILAN_COMFORT300 = "jablotron_comfort300";

        public static final Set<String> SUPPORTED_SLAVES = Stream.of(THING_JABLOTRON_AC116, THING_NILAN_COMFORT300)
                        .collect(toSet());

        public static final ThingTypeUID THING_TYPE_JABLOTRON_AC116 = new ThingTypeUID(BINDING_ID,
                        THING_JABLOTRON_AC116);
        public static final ThingTypeUID THING_TYPE_JABLOTRON_TP150 = new ThingTypeUID(BINDING_ID,
                        THING_JABLOTRON_TP150);
        public static final ThingTypeUID THING_TYPE_JABLOTRON_ACTUATOR = new ThingTypeUID(BINDING_ID,
                        THING_JABLOTRON_ACTUATOR);
        public static final ThingTypeUID THING_TYPE_NILAN_COMFORT_300 = new ThingTypeUID(BINDING_ID,
                        THING_NILAN_COMFORT300);

        public static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
                        .of(THING_TYPE_JABLOTRON_AC116, THING_TYPE_NILAN_COMFORT_300, THING_TYPE_JABLOTRON_ACTUATOR, THING_TYPE_JABLOTRON_TP150).collect(toSet());

        public static Set<ThingTypeUID> SUPPORTED_SLAVES_THING_TYPES_UIDS = Stream
                        .of(THING_TYPE_JABLOTRON_ACTUATOR, THING_TYPE_JABLOTRON_TP150).collect(toSet());

        // List of all Channel ids

        public static final String CHANNEL_SWITCH = "switch";
        public static final String CHANNEL_CONTACT = "contact";
        public static final String CHANNEL_DATETIME = "datetime";
        public static final String CHANNEL_DIMMER = "dimmer";
        public static final String CHANNEL_NUMBER = "number";
        public static final String CHANNEL_STRING = "string";
        public static final String CHANNEL_ROLLERSHUTTER = "rollershutter";

        public static final String CHANNEL_MESSAGE_COUNT = "message_count";
        public static final String CHANNEL_TIMEOUT_COUNT = "timeout_count";
        public static final String CHANNEL_FAILED_COUNT = "failed_count";

        public static final String CHANNEL_LAST_READ_SUCCESS = "lastReadSuccess";
        public static final String CHANNEL_LAST_READ_ERROR = "lastReadError";
        public static final String CHANNEL_LAST_WRITE_SUCCESS = "lastWriteSuccess";
        public static final String CHANNEL_LAST_WRITE_ERROR = "lastWriteError";
        public static final String CHANNEL_SUCCESS_READ_COUNT = "msgSuccessReadCnt";
        public static final String CHANNEL_ERROR_READ_COUNT = "msgErrorReadCnt";
        public static final String CHANNEL_SUCCESS_WRITE_COUNT = "msgSuccessReadCnt";
        public static final String CHANNEL_ERROR_WRITE_COUNT = "msgErrorReadCnt";

        public static final int MAX_NODE_COUNT = 232;

        // Meta data for transferring information to i.e. Watson
        public static final String META_THERMOSTATID = "thermostatId";
        public static final String META_HEATUNITID = "heatUnitId";

        /**
         * CONFIGURATION PARAMETERS
         * 
         */
        public final static String CONFIGURATION_MASTER = "controller_master";

        public static final String CONFIGURATION_TCPPORT = "tcpport"; // IP port or serial port
        public static final String CONFIGURATION_HOST = "host"; // IP address of modbus transceiver

        public static final int RTU_MODE = 0;
        public static final int BIN_MODE = 1;
        public static final int ASCII_MODE = 2;

        public static final int SERIAL_RECEIVE_TIMEOUT = 200;

        public static final String DFAULT_CONNTYPE = SERIAL_PORT;

        public static final int DEFAULT_LENGTH = 1;

        public static final String DEVICE_ID = "deviceId";
        public static final int ID_NOT_USED = 63;

        public static final String SUB_TYPE = "subType";

        public static final String PROPERTY_NODECLASS = "modbus_nodeclass";
        public static final String PROPERTY_PARENTTHINGUID = "modbus_parentthinguid";
        public static final String PROPERTY_CHANNELID = "modbus_channelid";
        public static final String PROPERTY_ELEMENTID = "modbus_elementid";

        // Indicated 'slave' set 'off' or does not exist in configuration
        public static final int SLAVE_UNAVAILABLE = 0;

        // Offset to separate vendor specific ModbusFunction function codes
        public static final int CUSTOMCODE_STANDARD = 0x0000;
        public static final int CUSTOMCODE_JABLOTRON = 0x0100;
        public static final int CUSTOMCODE_NILAN = 0x0200;

        public static final String PROPERTY_CHANNELCFG_DATASET = "dataset";
        public static final String PROPERTY_CHANNELCFG_VALUETYPE = "valuetype";
        public static final String PROPERTY_CHANNELCFG_INDEX = "index";
        public static final String PROPERTY_CHANNELCFG_REPORTON = "reportOn";

        public final static String OFFLINE_CTLR_OFFLINE = "@text/modbus.thingstate.controller_offline";
        public final static String OFFLINE_CTLR_ONLINE = "@text/modbus.thingstate.controller_online";
        public final static String OFFLINE_NODE_DEAD = "@text/modbus.thingstate.node_dead";
        public final static String OFFLINE_SERIAL_EXISTS = "@text/modbus.thingstate.serial_notfound";
        public final static String OFFLINE_SERIAL_INUSE = "@text/modbus.thingstate.serial_inuse";
        public final static String OFFLINE_SERIAL_UNSUPPORTED = "@text/modbus.thingstate.serial_unsupported";
        public final static String OFFLINE_SERIAL_LISTENERS = "@text/modbus.thingstate.serial_listeners";

        /**
         * Presents all supported Thing types by Zmartify MODBUS binding.
         */

        public static final Integer ACTION_CHECK_VALUE = new Integer(-232323);

}
