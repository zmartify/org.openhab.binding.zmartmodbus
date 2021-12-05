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
package org.openhab.binding.zmartmodbus.internal.transceiver;

import java.io.IOException;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.binding.zmartmodbus.ModbusBindingClass;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.config.ModbusSerialConfiguration;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolErrorCode;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusCounters;
import org.openhab.binding.zmartmodbus.internal.util.Crc16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Daniel Weber - Initial contribution
 * @author Peter Kristensen - Adopted for ZmartModbus
 * 
 */

public class ModbusSerialTransceiver extends ModbusTransceiver {

    private final Logger logger = LoggerFactory.getLogger(ModbusSerialTransceiver.class);

    private @Nullable SerialPortManager serialPortManager;
    private @Nullable SerialPort serialPort;

    private static int SLEEP_WHEN_NO_INPUT = 50; // Thread sleeps when input stream is empty

    protected ModbusSerialConfiguration serialConfig;

    public ModbusSerialTransceiver(final SerialPortManager serialPortManager,
            final ModbusSerialConfiguration serialConfig, final ModbusCounters counters) {
        super(counters);
        this.serialPortManager = serialPortManager;
        this.serialConfig = serialConfig;
        this.serialPort = null;
    }

    @Override
    public void connect() throws ModbusProtocolException {
        // Call disconnect to ensure we are not connected
        if (serialPort != null)
            disconnect();

        try {
            final String port = serialConfig.getPort();
            logger.debug("Connecting to serial port '{}'", port);

            @Nullable final SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(port);

                if (portIdentifier == null) {
                    throw new ModbusProtocolException(
                            "Could not find a gateway on given path '" + port + "', "
                                    + serialPortManager.getIdentifiers().count() + " ports available.",
                            ModbusProtocolErrorCode.NOT_AVAILABLE);
                }


            serialPort = portIdentifier.open(ModbusBindingConstants.BINDING_ID, 2000);

            logger.debug("SetSerial");
            serialPort.setSerialPortParams(serialConfig.getBaud(), serialConfig.getDataBits(),
                    serialConfig.getStopBits(), serialConfig.getParity());

            serialPort.enableReceiveThreshold(1);

            // In ms. small values mean faster shutdown but more CPU usage.
            serialPort.enableReceiveTimeout(serialConfig.getReceiveTimeoutMillis());

            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();

            setConnected(true);
        } catch (final PortInUseException e) {
            logger.error("PortInUse");
            if (serialPort != null) {
                logger.debug("Trying to close the port");
                serialPort.close();
            }
            throw new ModbusProtocolException(ModbusProtocolErrorCode.SERIAL_INUSE);
        } catch (final UnsupportedCommOperationException e) {
            logger.error("Unsupported");
            throw new ModbusProtocolException(ModbusProtocolErrorCode.SERIAL_UNSUPPORTED);
        } catch (final IOException e) {
            logger.error("IOException {}", e.getMessage());
            throw new ModbusProtocolException(e.getMessage(), e.getCause(), ModbusProtocolErrorCode.CONNECTION_FAILURE);
        }

        logger.info("ModbusSerialTransceiver initialized");
    }

    @Override
    public void disconnect() {

        logger.debug("Shutting down transceiver");

        if (outputStream != null) {
            logger.debug("Closing serial output stream");
            try {
                outputStream.close();
            } catch (IOException e) {
                logger.warn("Error while closing the output stream: {}", e.getMessage());
            }
            outputStream = null;
        }

        if (inputStream != null) {
            logger.debug("Closing serial input stream");
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.warn("Error while closing the input stream: {}", e.getMessage());
            }
            inputStream = null;
        }

        if (serialPort != null) {
            logger.debug("Closing serial port");
            serialPort.close();
        }

        serialPort = null;
        outputStream = null;
        inputStream = null;

        setConnected(false);

        logger.info("Transceiver shutdown");
    }

    /**
     * msgTransaction must be called with a byte array having two extra bytes for
     * the CRC. It will return a byte array of the response to the message.
     * Validation will include checking the CRC and verifying the command matches.
     */

    @Override
    public byte[] msgTransaction(final byte[] msg, final int customCode) throws ModbusProtocolException {
        byte[] cmd = null;

        // Update message counter
        counters.incrementMessageCounter();

        if (serialConfig.getTxMode() == ModbusBindingConstants.RTU_MODE) {
            cmd = new byte[msg.length + 2];
            for (int i = 0; i < msg.length; i++) {
                cmd[i] = msg[i];
            }
            // Add crc calculation to end of message
            final int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
            cmd[msg.length] = (byte) crc;
            cmd[msg.length + 1] = (byte) (crc >> 8);
        } else if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE) {
            cmd = convertCommandToAscii(msg);
        }

        logger.trace("MODBUS send ({}): {}", counters.getMessageCounter(), DatatypeConverter.printHexBinary(cmd));
        // Send the message
        try {
            //
            try {
                Thread.sleep(serialConfig.getTimeBetweenTransactionsMillis()); // ensure delay between polling
            } catch (final InterruptedException e) {
                throw new ModbusProtocolException("Thread interrupted", ModbusProtocolErrorCode.TRANSACTION_FAILURE);
            }

            synchronized (outputStream) {
                synchronized (inputStream) {
                    // flush input
                    while (inputStream.available() > 0) {
                        inputStream.read();
                    }
                    // send all data
                    outputStream.write(cmd, 0, cmd.length);
                    outputStream.flush();
                    // outputStream.waitAllSent(respTout);

                    // wait for and process response
                    byte[] response = new byte[262]; // response buffer
                    int respIndex = 0;
                    int minimumLength = 5; // default minimum message length
                    if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE) {
                        minimumLength = 11;
                    }
                    int timeOut = serialConfig.getConnectTimeoutMillis();

                    for (int maxLoop = 0; maxLoop < 1000; maxLoop++) {
                        boolean endFrame = false;
                        // while (respIndex < minimumLength) {
                        while (!endFrame) {
                            final long start = System.currentTimeMillis();
                            while (inputStream.available() == 0) {
                                try {
                                    Thread.sleep(SLEEP_WHEN_NO_INPUT); // avoid a high cpu load
                                } catch (final InterruptedException e) {
                                    throw new ModbusProtocolException("Thread interrupted",
                                            ModbusProtocolErrorCode.TRANSACTION_FAILURE);
                                }

                                final long elapsed = System.currentTimeMillis() - start;
                                if (elapsed > timeOut) {

                                    final String failMsg = String.format(
                                            "Recv timeout %d : minLength=%d respIndex=%d #%d cmd=%s", elapsed,
                                            minimumLength, respIndex, counters.getMessageCounter(),
                                            DatatypeConverter.printHexBinary(cmd));

                                    // Increase Response Time Out counter
                                    counters.incrementTimeOutCounter();
                                    throw new ModbusProtocolException(failMsg,
                                            ModbusProtocolErrorCode.RESPONSE_TIMEOUT);
                                }
                            }
                            // address byte must match first
                            if (respIndex == 0) {
                                if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE) {
                                    if ((response[0] = (byte) inputStream.read()) == ':') {
                                        respIndex++;
                                    }
                                } else {
                                    if ((response[0] = (byte) inputStream.read()) == msg[0]) {
                                        respIndex++;
                                    }
                                }
                            } else {
                                response[respIndex++] = (byte) inputStream.read();
                            }

                            if (serialConfig.getTxMode() == ModbusBindingConstants.RTU_MODE) {
                                timeOut = 100; // move to character timeout
                                if (respIndex >= minimumLength) {
                                    endFrame = true;
                                }
                            } else {
                                if (response[respIndex - 1] == 10 && response[respIndex - 2] == 13) {
                                    endFrame = true;
                                }
                            }
                        }

                        // if ASCII mode convert response
                        if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE) {
                            final byte lrcRec = asciiLrcCalc(response, respIndex);
                            response = convertAsciiResponseToBin(response, respIndex);
                            final byte lrcCalc = (byte) binLrcCalc(response);
                            if (lrcRec != lrcCalc) {
                                throw new ModbusProtocolException("Bad LRC",
                                        ModbusProtocolErrorCode.TRANSACTION_FAILURE);
                            }
                        }

                        // Check first for an Exception response
                        if ((response[1] & 0x80) == 0x80) {
                            if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE
                                    || Crc16.getCrc16(response, 5, 0xffff) == 0) {
                                throw new ModbusProtocolException("Exception response = " + Byte.toString(response[2]),
                                        ModbusProtocolErrorCode.TRANSACTION_FAILURE);
                            }
                        } else {
                            // then check for a valid message
                            // add customCode to high byte to separate custom modbus functions
                            int byteCnt;
                            switch (response[1] | customCode) {
                            case ModbusBindingClass.ENUMERATION:
                                if (respIndex < 6) {
                                    // wait for more data
                                    minimumLength = 6;
                                } else if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE
                                        || Crc16.getCrc16(response, 8, 0xffff) == 0) {
                                    final byte[] ret = new byte[6];
                                    for (int i = 0; i < 6; i++) {
                                        ret[i] = response[i];
                                    }
                                    return ret;
                                }
                                break;
                            case ModbusBindingClass.FORCE_SINGLE_COIL:
                            case ModbusBindingClass.PRESET_SINGLE_REG:
                            case ModbusBindingClass.FORCE_MULTIPLE_COILS:
                            case ModbusBindingClass.PRESET_MULTIPLE_REGS:
                                if (respIndex < 8) {
                                    // wait for more data
                                    minimumLength = 8;
                                } else if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE
                                        || Crc16.getCrc16(response, 8, 0xffff) == 0) {
                                    final byte[] ret = new byte[6];
                                    for (int i = 0; i < 6; i++) {
                                        ret[i] = response[i];
                                    }
                                    return ret;
                                }
                                break;
                            case ModbusBindingClass.READ_COIL_STATUS:
                            case ModbusBindingClass.READ_INPUT_STATUS:
                            case ModbusBindingClass.READ_INPUT_REGS:
                            case ModbusBindingClass.READ_HOLDING_REGS:
                            case ModbusBindingClass.READ_REGISTER_FROM_INDEX:
                            case ModbusBindingClass.WRITE_REGISTER_TO_INDEX:
                            case ModbusBindingClass.WRITE_REGISTER_MASKED_TO_INDEX:
                                if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE) {
                                    byteCnt = (response[2] & 0xff) + 3;
                                } else {
                                    byteCnt = (response[2] & 0xff) + 5;
                                }
                                if (respIndex < byteCnt) {
                                    // wait for more data
                                    minimumLength = byteCnt;
                                } else if (serialConfig.getTxMode() == ModbusBindingConstants.ASCII_MODE
                                        || Crc16.getCrc16(response, byteCnt, 0xffff) == 0) {
                                    final byte[] ret = new byte[byteCnt];
                                    for (int i = 0; i < byteCnt; i++) {
                                        ret[i] = response[i];
                                    }
                                    logger.trace("MODBUS receive: {}", DatatypeConverter.printHexBinary(ret));
                                    return ret;
                                }
                                break;
                            }
                        }

                        /*
                         * if required length then must have failed, drop first byte and try again
                         */
                        if (respIndex >= minimumLength) {
                            respIndex--;
                            for (int i = 0; i < respIndex; i++) {
                                response[i] = response[i + 1];
                            }
                            minimumLength = 5; // reset minimum length
                        }
                    }
                }
            }
        } catch (final IOException e) {
            // e.printStackTrace();
            throw new ModbusProtocolException(e.getMessage(), ModbusProtocolErrorCode.TRANSACTION_FAILURE);
        }
        throw new ModbusProtocolException("Too much activity on recv line",
                ModbusProtocolErrorCode.TRANSACTION_FAILURE);
    }

    public ModbusSerialConfiguration getSerialConfig() {
        return serialConfig;
    }

    public void setSerialConfig(final ModbusSerialConfiguration serialConfig) {
        this.serialConfig = serialConfig;
    }

    public SerialPortManager getSerialPortManager() {
        return serialPortManager;
    }

    public void setSerialPortManager(final SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

}
