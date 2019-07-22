/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.handler;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.BRIDGE_TYPE_TCP;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusTcpHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Peter Kristensen - Initial contribution
 */
public class ModbusTcpHandler extends ModbusBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusTcpHandler.class);

    /**
     * Constants for managing the ModbusFunction protocol
     */
    static final String PROTOCOL_NAME = "modbus";
    ThingTypeUID thingTypeUID = BRIDGE_TYPE_TCP;

    InputStream in = null;
    OutputStream out = null;
    Socket socket = null;

    public int tcpport = 999; // TODO: - need to be configured
    public boolean tcpbridge = false;
    public String host = "";
    public int respTout = 400; // TODO: - needs to be defined

    public ModbusTcpHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.info("Initializing Zmartify ModbusFunction Controller.");

        // Is it a TCP serial bridge
        tcpbridge = this.getThing().getThingTypeUID().equals(BRIDGE_TYPE_TCP);

        // Ethernet TCP/IP port
        logger.info("Setting up TCP/IP port");

/*
        host = getConfigParamStr(CONFIGURATION_HOST, "");
        tcpport = getConfigParamInt(CONFIGURATION_TCPPORT, DEFAULT_TCPPORT);
        respTout = getConfigParamInt(CONFIGURATION_RESPTOUT, DEFAULT_RESPTOUT);
        polls = getConfigParamInt(CONFIGURATION_RESPTOUT, DEFAULT_POLLS);
*/
        super.initialize();
    }

    @Override
    public void dispose() {
        super.dispose();
    }


  
    public void disconnect() {
        logger.debug("Disconnecting");

        if (out != null) {
            logger.debug("Close tcp out stream");
            IOUtils.closeQuietly(out);
        }
        if (in != null) {
            logger.debug("Close tcp in stream");
            IOUtils.closeQuietly(in);
        }

        if (socket != null) {
            logger.debug("Close socket");
            IOUtils.closeQuietly(socket);
        }

        socket = null;
        out = null;
        in = null;

        setConnected(false);

        logger.debug("Closed");
    }

    /*
     * @Override
     * public void sendMessage(byte[] data) throws IOException {
     * logger.trace("Send data (len={}): {}", data.length, DatatypeConverter.printHexBinary(data));
     * out.write(data);
     * out.flush();
     * }
     */
    /**
     * Installation of an ethernet connection to communicate
     */  
}
