/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.protocol;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

/**
 *
 * @author Peter Kristensen
 *
 */
public interface ModbusIoHandler {
    byte[] msgTransaction(byte[] msg, int customCode) throws ModbusProtocolException;

    byte[] msgTransaction(byte[] msg) throws ModbusProtocolException;

    public void connect() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException,
            TooManyListenersException, IOException;

    public ModbusNode getNode(int nodeId);

    public ModbusController getController();

    public void disconnect();

    boolean isConnected();
}
