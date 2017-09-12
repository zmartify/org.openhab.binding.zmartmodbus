package org.openhab.binding.zmartmodbus.internal.protocol;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants.CUSTOMCODE_JABLOTRON;
import static org.openhab.binding.zmartmodbus.internal.util.Register.*;

import java.util.Arrays;

import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusActionClass;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusMessageClass;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusDataSet;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
import org.openhab.binding.zmartmodbus.internal.util.Jablotron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGI service providing a connection to a device via Serial link (RS232/RS485) or Ethernet using ModbusFunction
 * protocol.
 * This service implements a subset of ModbusFunction Application Protocol as defined by ModbusFunction Organization :
 * http://www.modbus.org/specs.php.<br>
 * For the moment in Ethernet mode, only RTU over TCP/IP is supported
 * <p>
 * Function codes implemented are :
 * <ul>
 * <li>01 (0x01) readCoils(int dataAddress, int count) : Read 1 to 2000 max contiguous status of coils from the attached
 * field device.
 * It returns an array of booleans representing the requested data points.
 * <li>02 (0x02) readDiscreteInputs(int dataAddress, int count) : Read 1 to 2000 max contiguous status of discrete
 * inputs
 * from the attached field device. It returns an array of booleans representing the requested data points.
 * <li>03 (0x03) readHoldingRegisters(int dataAddress, int count) : Read contents of 1 to 125 max contiguous block of
 * holding
 * registers from the attached field device. It returns an array of int representing the requested data points
 * (data registers on 2 bytes).
 * <li>04 (0x04) readInputRegisters(int dataAddress, int count) : Read contents of 1 to 125 max contiguous block of
 * input registers
 * from the attached field device. It returns an array of int representing the requested data points (data registers on
 * 2 bytes).
 * <li>05 (0x05) writeSingleCoil(int dataAddress, boolean data) : Write a single output to either ON or OFF in the
 * attached field
 * device.
 * <li>06 (0x06) writeSingleRegister(int dataAddress, int data) : write a single holding register in the attached field
 * device.
 * <li>07 (0x07) readExceptionStatus() : read the content of 8 Exception Status outputs in the field
 * device.
 * <li>11 (0x0B) getCommEventCounter() : Get a status word and an event count from the field
 * device.
 * <li>12 (0x0C) getCommEventLog() : Get a status word, an event count, a message count and a list of event bytes from
 * the field
 * device.
 * <li>15 (0x0F) writeMultipleCoils(int dataAddress, boolean[] data) : Write multiple coils in a sequence of coils to
 * either
 * ON or OFF in the attached field device.
 * <li>16 (0x10) writeMultipleRegister(int dataAddress, int[] data) : write a block of contiguous registers (1 to 123)
 * in the attached
 * field device.
 * </ul>
 */

public class ModbusFunctionJablotron extends ModbusFunction {

    private Logger logger = LoggerFactory.getLogger(ModbusFunctionJablotron.class);

    public ModbusFunctionJablotron(ModbusIoHandler bridgeHandler) {
        super(bridgeHandler);
    }

    @Override
    public BitVector readCoils(int unitAddr, int dataAddress, int offset, int count) throws ModbusProtocolException {
        BitVector ret = new BitVector(count + offset);
        int length = ret.regSize();

        byte[] resp = jablotronReadRegisterFromIndex(unitAddr, dataAddress, length);

        ret = BitVector.createBitVectorSwap(resp, count + offset);

        return ret.rangeOf(offset, count);
    }

    @Override
    public BitVector readDiscreteInputs(int unitAddr, int dataAddress, int offset, int count)
            throws ModbusProtocolException {
        BitVector ret = new BitVector(count + offset);
        int length = ret.regSize();

        byte[] resp = jablotronReadRegisterFromIndex(unitAddr, dataAddress, length);

        ret = BitVector.createBitVectorSwap(resp, count + offset);

        return ret.rangeOf(offset, count);
    }

    @Override
    public byte[] readHoldingRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
        return jablotronReadRegisterFromIndex(unitAddr, dataAddress, count);
    }

    @Override
    public byte[] readInputRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
        return jablotronReadRegisterFromIndex(unitAddr, dataAddress, count);
    }

    @Override
    public void writeMultipleCoils(int unitAddr, int dataAddress, int offset, BitVector data)
            throws ModbusProtocolException {
        BitVector dataAdjusted = new BitVector(data.size() + offset);
        BitVector dataMask = BitVector.createBitVector((byte) 0xFF, data.size() + offset);

        if (offset > 0) {
            dataAdjusted.add(new BitVector(offset), data);
        } else {
            dataAdjusted = data;
        }

        for (int i = offset; i < dataAdjusted.size(); i++) {
            dataMask.setBit(i, false);
        }

        jablotronWriteRegisterMaskedToIndex(unitAddr, dataAddress, dataAdjusted, dataMask);
    }

    @Override
    public void writeSingleCoil(int unitAddr, int dataAddress, int index, boolean state)
            throws ModbusProtocolException {

        BitVector data = new BitVector(16);
        BitVector mask = BitVector.createBitVector((byte) 0xFF, 16);
        data.setBit(index, state);
        mask.setBit(index, false);

        jablotronWriteRegisterMaskedToIndex(unitAddr, dataAddress, data, mask);
    }

    private byte[] jablotronReadRegisterFromIndex(int unitAddr, int dataAddress, int count)
            throws ModbusProtocolException {

        if (!bridgeHandler.isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        byte[] resp;
        /*
         * construct the command issue and get results, putting the results
         * away at index and then incrementing index for the next command
         */
        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) (ZmartModbusBindingClass.READ_REGISTER_FROM_INDEX & 0xFF); // Strip Jablotron special function
        // high
        // byte
        cmd[2] = Jablotron.getCategory(dataAddress);
        cmd[3] = Jablotron.getIndex(dataAddress);
        cmd[4] = Jablotron.getPage(dataAddress);
        cmd[5] = (byte) count;

        /*
         * send the message and get the response
         */
        resp = bridgeHandler.msgTransaction(cmd, CUSTOMCODE_JABLOTRON);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3 || resp.length < (resp[2] & 0xff) + 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        if ((resp[2] & 0xff) == count * 2) {
            return Arrays.copyOfRange(resp, 3, 3 + count * 2);
        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
        }
    }

    private void jablotronWriteRegisterToIndex(int unitAddr, int dataAddress, byte[] data)
            throws ModbusProtocolException {
        if (!bridgeHandler.isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        int localCnt = data.length / 2;
        int dataLength = data.length;

        /*
         * construct the command, issue and verify response
         */
        byte[] cmd = new byte[dataLength + 6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ZmartModbusBindingClass.WRITE_REGISTER_TO_INDEX & 0xFF;
        cmd[2] = Jablotron.getCategory(dataAddress);
        cmd[3] = Jablotron.getIndex(dataAddress);
        cmd[4] = Jablotron.getPage(dataAddress);
        cmd[5] = (byte) (localCnt & 0xff);

        // put the data on the command
        for (int index = 0; index < data.length; index++) {
            cmd[6 + index] = data[index];
        }

        /*
         * send the message and get the response
         */
        byte[] resp = bridgeHandler.msgTransaction(cmd, CUSTOMCODE_JABLOTRON);

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }

        if ((cmd[0] != resp[0]) || // SlaveId
                (cmd[1] != resp[1]) || // Function Code test
                (cmd[2] != dataLength)) // Byte Count test
        {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
    }

    private void jablotronWriteRegisterMaskedToIndex(int unitAddr, int dataAddress, BitVector data, BitVector mask)
            throws ModbusProtocolException {
        if (!bridgeHandler.isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }
        if (data.size() != mask.size()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }

        // Make sure we have an even number of bytes
        int forceSize = ((data.size() + 15) / 16) * 16;

        data.forceSize(forceSize);
        mask.forceSize(forceSize);

        /*
         * construct the command, issue and verify response
         */

        byte[] cmd = new byte[data.byteSize() * 2 + 6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ZmartModbusBindingClass.WRITE_REGISTER_MASKED_TO_INDEX & 0x00FF;
        cmd[2] = Jablotron.getCategory(dataAddress);
        cmd[3] = Jablotron.getIndex(dataAddress);
        cmd[4] = Jablotron.getPage(dataAddress);
        cmd[5] = (byte) data.regSize();
        for (int i = 0; i < data.regSize(); i++) {
            int index2 = i * 2;
            int index4 = i * 4;
            cmd[index4 + 6] = data.getByte(index2 + 1);
            cmd[index4 + 7] = data.getByte(index2);
            cmd[index4 + 8] = mask.getByte(index2 + 1);
            cmd[index4 + 9] = mask.getByte(index2);
        }

        /*
         * send the message and get the response
         */
        byte[] resp = bridgeHandler.msgTransaction(cmd, CUSTOMCODE_JABLOTRON);

        /*
         * process the response
         */
        if (resp.length < 4) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }

        // Test for correct unitAddress, FunctionCode and dataLength
        if ((cmd[0] != resp[0]) || (cmd[1] != resp[1]) || (resp[2] != data.byteSize())) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
    }

    /**
     * <b>ModbusFunction function 06</b><br>
     * write a single holding register in the attached field device.
     * <p>
     *
     * @param unitAddr
     *            modbus slave address (must be unique in the range 1 - 247)
     * @param dataAddress
     *            Output address.
     * @param data
     *            Output value (2 bytes) to write.
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#NOT_CONNECTED}
     *             current connection is in a status other than <b>CONNECTED</b>
     * @throws ModbusProtocolException with a {@link ModbusProtocolErrorCode#TRANSACTion_FAILURE}
     *             should include a protocol specific message to help clarify
     *             the cause of the exception
     */
    @Override
    public void writeSingleRegister(int unitAddr, int dataAddress, byte[] data) throws ModbusProtocolException {
        jablotronWriteRegisterToIndex(unitAddr, dataAddress, data);
    }

    @Override
    public void writeMultipleRegisters(int unitAddr, int dataAddress, byte[] data) throws ModbusProtocolException {
        jablotronWriteRegisterToIndex(unitAddr, dataAddress, data);
    }

    @Override
    public void startSubDeviceDiscovery(int nodeId) {
        for (int elementId = 0; elementId < 48; elementId++) {
            // Add channel for the element index
            ModbusDataSet dataSet = new ModbusDataSet(nodeId, ModbusMessageClass.Holding,
                    Jablotron.getAddress(0x01, 0, elementId), 0x0C, 0, ModbusReportOn.Allways, ModbusFeedRepeat.Once);
            dataSet.setInternal(true);
            String dataSetKey = String.format("%d-%d-discovery1", nodeId, elementId);
            bridgeHandler.getController().getModbusFactory().addDataSet(dataSetKey, dataSet);
            bridgeHandler.getController().getActionFeed().addAction(new ModbusAction(dataSet, ModbusActionClass.Read));
        }
    }

    private byte[] enumeration(int unitAddr, byte[] physicalAddress) throws ModbusProtocolException {
        if (!bridgeHandler.isConnected()) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        /*
         * construct the command, issue and verify response
         */

        byte[] cmd = new byte[7];
        cmd[0] = (byte) 0x01;
        cmd[1] = (byte) ZmartModbusBindingClass.ENUMERATION & 0x00FF;

        // Physical Address
        cmd[2] = physicalAddress[0];
        cmd[3] = physicalAddress[1];
        cmd[4] = physicalAddress[2];
        cmd[5] = physicalAddress[3];

        // Target Logical Address
        cmd[6] = (byte) unitAddr;

        logger.debug("Enumeration command: {}", toHex(cmd));
        /*
         * send the message and get the response
         */
        byte[] resp = bridgeHandler.msgTransaction(cmd, CUSTOMCODE_JABLOTRON);

        logger.debug("We received {}", toHex(resp));

        if (unitAddr != 0) {
            /*
             * It's not a RESET, expect response
             */
            byte[] data = new byte[resp.length - 3];
            /*
             * process the response
             */
            if (resp.length < 7) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }

            // Test for correct unitAddress, FunctionCode and dataLength
            if ((cmd[0] != resp[0]) || (cmd[1] != resp[1])) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }

            return Arrays.copyOf(data, 4);
        }
        // If we end here it's an Enumeration RESET command, without response
        return null;
    }

    /**
     * Set Logical Address of All Units
     *
     * @throws ModbusProtocolException
     */
    @Override
    public void setLogicalAddress(int unitAddress) {

        // Reset Logical Address of All Units
        try {
            enumeration(0, intToRegisters(0));
        } catch (ModbusProtocolException e) {
            switch (e.getCode()) {
                case RESPONSE_TIMEOUT:
                    // That was expected
                    break;
                default:
                    logger.error("enumeration RESET got an error {} {} {} {}", e.getCode(), e.getMessage(),
                            e.getClass(), e.getCause());
                    break;
            }
        }

        int counter = 0;
        try {
            byte[] physicalAddress = new byte[4];
            do {
                physicalAddress = enumeration(1, intToRegisters(0));
                enumeration(unitAddress, physicalAddress);
                counter++;
            } while (true); // We continue until we receive a time out error
        } catch (ModbusProtocolException e) {
            switch (e.getCode()) {
                case RESPONSE_TIMEOUT:
                    // That was expected
                    break;
                default:
                    logger.error("setLogicalAddres got an error {} {}", e.getCode().getClass(), e.getMessage());
                    break;
            }
            logger.debug("Enumeration process completed - ({}) units got new addresses starting from {}", counter,
                    unitAddress);
        }
        logger.info("WE ARRIVED HERE");
    }

    @Override
    public boolean controllerOnline(int unitAddr) {

        if (!bridgeHandler.isConnected()) {
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

}
