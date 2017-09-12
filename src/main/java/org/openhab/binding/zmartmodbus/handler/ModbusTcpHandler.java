/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.handler;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusProtocolErrorCode;
import org.openhab.binding.zmartmodbus.internal.util.Crc16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusTcpHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Chris Jackson - Initial contribution
 */
public class ModbusTcpHandler extends ZmartModbusHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusTcpHandler.class);

    /**
     * Constants for managing the ModbusFunction protocol
     */
    static final String PROTOCOL_NAME = "modbus";
    ThingTypeUID thingTypeUID = BRIDGE_TCP;

    private static int transactionIndex = 0;

    InputStream in = null;
    OutputStream out = null;
    Socket socket = null;

    public int tcpport = DEFAULT_TCPPORT;
    public boolean tcpbridge = false;
    public String host = "";
    public int txMode = DEFAULT_TXMODE;
    public int polls = DEFAULT_POLLS;
    public int respTout = 400; // TODO - needs to be defined

    public ModbusTcpHandler(Bridge bridge) {
        super(bridge);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialize() {
        logger.info("Initializing Zmartify ModbusFunction Controller.");

        // Is it a TCP serial bridge
        tcpbridge = this.getThing().getThingTypeUID().equals(CONTROLLER_TCP);

        // Ethernet TCP/IP port
        logger.info("Setting up TCP/IP port");

        controller = new ModbusController(this);

        host = getConfigParamStr(CONFIGURATION_HOST, "");
        tcpport = getConfigParamInt(CONFIGURATION_TCPPORT, DEFAULT_TCPPORT);
        respTout = getConfigParamInt(CONFIGURATION_RESPTOUT, DEFAULT_RESPTOUT);
        polls = getConfigParamInt(CONFIGURATION_RESPTOUT, DEFAULT_POLLS);

        super.initialize();
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void connect() throws IOException {
        logger.info("Connecting to ZmartifyModbus at {}:{} over TCP/IP", host, tcpport);
        socket = new Socket(host, tcpport);
        in = socket.getInputStream();
        out = socket.getOutputStream();

        out.flush();
        if (in.markSupported()) {
            in.reset();
        }

        setConnected(true);
    }

    @Override
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

    @Override
    public byte[] msgTransaction(byte[] msg) throws ModbusProtocolException {
        byte[] cmd = null;

        // Update message counter
        increaseMsgCounter();
        // ---------------------------------------------- Send Message
        // ---------------------------------------------------
        if (txMode == RTU_MODE) {
            if (tcpbridge) {
                // Serial TCP bridge communication
                cmd = new byte[msg.length + 6];
                // build MBAP header
                int index = getNextTransactionIndex();
                cmd[0] = (byte) (index >> 8);
                cmd[1] = (byte) index;
                cmd[2] = 0;
                cmd[3] = 0;
                // length
                int len = msg.length;
                cmd[4] = (byte) (len >> 8);
                cmd[5] = (byte) len;
                for (int i = 0; i < msg.length; i++) {
                    cmd[i + 6] = msg[i];
                }
                // No crc in ModbusFunction TCP
            } else {
                // TCP/IP RTU communication
                cmd = new byte[msg.length + 2];
                for (int i = 0; i < msg.length; i++) {
                    cmd[i] = msg[i];
                }
                // Add crc calculation to end of message
                int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
                cmd[msg.length] = (byte) crc;
                cmd[msg.length + 1] = (byte) (crc >> 8);

            }

        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.METHOD_NOT_SUPPORTED,
                    "Only RTU over TCP/IP supported");
        }

        // Check connection status and connect
        // connect();
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                    "Cannot transact on closed socket");
        }

        // Send the message
        try {
            // flush input
            while (in.available() > 0) {
                in.read();
            }
            // send all data
            out.write(cmd, 0, cmd.length);
            out.flush();
        } catch (IOException e) {
            // Assume this means the socket is closed...make sure it is
            logger.error("Socket disconnect in send: {}", e);
            disconnect();
            throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                    "Send failure: " + e.getMessage());
        }

        // ---------------------------------------------- Receive response
        // ---------------------------------------------------
        // wait for and process response

        boolean endFrame = false;
        byte[] response = new byte[262]; // response buffer
        int respIndex = 0;
        int minimumLength = 5; // default minimum message length
        if (tcpbridge) {
            minimumLength += 6;
        }
        while (!endFrame) {
            try {
                this.socket.setSoTimeout(respTout);
                int resp = in.read(response, respIndex, 1);
                if (resp > 0) {
                    respIndex += resp;
                    if (tcpbridge) {
                        if (respIndex == 7) {
                            // test modbus id
                            if (response[6] != msg[0]) {
                                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                        "incorrect modbus id " + String.format("%02X", response[6]));
                            }
                        } else if (respIndex == 8) {
                            // test function number
                            if ((response[7] & 0x7f) != msg[1]) {
                                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                        "incorrect function number " + String.format("%02X", response[7]));
                            }
                        } else if (respIndex == 9) {
                            // Check first for an Exception response
                            if ((response[7] & 0x80) == 0x80) {
                                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                        "ModbusFunction responds an error = " + String.format("%02X", response[8]));
                            } else {
                                if (response[7] == ZmartModbusBindingClass.FORCE_SINGLE_COIL
                                        || response[7] == ZmartModbusBindingClass.PRESET_SINGLE_REG
                                        || response[7] == ZmartModbusBindingClass.FORCE_MULTIPLE_COILS
                                        || response[7] == ZmartModbusBindingClass.PRESET_MULTIPLE_REGS) {
                                    minimumLength = 12;
                                } else {
                                    // bytes count
                                    minimumLength = response[8] + 9;
                                }
                            }
                        } else if (respIndex == minimumLength) {
                            endFrame = true;
                        }
                    } else {

                    }
                } else {
                    logger.error("Socket disconnect in recv");
                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, "Recv failure");
                }
            } catch (SocketTimeoutException e) {
                String failMsg = "Recv timeout";
                logger.warn(failMsg);
                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, failMsg);
            } catch (IOException e) {
                logger.error("Socket disconnect in recv: {}", e);
                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, "Recv failure");
            }

        }

        // then check for a valid message
        switch (response[7]) {
            case ZmartModbusBindingClass.FORCE_SINGLE_COIL:
            case ZmartModbusBindingClass.PRESET_SINGLE_REG:
            case ZmartModbusBindingClass.FORCE_MULTIPLE_COILS:
            case ZmartModbusBindingClass.PRESET_MULTIPLE_REGS:
                byte[] ret = new byte[8];
                for (int i = 6; i < 12; i++) {
                    ret[i - 6] = response[i];
                }
                return ret;
            case ZmartModbusBindingClass.READ_COIL_STATUS:
            case ZmartModbusBindingClass.READ_INPUT_STATUS:
            case ZmartModbusBindingClass.READ_INPUT_REGS:
            case ZmartModbusBindingClass.READ_HOLDING_REGS:
                int byteCnt = (response[8] & 0xff) + 3 + 6;
                ret = new byte[byteCnt - 6];
                for (int i = 6; i < byteCnt; i++) {
                    ret[i - 6] = response[i];
                }
                return ret;
        }
        return null;
    }

    /**
     * Calculates and returns the next transaction index for ModbusFunction TCP.
     *
     * @return the next transaction index.
     */
    private int getNextTransactionIndex() {
        transactionIndex++;
        if (transactionIndex > 0xffff) {
            transactionIndex = 0;
        }
        return transactionIndex;
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

}
