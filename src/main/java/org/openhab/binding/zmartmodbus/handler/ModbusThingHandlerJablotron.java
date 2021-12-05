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
package org.openhab.binding.zmartmodbus.handler;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_TYPE_JABLOTRON_ACTUATOR;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_JABLOTRON_ACTUATOR_NAME;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_TYPE_JABLOTRON_TP150;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_JABLOTRON_TP150_NAME;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registerToUnsignedShort;

import java.util.Arrays;

import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusDeviceInfo;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunctionJablotron;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
import org.openhab.binding.zmartmodbus.internal.util.Jablotron;
import org.openhab.binding.zmartmodbus.internal.util.Register;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 */

public class ModbusThingHandlerJablotron extends ModbusThingHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusThingHandler.class);

    public ModbusThingHandlerJablotron(Thing modbusDevice) {
        super(modbusDevice);
        this.modbusFunction = new ModbusFunctionJablotron();
        logger.debug("ModbusThingHandler Jablotron loaded...");
    }

    /**
     * Received discovery message from Message Listener ThingHandler
     * 
     * @param modbusMessage
     */
    @Override
    public void handleInternalMsg(ModbusMessage modbusMessage) {
        int dataSetId = modbusMessage.getDataSetId();
        String dataSetKey = getController().getModbusFactory().getDataSets().getDataSetKey(dataSetId);

        logger.debug("Received internal message - dataSet: {}", dataSetKey);

        switch (dataSetKey) {
            case "get-device-info": {
                byte[] response = (byte[]) modbusMessage.getPayload();

                // We got a response, no build the deviceInfo
                String serialNo = String.format("%2$08d-%1$08d",
                        registerToUnsignedShort(Arrays.copyOfRange(response, 0, 2)),
                        registerToUnsignedShort(Arrays.copyOfRange(response, 2, 4)));
                String hwVersion = "MC110" + String.format("%1$02d", response[4] & 0xEF);
                String swVersion = "MC610" + String.format("%1$02d.%2$02d",
                        registerToUnsignedShort(Arrays.copyOfRange(response, 6, 8)) & 0x0FF0 >> 4,
                        registerToUnsignedShort(Arrays.copyOfRange(response, 6, 8)) & 0x000F);
                String deviceName = "AC-"
                        + String.format("%1$03d", registerToUnsignedShort(Arrays.copyOfRange(response, 8, 10)));

                ModbusDeviceInfo modbusDeviceInfo = new ModbusDeviceInfo(serialNo, hwVersion, swVersion, deviceName);

                getBridgeHandler().handleUpdate(
                        new ChannelUID(getThing().getUID(), ModbusBindingConstants.CHANNEL_DEVICE_INFO),
                        new StringType(modbusDeviceInfo.toString()));
                break;
            }
            default: 
                if (dataSetKey.contains("discovery")) {

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
                                getDiscoveryService().deviceDiscovered(THING_TYPE_JABLOTRON_ACTUATOR, THING_JABLOTRON_ACTUATOR_NAME,thing.getUID(), i,
                                        ID_NOT_USED);
                            }
                        }
                        // We have discover a thermostat
                        getDiscoveryService().deviceDiscovered(THING_TYPE_JABLOTRON_TP150,THING_JABLOTRON_TP150_NAME, thing.getUID(), lowestChannel,
                                Jablotron.getPage(getController().getModbusFactory().getDataSets().getDataSet(dataSetId)
                                        .getStart()));
                    }
                }
                break;
            }
        }
    }
}