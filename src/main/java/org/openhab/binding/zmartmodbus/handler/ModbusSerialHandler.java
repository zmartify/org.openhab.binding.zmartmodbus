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

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.BRIDGE_TYPE_SERIAL;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.zmartmodbus.internal.config.ModbusSerialConfiguration;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.transceiver.ModbusSerialTransceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusSerialHandler} is responsible for the serial communications
 * to the ZWave stick.
 * <p>
 * The {@link ModbusSerialHandler} is a SmartHome bridge. It handles the serial
 * communications, and provides a number of channels that feed back serial
 * communications statistics.
 *
 * @author Chris Jackson - Initial contribution
 * 
 */
public class ModbusSerialHandler extends ModbusBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(ModbusSerialHandler.class);

    /**
     * Constants for managing the ModbusFunction protocol
     */
    static final String PROTOCOL_NAME = "serial";

    ModbusSerialConfiguration modbusSerialConfig;

    ThingTypeUID thingTypeUID = BRIDGE_TYPE_SERIAL;

    private SerialPortManager serialPortManager;

    private ScheduledFuture<?> initializationFuture;

    public ModbusSerialHandler(Bridge thing, SerialPortManager serialPortManager) {
        super(thing);
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void initialize() {
        super.initialize();
        logger.debug("Initializing Modbus Serial controller.");

        modbusSerialConfig = getConfigAs(ModbusSerialConfiguration.class);

        if (getModbusIO().getTransceiver() == null) {
            getModbusIO().setTransceiver(new ModbusSerialTransceiver(serialPortManager, modbusSerialConfig, counters));
        } else {
            logger.error("Trying to set up IO controller twice");
        }

        if (this.initializationFuture == null || this.initializationFuture.isDone()) {
            initializationFuture = scheduler.scheduleWithFixedDelay(this::initializeBridgeStatusAndPropertiesIfOffline,
                    0, 300, TimeUnit.SECONDS);
        }

    }
    private void initializeBridgeStatusAndPropertiesIfOffline() {
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getStatus() == ThingStatus.ONLINE) {
            return;
        }
        updateStatus(ThingStatus.OFFLINE,ThingStatusDetail.CONFIGURATION_PENDING,"Opening serial port");
        try {
            logger.debug("We wil not connect and then initialize the Network");
            getModbusIO().getTransceiver().connect();
            initializeNetwork();
            updateStatus(ThingStatus.ONLINE);
        } catch (ModbusProtocolException e) {
            logger.error("IOException {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
            return;
        }

        updateStatus(ThingStatus.ONLINE);
    }


    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    public ModbusSerialConfiguration getModbusSerialConfig() {
        return modbusSerialConfig;
    } 
}
