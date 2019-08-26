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
package org.openhab.binding.zmartmodbus.handler;

<<<<<<< HEAD
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.META_HEATUNITID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.META_THERMOSTATID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELCFG_DATASET;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELCFG_INDEX;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELCFG_REPORTON;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELCFG_VALUETYPE;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_TYPE_JABLOTRON_ACTUATOR;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_TYPE_JABLOTRON_TP150;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusDataSetClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusMessageClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusNodeClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusValueClass;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.config.ModbusThingConfiguration;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusThingChannel;
import org.openhab.binding.zmartmodbus.internal.discovery.ModbusSlaveDiscoveryService;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusDataSet;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunction;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunctionJablotron;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
import org.openhab.binding.zmartmodbus.internal.util.Jablotron;
=======
import java.util.Arrays;

import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunctionJablotron;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
>>>>>>> 9285026f58efc8c41d961888881d57e72019334a
import org.openhab.binding.zmartmodbus.internal.util.Register;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

<<<<<<< HEAD
/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 */

public class ModbusThingHandlerJablotron extends ModbusThingHandler {


    private Logger logger = LoggerFactory.getLogger(ModbusThingHandler.class);

    protected ModbusFunction modbusFunction = new ModbusFunctionJablotron();

    public ModbusThingHandlerJablotron(Thing modbusDevice) {
        super(modbusDevice);
        logger.debug("ModbusThingHandler Jablotron");
    }
=======

    private Logger logger = LoggerFactory.getLogger(ModbusThingHandlerJablotron.class);

    public ModbusThingHandlerJablotron(Thing modbusDevice) {
        super(modbusDevice);
        if (getBridge() != null) {
        this.modbusFunction = new ModbusFunctionJablotron((ModbusBridgeHandler) getBridge().getHandler());
        } else {
            logger.error("Bridge not configured");
        }
    }

        /**
     * Received discovery message from Message Listener ThingHandler
     * 
     * @param modbusMessage
     */
    @Override   
    public void receivedDiscovery(ModbusMessage modbusMessage) {
        logger.debug("Received discovery message");
        int dataSetId = modbusMessage.getDataSetId();
        int elementAddress = Register.registersToIntSwap((byte[]) modbusMessage.getPayload(), 0);

        if (elementAddress != 0) {
            BitVector assignmentMap = BitVector
                    .createBitVectorSwap(Arrays.copyOfRange(((byte[]) modbusMessage.getPayload()), 4, 8));
            // assignmentMap.setMSBAccess();
            assignmentMap.forceSize(17);
            BitVector status = BitVector
                    .createBitVectorSwap(Arrays.copyOfRange(((byte[]) modbusMessage.getPayload()), 16, 18), 16);
            // status.setMSBAccess();
            if (!status.getBit(9)) {
                // We assume it is a Thermostat as it is not a magnetic contact
                int lowestChannel = 16;
                for (int i = 0; i < 16; i++) {
                    if (assignmentMap.getBit(i)) {
                        // First channel found, set lowestChannel equal it.
                        if (lowestChannel == 16) {
                            lowestChannel = i;
                        }
                        // We have discovered an actuator
                        discoveryService.deviceDiscovered(THING_TYPE_JABLOTRON_ACTUATOR, thing.getUID(), i,
                                ID_NOT_USED);
                    }
                }
                // We have discover a thermostat
                discoveryService.deviceDiscovered(THING_TYPE_JABLOTRON_TP150, thing.getUID(), lowestChannel, Jablotron
                        .getPage(getController().getModbusFactory().getDataSets().getDataSet(dataSetId).getStart()));
            }
        }
    }

    @Override
    public ModbusDeviceInfo getDeviceInfo(int unitAddr) {
        byte[] response;
        try {
            response = readInputRegisters(unitAddr, Jablotron.getAddress(0x07, 0, 0), 5);
        } catch (ModbusProtocolException e) {
            return new ModbusDeviceInfo("serialno", "hwVersion", "swVersion", "deviceName");
        }
        // We got a response, no build the deviceInfo
        String serialNo = String.format("%2$08d-%1$08d", registerToUnsignedShort(Arrays.copyOfRange(response, 0, 2)),
                registerToUnsignedShort(Arrays.copyOfRange(response, 2, 4)));
        String hwVersion = "MC110" + String.format("%1$02d", response[4] & 0xEF);
        String swVersion = "MC610" + String.format("%1$02d.%2$02d",
                registerToUnsignedShort(Arrays.copyOfRange(response, 6, 8)) & 0x0FF0 >> 4,
                registerToUnsignedShort(Arrays.copyOfRange(response, 6, 8)) & 0x000F);
        String deviceName = "AC-"
                + String.format("%1$03d", registerToUnsignedShort(Arrays.copyOfRange(response, 8, 10)));
                return new ModbusDeviceInfo(serialNo, hwVersion, swVersion, deviceName);
    }


>>>>>>> 9285026f58efc8c41d961888881d57e72019334a
}