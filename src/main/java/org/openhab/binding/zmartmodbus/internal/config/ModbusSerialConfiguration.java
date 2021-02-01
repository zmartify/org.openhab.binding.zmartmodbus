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
package org.openhab.binding.zmartmodbus.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import gnu.io.SerialPort;

/**
 * The {@link ModbusSerialConfiguration} is the base class for configuration
 * information held by the device
 *
 * @author Sami Salonen - Initial contribution
 * @author Peter Kristensen - Modified for ZmartModbus
 *
 */
@NonNullByDefault
public class ModbusSerialConfiguration {

    private String port = "";
    private int baud;
    @Nullable
    private String stopBits;
    @Nullable
    private String parity;
    private int dataBits;
    @Nullable
    private String encoding;
    private boolean echo;
    private int receiveTimeoutMillis;
    @Nullable
    private String flowControlIn;
    @Nullable
    private String flowControlOut;
    
    private int txMode;

    private int timeBetweenTransactionsMillis;
    private int connectMaxTries;
    private int connectTimeoutMillis;

    // Time between updating the reported counters in OpenHAB
    private int timeBetweenCounterUpdates;


    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getBaud() {
        return baud;
    }

    public void setBaud(int baud) {
        this.baud = baud;
    }

    public int getStopBits() {
        switch (this.stopBits != null ? this.stopBits : "1.0") {
            case "1.0": return SerialPort.STOPBITS_1; // default if not specified
            case "1.5": return SerialPort.STOPBITS_1_5;
            case "2.0": return SerialPort.STOPBITS_2;
            default: return 0;
        }
    }

    public void setStopBits(String stopBits) {
        this.stopBits = stopBits;
    }

    public int getParity() {
        switch (this.parity != null ? this.parity : "none") {
            case "none": return SerialPort.PARITY_NONE; // default if not specified
            case "even": return SerialPort.PARITY_EVEN;
            case "odd": return SerialPort.PARITY_ODD;
            case "space": return SerialPort.PARITY_SPACE;
            default:
                return SerialPort.PARITY_NONE;
        }
    } 

    public void setParity(String parity) {
        this.parity = parity;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public @Nullable String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isEcho() {
        return echo;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }

    public int getReceiveTimeoutMillis() {
        return receiveTimeoutMillis;
    }

    public void setReceiveTimeoutMillis(int receiveTimeoutMillis) {
        this.receiveTimeoutMillis = receiveTimeoutMillis;
    }

    public @Nullable String getFlowControlIn() {
        return flowControlIn;
    }

    public void setFlowControlIn(String flowControlIn) {
        this.flowControlIn = flowControlIn;
    }

    public @Nullable String getFlowControlOut() {
        return flowControlOut;
    }

    public void setFlowControlOut(String flowControlOut) {
        this.flowControlOut = flowControlOut;
    }

    public int getTimeBetweenTransactionsMillis() {
        return timeBetweenTransactionsMillis;
    }

    public void setTimeBetweenTransactionsMillis(int timeBetweenTransactionsMillis) {
        this.timeBetweenTransactionsMillis = timeBetweenTransactionsMillis;
    }

    public int getConnectMaxTries() {
        return connectMaxTries;
    }

    public void setConnectMaxTries(int connectMaxTries) {
        this.connectMaxTries = connectMaxTries;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public boolean isValid() {
        return this.port != null;
    }

    public int getTxMode() {
        return txMode;
    }

    public void setTxMode(int txMode) {
        this.txMode = txMode;
    }

    public int getTimeBetweenCounterUpdates() {
        return timeBetweenCounterUpdates;
    }

    public void setTimeBetweenCounterUpdates(int timeBetweenCounterUpdates) {
        this.timeBetweenCounterUpdates = timeBetweenCounterUpdates;
    }

}
