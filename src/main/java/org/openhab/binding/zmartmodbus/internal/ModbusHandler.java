/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal;

import static org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass.Read;
import static org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass.Status;
import static org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass.Write;
import static org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusFeedRepeat.Once;

import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusThingHandler;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.listener.MessageListener;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusCommEvent;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunction;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusThing;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * @author Peter Kristensen
 *
 */
public class ModbusHandler<T> {

    private Logger logger = LoggerFactory.getLogger(ModbusHandler.class);

    private ModbusBridgeHandler bridgeHandler = null;

    private ActionListener actionSubscriber;
    private MessageListener messageSubscriber;

    public Observer<ModbusAction> ModbusCommunicator() {
        return new Observer<ModbusAction>() {
            // Set listening flag - can be turned off to stop listening

            @Override
            public void onSubscribe(Disposable d) {
                logger.info(" onSubscribe : {}", d.isDisposed());
            }

            @Override
            public void onNext(ModbusAction modbusAction) {
                if (bridgeHandler == null) {
                    logger.error("BridgeHandler not set");
                    return;
                }
                Object payload = null;
                @SuppressWarnings("unused")
                ModbusCommEvent commEvent = null;
                ModbusThing modbusThing = ((ModbusThingHandler) bridgeHandler.getThingByUID(modbusAction.getThingUID()).getHandler()).getModbusThing();
                int unitAddress = modbusThing.getId();
                ModbusFunction modbusFunction = modbusThing.getModbusFunction();

                try {

                    if (!bridgeHandler.isConnected()) {
                        logger.error("Not CONNECTED - ModbusAction discarded");
                    } else {
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
                                modbusThing.setDeviceInfo(modbusFunction.getDeviceInfo(unitAddress));
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
                                    logger.warn("NODE {}: Wrong messageClass for writing {} ({})",
                                            modbusAction.getThingUID(), modbusAction.getMessageClass(),
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
                            bridgeHandler.getModbusIO().getTransceiver().disconnect();
                            break;
                        default:
                            logger.error("We got an exception in ModbusCommunicator ({}) {}", e.getCode(), e.getCode().name());
                            break;
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                logger.info("onError {}", e.getMessage());
            }

            @Override
            public void onComplete() {
                bridgeHandler.getModbusIO().getTransceiver().disconnect();
                logger.info("onComplete");
            }
        };
    }

    /*
     * register ModbusMessages to listen for
     *
     */
    public void register(MessageListener listener) {
        messageSubscriber = listener;
    }

    public void register(ActionListener listener) {
        actionSubscriber = listener;
    }

    public void setBridgeHandler(ModbusBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    public void terminate() {
    }
}
