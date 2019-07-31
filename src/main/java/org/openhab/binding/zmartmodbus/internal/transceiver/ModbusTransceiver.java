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
package org.openhab.binding.zmartmodbus.internal.transceiver;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusCounters;

/**
 *
 * @author Peter Kristensen - Initial contribution
 * 
 */
@NonNullByDefault
public abstract class ModbusTransceiver {

    // Input and output streams, must be created by transceiver implementations
    @Nullable protected InputStream inputStream;
    @Nullable protected OutputStream outputStream;

    protected ModbusCounters counters;

    private boolean connected = false;

    public ModbusTransceiver(ModbusCounters counters) {
        this.counters = counters;
    }

    public void connect()
            throws  ModbusProtocolException  {
                setConnected(true);
    }

    public void disconnect() {
        setConnected(false);
    }

    /**
     * msgTransaction is responsible for the low level communication with the modbus device
     *  
     * msgTransaction must be called with a byte array having two extra bytes for
     * the CRC. It will return a byte array of the response to the message.
     * Validation will include checking the CRC and verifying the command matches.
     * 
     * @param msg
     * @param customCode
     * @return msg
     * @throws ModbusProtocolException
     */
    public byte[] msgTransaction(byte[] msg, int customCode) throws ModbusProtocolException {
        return new byte[0];
    }

    protected byte asciiLrcCalc(byte[] msg, int len) {
        char[] ac = new char[2];
        ac[0] = (char) msg[len - 4];
        ac[1] = (char) msg[len - 3];
        String s = new String(ac);
        byte lrc = (byte) Integer.parseInt(s, 16);
        return lrc;
    }

    protected int binLrcCalc(byte[] msg) {
        int llrc = 0;
        for (byte element : msg) {
            llrc += element & 0xff;
        }
        llrc = (llrc ^ 0xff) + 1;
        // byte lrc=(byte)(llrc & 0x0ff);
        return llrc;
    }

    /**
     * convertCommandToAscii: convert a binary command into a standard
     * ModbusFunction ASCII frame
     */
    protected byte[] convertCommandToAscii(byte[] msg) {
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
     * convertAsciiResponseToBin: convert a standard ModbusFunction frame to byte
     * array
     */
    protected byte[] convertAsciiResponseToBin(byte[] msg, int len) {
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

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }
}
