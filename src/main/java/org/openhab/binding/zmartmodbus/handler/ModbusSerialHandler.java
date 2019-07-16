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

import java.io.IOException;
import java.util.TooManyListenersException;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.eclipse.smarthome.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.config.ModbusSerialConfiguration;
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

    ThingTypeUID thingTypeUID = BRIDGE_TYPE_SERIAL;

    private SerialPortManager serialPortManager;

    public ModbusSerialHandler(Bridge thing, SerialPortManager serialPortManager) {
        super(thing);
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void initialize() {
        super.initialize();
        logger.debug("Initializing ZmartModbus serial controller.");

        transceiver = new ModbusSerialTransceiver(serialPortManager, getConfigAs(ModbusSerialConfiguration.class));

        try {
            transceiver.connect();
        } catch (PortInUseException e) {
            logger.error("PortInUse");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    ModbusBindingConstants.OFFLINE_SERIAL_INUSE);
        } catch (UnsupportedCommOperationException e) {
            logger.error("Unsupported");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    ModbusBindingConstants.OFFLINE_SERIAL_UNSUPPORTED);
        } catch (IOException e) {
            logger.error("IOException {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
        } catch (TooManyListenersException e) {
            logger.error("TooManyListenersException");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    ModbusBindingConstants.OFFLINE_SERIAL_LISTENERS);
        }

        initializeNetwork();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

}
