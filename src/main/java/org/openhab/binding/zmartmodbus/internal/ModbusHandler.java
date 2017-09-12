/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusActionClass.*;
import static org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusFeedRepeat.Once;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.openhab.binding.zmartmodbus.handler.ZmartModbusHandler;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.listener.MessageListener;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusCommEvent;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class ModbusHandler<T> {

    private Logger logger = LoggerFactory.getLogger(ModbusHandler.class);

    private ZmartModbusHandler bridgeHandler = null;

    private ActionListener actionSubscriber;
    private MessageListener messageSubscriber;

    public Observer<ModbusAction> ModbusCommunicator() {
        return new Observer<ModbusAction>() {
            // Set listening flag - can be turned off to stop listening

            @Override
            public void onSubscribe(Disposable d) {
                logger.info(" onSubscribe : {}", d.isDisposed());
                try {
                    bridgeHandler.connect();
                } catch (NoSuchPortException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (PortInUseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (UnsupportedCommOperationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (TooManyListenersException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void onNext(ModbusAction modbusAction) {
                if (bridgeHandler == null) {
                    logger.info("BridgeHandler not set");
                    return;
                }
                Object payload = null;
                @SuppressWarnings("unused")
                ModbusCommEvent commEvent = null;
                int unitAddress = bridgeHandler.getController().getUnitAddress(modbusAction.getNodeId());
                ModbusFunction modbusFunction = bridgeHandler.getController().getNode(modbusAction.getNodeId())
                        .getModbusFunction();

                try {

                    if (!bridgeHandler.isConnected()) {
                        bridgeHandler.connect();
                    }

                    if (bridgeHandler.isConnected()) {

                        if (modbusAction.getActionClass().equals(Read)) {
                            switch (modbusAction.getMessageClass()) {
                                case Coil:
                                    payload = modbusFunction.readCoils(unitAddress, modbusAction.getStart(),
                                            modbusAction.getOffset(), modbusAction.getLength());
                                    break;
                                case Discrete:
                                    payload = modbusFunction.readDiscreteInputs(unitAddress, modbusAction.getStart(),
                                            modbusAction.getOffset(), modbusAction.getLength());
                                    break;
                                case Holding:
                                case Input:
                                    payload = modbusFunction.readHoldingRegisters(unitAddress, modbusAction.getStart(),
                                            modbusAction.getLength());
                                    break;
                                case GetDeviceInfo:
                                    bridgeHandler.getController().getNode(modbusAction.getNodeId())
                                            .setDeviceInfo(modbusFunction.getDeviceInfo(unitAddress));
                                    // Set no further action
                                    payload = null;
                                default:
                                    break;
                            }
                            if (payload != null) {
                                messageSubscriber.modbusMessage(new ModbusMessage(modbusAction.getDataSetId(), payload,
                                        modbusAction.isInternal()));
                            }
                        } else if (modbusAction.getActionClass().equals(Write)) {
                            switch (modbusAction.getMessageClass()) {
                                case Coil:
                                    modbusFunction.writeSingleCoil(unitAddress, modbusAction.getStart(),
                                            modbusAction.getOffset(), (boolean) modbusAction.getPayload());
                                    break;
                                case Holding:
                                    modbusFunction.writeMultipleRegisters(unitAddress,
                                            modbusAction.getStart() + modbusAction.getOffset(),
                                            ((byte[]) modbusAction.getPayload()));
                                    break;
                                case SetLogicalAddress:
                                    modbusFunction.setLogicalAddress(unitAddress);
                                    break;
                                default:
                                    logger.debug("NODE {}: Wrong messageClass for writing {} ({})",
                                            modbusAction.getNodeId(), modbusAction.getMessageClass(),
                                            modbusAction.getDataSetId());
                                    break;
                            }
                        } else if (modbusAction.getActionClass().equals(Status)) {
                            switch (modbusAction.getMessageClass()) {
                                case GetCommEventCounter:
                                    commEvent = modbusFunction.getCommEventCounter(unitAddress);
                                    break;
                                case GetCommEventLog:
                                    commEvent = modbusFunction.getCommEventLog(unitAddress);
                                    break;
                                case ReadExceptionStatus:
                                    payload = modbusFunction.readExceptionStatus(unitAddress);
                                    break;
                                default:
                                    logger.warn("Unsupported ModbusFunction function");
                                    break;
                            }
                        }
                    }

                } catch (ModbusProtocolException e) {
                    switch (e.getCode()) {
                        case RESPONSE_TIMEOUT:
                            // Check for possible retries (automatically counts # of retries)
                            if (modbusAction.retry()) {
                                // As this is a retry, only run once
                                modbusAction.setFeedRepeat(Once);
                                // Add it to the action feed
                                actionSubscriber.modbusAction(modbusAction);
                            }
                            break;
                        case TRANSACTION_FAILURE:
                        case INVALID_CONFIGURATION:
                        case INVALID_DATA_ADDRESS:
                        case INVALID_DATA_LENGTH:
                        case INVALID_DATA_TYPE:
                            logger.error("Modbus error: {} {}", e.getCause(), e.getMessage());
                            break;
                        case NOT_CONNECTED:
                            logger.error("Modbus unit ({}) NOT CONNECTED", unitAddress);
                            break;
                        case CONNECTION_FAILURE:
                            logger.error("Connection failure: {} {}", e.getCause(), e.getMessage());
                            bridgeHandler.disconnect();
                            break;
                        default:
                            logger.error("We got an exception in ModbusCommunicator {}  {}", e.getCause(),
                                    e.getMessage());
                            break;
                    }

                } catch (Exception e) {
                    logger.info("We got an exception in ModbusCommunicator {}  {}", e.getCause(), e.getMessage());
                    e.printStackTrace();
                    bridgeHandler.disconnect();
                }
            }

            @Override
            public void onError(Throwable e) {
                logger.info("onError {}", e.getMessage());
            }

            @Override
            public void onComplete() {
                bridgeHandler.disconnect();
                logger.info("onComplete");
            }
        };
    }

    /*
     * register ModbusMessages to listen for
     *
     */
    public void register(MessageListener listener) {
        logger.info("register Message listener");
        messageSubscriber = listener;
    }

    public void register(ActionListener listener) {
        logger.info("register Action listener");
        actionSubscriber = listener;
    }

    public void setBridgeHandler(ZmartModbusHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    public void terminate() {
    }
}
