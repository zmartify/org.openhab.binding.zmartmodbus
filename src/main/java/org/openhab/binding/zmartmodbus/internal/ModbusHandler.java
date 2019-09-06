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
package org.openhab.binding.zmartmodbus.internal;

import static org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass.Read;
import static org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass.Status;
import static org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass.Write;
import static org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusFeedRepeat.Once;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusThingHandler;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.listener.MessageListener;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusCommEvent;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * @author Peter Kristensen - Initial contribution
 *
 */

public class ModbusHandler<T> {

    private Logger logger = LoggerFactory.getLogger(ModbusHandler.class);

    @Nullable private ModbusBridgeHandler bridgeHandler;
    @Nullable private ActionListener actionSubscriber;
    @Nullable private MessageListener messageSubscriber;
    
    public ModbusHandler() {
    }

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
                ModbusThingHandler modbusThingHandler = (ModbusThingHandler) getBridgeHandler().getThingHandlerByUID(modbusAction.getThingUID());
                int unitAddress = modbusThingHandler.getId();
                ModbusFunction modbusFunction = modbusThingHandler.getModbusFunction();

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
                            } else {
                                getBridgeHandler().getCounters().incrementFailedCounter();
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
                            getBridgeHandler().getTransceiver().disconnect();
                            break;
                        default:
                            logger.error("We got an exception in ModbusCommunicator ({}) {}", e.getCode(), e.getCode().name());
                            break;
                        }
                } catch (Exception e) {
                    logger.error("EXCEPTION: {}", e.getMessage());
                }
            }

            @Override
            public void onError(Throwable e) {
                logger.info("onError {}", e.getMessage());
            }

            @Override
            public void onComplete() {
                getBridgeHandler().getTransceiver().disconnect();
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

    public void terminate() {
    }

    public ModbusBridgeHandler getBridgeHandler() {
        if (bridgeHandler != null) {
            return bridgeHandler;
        } else {
            logger.error("BridgeHandler not set");
            return null;
        }
    }

    public void setBridgeHandler(ModbusBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }
}
