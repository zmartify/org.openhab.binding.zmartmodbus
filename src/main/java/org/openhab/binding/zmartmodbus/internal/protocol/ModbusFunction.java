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

package org.openhab.binding.zmartmodbus.internal.protocol;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.openhab.core.thing.ThingUID;
import org.openhab.binding.zmartmodbus.ModbusBindingClass;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolErrorCode;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 *         Service providing a connection to a device via Serial link (RS232/RS485) or Ethernet using
 *         ModbusFunction protocol.
 *         This service implements a subset of ModbusFunction Application Protocol as defined by ModbusFunction
 *         Organization : http://www.modbus.org/specs.php.<br>
 *         For the moment in Ethernet mode, only RTU over TCP/IP is supported
 */

public class ModbusFunction {


    protected ModbusBridgeHandler bridgeHandler;

    public ModbusFunction() {
    }

    public void setBridgeHandler(ModbusBridgeHandler modbusBridgeHandler) {
        this.bridgeHandler = modbusBridgeHandler;
    }

    private BitVector readBitVector(byte functionCode, int unitAddr, int dataAddress, int offset, int count)
            throws ModbusProtocolException {
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        dataAddress = +offset;

        byte[] resp;
        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = functionCode;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (count / 256);
        cmd[5] = (byte) (count % 256);

        /*
         * send the message and get the response
         */
        resp = msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3 || resp.length < (resp[2] & 0xff) + 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        if ((resp[2] & 0xff) != ((count + 7) / 8)) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
        }

        return BitVector.createBitVector(Arrays.copyOfRange(resp, 3, resp.length), count);
    }

    /**
     * <b>ModbusFunction function 01</b><br>
     * Read 1 to 2000 contiguous status of coils from the attached field device.
     * <p>
     *
     * @param unitAddr
     *                        modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *                        starting address
     * @param offset
     *                        Offset to be added to address - used for custom code, just leave as 0 otherwise
     * @param count
     *                        quantity of coils
     * @return an array of booleans representing the requested data points.
     *         <b>true</b> for a given point if the point is set, <b>false</b>
     *         otherwise.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public BitVector readCoils(int unitAddr, int dataAddress, int offset, int count) throws ModbusProtocolException {
        return readBitVector((byte) ModbusBindingClass.READ_COIL_STATUS, unitAddr, dataAddress, offset, count);
    }

    /**
     * <b>ModbusFunction function 02</b><br>
     * Read 1 to 2000 contiguous status of discrete inputs from the attached field device.
     * <p>
     *
     * @param unitAddr
     *                        modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *                        starting address
     * @param offset
     *                        Offset to be added to address - used for customcode, just leave as 0 otherwise
     * @param count
     *                        quantity of inputs
     * @return an array of booleans representing the requested data points.
     *         <b>true</b> for a given point if the point is set, <b>false</b>
     *         otherwise.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public BitVector readDiscreteInputs(int unitAddr, int dataAddress, int offset, int count)
            throws ModbusProtocolException {
        return readBitVector((byte) ModbusBindingClass.READ_INPUT_STATUS, unitAddr, dataAddress, offset, count);
    }

    /**
     * <b>ModbusFunction function 07</b><br>
     * read the content of 8 Exception Status outputs in the field device.
     * <p>
     *
     * @param unitAddr
     *                     modbus slave address (must be unique in the range 1 - 247)
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public BitVector readExceptionStatus(int unitAddr) throws ModbusProtocolException {
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        byte[] resp;
        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[2];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusBindingClass.READ_EXCEPTION_STATUS;

        /*
         * send the message and get the response
         */
        resp = msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        return BitVector.createBitVector(Arrays.copyOfRange(resp, 2, 3), 8);
    }

    private byte[] readRegisters(byte functionCode, int unitAddr, int dataAddress, int count)
            throws ModbusProtocolException {
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        byte[] resp;
        /*
         * construct the command issue and get results, putting the results
         * away at index and then incrementing index for the next command
         */
        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = functionCode;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = 0;
        cmd[5] = (byte) count;

        /*
         * send the message and get the response
         */
        resp = msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3 || resp.length < (resp[2] & 0xff) + 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        if ((resp[2] & 0xff) == count * 2) {
            return Arrays.copyOfRange(resp, 3, 3 + count * 3);
        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
        }

    }

    /**
     * <b>ModbusFunction function 03</b><br>
     * Read contents of 1 to 125 contiguous block of holding registers from the attached field device.
     * <p>
     *
     * @param unitAddr
     *                        modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *                        starting address
     * @param count
     *                        quantity of registers (maximum 0x7D)
     * @return an array of int representing the requested data points (data registers on 2 bytes).
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public byte[] readHoldingRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
         return readRegisters((byte) ModbusBindingClass.READ_HOLDING_REGS, unitAddr, dataAddress, count);
    }

    /**
     * <b>ModbusFunction function 04</b><br>
     * Read contents of 1 to 125 contiguous block of input registers from the attached field device.
     * <p>
     *
     * @param unitAddr
     *                        modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *                        starting address
     * @param count
     *                        quantity of registers (maximum 0x7D)
     * @return an array of int representing the requested data points (data registers on 2 bytes).
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public byte[] readInputRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
        return readRegisters((byte) ModbusBindingClass.READ_INPUT_REGS, unitAddr, dataAddress, count);
    }

    /**
     * <b>ModbusFunction function 15 (0x0F)</b><br>
     * write multiple coils in a sequence of coils to either ON or OFF in the attached field device.
     * <p>
     *
     * @param unitAddr
     *                        modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *                        Starting Output address.
     * @param offset
     *                        Offset to be added to address - used for customcode, just leave as 0 otherwise
     * @param data
     *                        Outputs value (array of boolean) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public void writeMultipleCoils(int unitAddr, int dataAddress, int offset, BitVector data)
            throws ModbusProtocolException {
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        dataAddress = +offset;

        /*
         * write multiple boolean values
         */
        int localCnt = data.size();
        byte[] resp;
        /*
         * construct the command, issue and verify response
         */
        int dataLength = (localCnt + 7) / 8;
        byte[] cmd = new byte[dataLength + 7];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusBindingClass.FORCE_MULTIPLE_COILS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (localCnt / 256);
        cmd[5] = (byte) (localCnt % 256);
        cmd[6] = (byte) dataLength;

        // make sure the BitVector size and localCnt matches
        data.forceSize(localCnt);

        /*
         * send the message and get the response
         */
        resp = msgTransaction(ArrayUtils.addAll(cmd, data.getBytes()));

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        for (int j = 0; j < 6; j++) {
            if (cmd[j] != resp[j]) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }
        }
    }

    /**
     * <b>ModbusFunction function 16 (0x10)</b><br>
     * write a block of contiguous registers (1 to 123) in the attached field device.
     * <p>
     *
     * @param unitAddr
     *                        modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *                        Output address.
     * @param data
     *                        Registers value (array of int converted in 2 bytes values) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public void writeMultipleRegisters(int unitAddr, int dataAddress, byte[] data) throws ModbusProtocolException {
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        // Only write and even number of bytes (i.e. registers)
        int localCnt = (data.length / 2);
        /*
         * construct the command, issue and verify response
         */
        int dataLength = localCnt * 2;
        byte[] cmd = new byte[dataLength + 7];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusBindingClass.PRESET_MULTIPLE_REGS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (localCnt / 256);
        cmd[5] = (byte) (localCnt % 256);
        cmd[6] = (byte) dataLength;

        // put the data on the command
        int byteOffset = 7;
        for (int index = 0; index < data.length; index++) {
            cmd[byteOffset + index] = data[index];
        }

        /*
         * send the message and get the response
         */
        byte[] resp = msgTransaction(cmd);

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        for (int j = 0; j < 6; j++) {
            if (cmd[j] != resp[j]) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }
        }
    }

    /**
     * <b>ModbusFunction function 05</b><br>
     * write a single output to either ON or OFF in the attached field device.
     * <p>
     *
     * @param unitAddr
     *                        modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *                        Output address.
     * @param offset
     *                        Offset to be added to address - used for customcode, just leave as 0 otherwise
     * @param data
     *                        Output value (boolean) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public void writeSingleCoil(int unitAddr, int dataAddress, int offset, boolean state)
            throws ModbusProtocolException {
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        dataAddress = +offset;

        byte[] resp;

        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusBindingClass.FORCE_SINGLE_COIL;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = state == true ? (byte) 0xff : (byte) 0;
        cmd[5] = 0;

        /*
         * send the message and get the response
         */
        resp = msgTransaction(cmd);

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        for (int i = 0; i < 6; i++) {
            if (cmd[i] != resp[i]) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }
        }

    }

    /**
     * <b>ModbusFunction function 11 (0x0B)</b><br>
     * Get a status word and an event count from the device.<br>
     * Return values in a ModbusCommEvent.
     * <p>
     *
     * @param unitAddr
     *                     modbus slave address (must be unique in the range 1 - 247)
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     * @see ModbusCommEvent
     */
    public ModbusCommEvent getCommEventCounter(int unitAddr) throws ModbusProtocolException {
        ModbusCommEvent mce = new ModbusCommEvent();
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[2];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusBindingClass.GET_COMM_EVENT_COUNTER;

        /*
         * send the message and get the response
         */
        byte[] resp;
        resp = msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        int val = resp[2] & 0xff;
        val <<= 8;
        val += resp[3] & 0xff;
        mce.setStatus(val);
        val = resp[4] & 0xff;
        val <<= 8;
        val += resp[5] & 0xff;
        mce.setEventCount(val);

        return mce;
    }

    /**
     * <b>ModbusFunction function 12 (0x0C)</b><br>
     * Get a status word, an event count, a message count and a list of event bytes
     * from the device.<br>
     * Return values in a ModbusCommEvent.
     * <p>
     *
     * @param unitAddr
     *                     modbus slave address (must be unique in the range 1 - 247)
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     * @see ModbusCommEvent
     */
    public ModbusCommEvent getCommEventLog(int unitAddr) throws ModbusProtocolException {
        ModbusCommEvent mce = new ModbusCommEvent();
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[2];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusBindingClass.GET_COMM_EVENT_LOG;

        /*
         * send the message and get the response
         */
        byte[] resp;
        resp = msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < (resp[2] & 0xff) + 3 || (resp[2] & 0xff) > 64 + 7) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        int val = resp[3] & 0xff;
        val <<= 8;
        val += resp[4] & 0xff;
        mce.setStatus(val);

        val = resp[5] & 0xff;
        val <<= 8;
        val += resp[6] & 0xff;
        mce.setEventCount(val);

        val = resp[7] & 0xff;
        val <<= 8;
        val += resp[8] & 0xff;
        mce.setMessageCount(val);

        int count = (resp[2] & 0xff) - 4;
        int[] events = new int[count];
        for (int j = 0; j < count; j++) {
            int bval = resp[9 + j] & 0xff;
            events[j] = bval;
        }
        mce.setEvents(events);

        return mce;
    }

    /**
     * <b>ModbusFunction function 06</b><br>
     * write a single holding register in the attached field device.
     * <p>
     *
     * @param unitAddr
     *                        modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *                        Output address.
     * @param data
     *                        Output value (2 bytes) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *                                     current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTION_FAILURE}
     *                                     should include a protocol specific message to help clarify
     *                                     the cause of the exception
     */
    public void writeSingleRegister(int unitAddr, int dataAddress, byte[] data) throws ModbusProtocolException {
        if (!isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusBindingClass.PRESET_SINGLE_REG;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = data[0];
        cmd[5] = data[1];

        /*
         * send the message and get the response
         */
        byte[] resp = msgTransaction(cmd);

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        for (int i = 0; i < 6; i++) {
            if (cmd[i] != resp[i]) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }
        }
    }

    public void startSubDeviceDiscovery(ThingUID thingUID) {
        // Initiate auto discovery process
    }

    public boolean controllerOnline(int unitAddr) {

        if (!isConnected()) {
            return false;
        }

        try {
            readInputRegisters(unitAddr, 0, 0);
        } catch (ModbusProtocolException e) {
            // If we get an error, it's possible not connected
            return false;
        }

        // If we reach here we got a successful read
        return true;
    }

    public void setLogicalAddress(int unitAddr) {
        // Initiate auto setup of address
        // Currently only implemented for Jablotron AC-116
    }

    public void getDeviceInfo(ThingUID thingUID) {
        // 
    }

    /**
    *
    */
    public byte[] msgTransaction(byte[] msg) throws ModbusProtocolException {
        return bridgeHandler.getTransceiver().msgTransaction(msg, ModbusBindingConstants.CUSTOMCODE_STANDARD);
    }

    public byte[] msgTransaction(byte[] msg, int customCode) throws ModbusProtocolException {
        return bridgeHandler.getTransceiver().msgTransaction(msg, customCode);
    }

    public boolean isConnected() {
        return bridgeHandler.isConnected();
    }

    public ModbusCounters getCounters() {
        return bridgeHandler.getCounters();
    }
}
