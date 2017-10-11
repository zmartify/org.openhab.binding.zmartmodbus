
package org.openhab.binding.zmartmodbus.handler;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusProtocolErrorCode;
import org.openhab.binding.zmartmodbus.internal.util.Crc16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * The {@link ModbusSerialHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Chris Jackson - Initial contribution
 */
public class ModbusSerialHandler extends ZmartModbusHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusSerialHandler.class);

    /**
     * Constants for managing the ModbusFunction protocol
     */
    static final String PROTOCOL_NAME = "modbus";
    ThingTypeUID thingTypeUID = BRIDGE_SERIAL;

    public String port = "";
    private int baudrate = 38400;
    private int databits = SerialPort.DATABITS_8;
    private int stopbits = SerialPort.STOPBITS_1;
    private int parity = SerialPort.PARITY_NONE;

    private InputStream in = null;
    private OutputStream out = null;
    private SerialPort serialPort = null;

    public ModbusSerialHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.info("Initializing Zmartify ModbusFunction Serial Controller.");

        super.initialize();

        // Serial port
        port = getConfigParamStr(CONFIGURATION_PORT, "/dev/ttyS0");
        baudrate = getConfigParamInt(CONFIGURATION_BAUDRATE, DEFAULT_BAUDRATE);
        databits = getConfigParamInt(CONFIGURATION_DATABITS, DEFAULT_DATABITS);
        stopbits = getConfigParamInt(CONFIGURATION_STOPBITS, DEFAULT_STOPBITS);
        parity = getConfigParamInt(CONFIGURATION_PARITY, DEFAULT_PARITY);
        slowPoll = getConfigParamInt(CONFIGURATION_SLOWPOLL, DEFAULT_POLLS);
        fastPoll = getConfigParamInt(CONFIGURATION_FASTPOLL, DEFAULT_POLLS);

        initializeNetwork();
    }

    @Override
    public void dispose() {
        setListening(false);
        while (isConnected()) {
            // Wait for slavelistener to gracefully shut down
            logger.info("Waiting for slave to shutdown..");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                logger.error("Error while waiting for slave to shutdown {}", e.getMessage());
            }
            disconnect();
        }
        logger.info("Disconnected and ready to continue dispose");
        super.dispose();
    }

    @Override
    public void connect() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException,
            TooManyListenersException, IOException {
        try {
            logger.info("Trying to connect to serial port {}", port);
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);

            CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

            serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
            serialPort.enableReceiveThreshold(1);
            serialPort.disableReceiveTimeout();

            in = serialPort.getInputStream();
            out = serialPort.getOutputStream();

            out.flush();
            if (in.markSupported()) {
                in.reset();
            }
            setConnected(true);
            updateStatus(ThingStatus.ONLINE);
        } catch (NoSuchPortException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    String.format("Offline - serial port '%s' does not exist", port));
        } catch (PortInUseException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    String.format("Offline - serial port %s in use", port));
        } catch (UnsupportedCommOperationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                    String.format("Offline - serial port '%s' is not supported", port));
        } catch (Exception e) {
            logger.error("Unknown communication error {}", e.getMessage());
        }
        logger.info("Connected to serial port {}", port);
    }

    @Override
    public void disconnect() {
        logger.info("Disconnecting serial port");

        // set Connected flag = false - stopping the listener and aborting further commands
        //
        setConnected(false);

        if (serialPort != null) {
            serialPort.removeEventListener();
            logger.info("Serial port event listener stopped");
        }

        if (out != null) {
            logger.info("Close serial out stream");
            IOUtils.closeQuietly(out);
        }
        if (in != null) {
            logger.info("Close serial in stream");
            IOUtils.closeQuietly(in);
        }

        if (serialPort != null) {
            logger.info("Close serial port");
            serialPort.close();
        }

        serialPort = null;
        out = null;
        in = null;

        logger.info("Serial port closed");
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    private byte asciiLrcCalc(byte[] msg, int len) {
        char[] ac = new char[2];
        ac[0] = (char) msg[len - 4];
        ac[1] = (char) msg[len - 3];
        String s = new String(ac);
        byte lrc = (byte) Integer.parseInt(s, 16);
        return lrc;
    }

    private int binLrcCalc(byte[] msg) {
        int llrc = 0;
        for (byte element : msg) {
            llrc += element & 0xff;
        }
        llrc = (llrc ^ 0xff) + 1;
        // byte lrc=(byte)(llrc & 0x0ff);
        return llrc;
    }

    /**
     * convertCommandToAscii: convert a binary command into a standard ModbusFunction
     * ASCII frame
     */
    private byte[] convertCommandToAscii(byte[] msg) {
        int lrc = binLrcCalc(msg);

        char[] hexArray = "0123456789ABCDEF".toCharArray();
        byte[] ab = new byte[msg.length * 2 + 5];
        ab[0] = ':';
        int v;
        for (int i = 0; i < msg.length; i++) {
            v = msg[i] & 0xff;
            ab[i * 2 + 1] = (byte) hexArray[v >>> 4];
            ab[i * 2 + 2] = (byte) hexArray[v & 0x0f];
        }
        v = lrc & 0x0ff;
        ab[ab.length - 4] = (byte) hexArray[v >>> 4];
        ab[ab.length - 3] = (byte) hexArray[v & 0x0f];
        ab[ab.length - 2] = 13;
        ab[ab.length - 1] = 10;
        return ab;
    }

    /**
     * convertAsciiResponseToBin: convert a standard ModbusFunction frame to
     * byte array
     */
    private byte[] convertAsciiResponseToBin(byte[] msg, int len) {
        int l = (len - 5) / 2;
        byte[] ab = new byte[l];
        char[] ac = new char[2];
        // String s=new String(msg);
        for (int i = 0; i < l; i++) {
            ac[0] = (char) msg[i * 2 + 1];
            ac[1] = (char) msg[i * 2 + 2];
            // String s=new String(ac);
            ab[i] = (byte) Integer.parseInt(new String(ac), 16);
        }
        return ab;
    }

    /**
     * msgTransaction must be called with a byte array having two extra
     * bytes for the CRC. It will return a byte array of the response to the
     * message. Validation will include checking the CRC and verifying the
     * command matches.
     */
    @SuppressWarnings("null")
    @Override
    public byte[] msgTransaction(byte[] msg, int customCode) throws ModbusProtocolException {
        byte[] cmd = null;

        // Update message counter
        increaseMsgCounter();

        if (txMode == RTU_MODE) {
            cmd = new byte[msg.length + 2];
            for (int i = 0; i < msg.length; i++) {
                cmd[i] = msg[i];
            }
            // Add crc calculation to end of message
            int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
            cmd[msg.length] = (byte) crc;
            cmd[msg.length + 1] = (byte) (crc >> 8);
        } else if (txMode == ASCII_MODE) {
            cmd = convertCommandToAscii(msg);
        }

        // logger.info("MODBUS send : {}", DatatypeConverter.printHexBinary(cmd));
        // Send the message
        try {
            //
            try {
                Thread.sleep(delayBetweenMessages); // ensure delay between polling
            } catch (InterruptedException e) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, "Thread interrupted");
            }

            synchronized (this.out) {
                synchronized (this.in) {
                    // flush input
                    while (this.in.available() > 0) {
                        this.in.read();
                    }
                    // send all data
                    this.out.write(cmd, 0, cmd.length);
                    this.out.flush();
                    // outputStream.waitAllSent(respTout);

                    // wait for and process response
                    byte[] response = new byte[262]; // response buffer
                    int respIndex = 0;
                    int minimumLength = 5; // default minimum message length
                    if (txMode == ASCII_MODE) {
                        minimumLength = 11;
                    }
                    int timeOut = respTout;
                    for (int maxLoop = 0; maxLoop < 1000; maxLoop++) {
                        boolean endFrame = false;
                        // while (respIndex < minimumLength) {
                        while (!endFrame) {
                            long start = System.currentTimeMillis();
                            while (this.in.available() == 0) {
                                try {
                                    Thread.sleep(5); // avoid a high cpu load
                                } catch (InterruptedException e) {
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                            "Thread interrupted");
                                }

                                long elapsed = System.currentTimeMillis() - start;
                                if (elapsed > timeOut) {
                                    String failMsg = "Recv timeout";

                                    logger.warn("{} : {} minimumLength={} respIndex={} {}", failMsg, elapsed,
                                            minimumLength, respIndex);

                                    // Increase Response Time Out counter
                                    increaseRespToutCounter();
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.RESPONSE_TIMEOUT,
                                            failMsg);
                                }
                            }
                            // address byte must match first
                            if (respIndex == 0) {
                                if (txMode == ASCII_MODE) {
                                    if ((response[0] = (byte) this.in.read()) == ':') {
                                        respIndex++;
                                    }
                                } else {
                                    if ((response[0] = (byte) this.in.read()) == msg[0]) {
                                        respIndex++;
                                    }
                                }
                            } else {
                                response[respIndex++] = (byte) this.in.read();
                            }

                            if (txMode == RTU_MODE) {
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
                        if (txMode == ASCII_MODE) {
                            byte lrcRec = asciiLrcCalc(response, respIndex);
                            response = convertAsciiResponseToBin(response, respIndex);
                            byte lrcCalc = (byte) binLrcCalc(response);
                            if (lrcRec != lrcCalc) {
                                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                        "Bad LRC");
                            }
                        }

                        // Check first for an Exception response
                        if ((response[1] & 0x80) == 0x80) {
                            if (txMode == ASCII_MODE || Crc16.getCrc16(response, 5, 0xffff) == 0) {
                                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                        "Exception response = " + Byte.toString(response[2]));
                            }
                        } else {
                            // then check for a valid message
                            // add customCode to high byte to separate custom modbus functions
                            int byteCnt;
                            switch (response[1] | customCode) {
                                case ZmartModbusBindingClass.ENUMERATION:
                                    if (respIndex < 6) {
                                        // wait for more data
                                        minimumLength = 6;
                                    } else if (txMode == ASCII_MODE || Crc16.getCrc16(response, 8, 0xffff) == 0) {
                                        byte[] ret = new byte[6];
                                        for (int i = 0; i < 6; i++) {
                                            ret[i] = response[i];
                                        }
                                        return ret;
                                    }
                                    break;
                                case ZmartModbusBindingClass.FORCE_SINGLE_COIL:
                                case ZmartModbusBindingClass.PRESET_SINGLE_REG:
                                case ZmartModbusBindingClass.FORCE_MULTIPLE_COILS:
                                case ZmartModbusBindingClass.PRESET_MULTIPLE_REGS:
                                    if (respIndex < 8) {
                                        // wait for more data
                                        minimumLength = 8;
                                    } else if (txMode == ASCII_MODE || Crc16.getCrc16(response, 8, 0xffff) == 0) {
                                        byte[] ret = new byte[6];
                                        for (int i = 0; i < 6; i++) {
                                            ret[i] = response[i];
                                        }
                                        return ret;
                                    }
                                    break;
                                case ZmartModbusBindingClass.READ_COIL_STATUS:
                                case ZmartModbusBindingClass.READ_INPUT_STATUS:
                                case ZmartModbusBindingClass.READ_INPUT_REGS:
                                case ZmartModbusBindingClass.READ_HOLDING_REGS:
                                case ZmartModbusBindingClass.READ_REGISTER_FROM_INDEX:
                                case ZmartModbusBindingClass.WRITE_REGISTER_TO_INDEX:
                                case ZmartModbusBindingClass.WRITE_REGISTER_MASKED_TO_INDEX:
                                    if (txMode == ASCII_MODE) {
                                        byteCnt = (response[2] & 0xff) + 3;
                                    } else {
                                        byteCnt = (response[2] & 0xff) + 5;
                                    }
                                    if (respIndex < byteCnt) {
                                        // wait for more data
                                        minimumLength = byteCnt;
                                    } else if (txMode == ASCII_MODE || Crc16.getCrc16(response, byteCnt, 0xffff) == 0) {
                                        byte[] ret = new byte[byteCnt];
                                        for (int i = 0; i < byteCnt; i++) {
                                            ret[i] = response[i];
                                        }
                                        // logger.debug("MODBUS receive: {}", DatatypeConverter.printHexBinary(ret));
                                        return ret;
                                    }
                                    break;
                            }
                        }

                        /*
                         * if required length then must have failed, drop
                         * first byte and try again
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
        } catch (IOException e) {
            // e.printStackTrace();
            throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, e.getMessage());
        }
        throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                "Too much activity on recv line");
    }
}
