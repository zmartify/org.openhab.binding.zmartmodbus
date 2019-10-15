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
package org.openhab.binding.zmartmodbus.internal.controller;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.internal.ModbusHandler;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusActionFeed;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusFactory;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.listener.MessageListener;
import org.openhab.binding.zmartmodbus.internal.listener.StateListener;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;

/**
 * This interface defines interface to communicate ModbusFunction controller.
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
public class ModbusController {

    private Logger logger = LoggerFactory.getLogger(ModbusController.class);

    private ModbusBridgeHandler bridgeHandler;

    private boolean connected = false; // Connected to ModbusFunction
    private boolean listening = false; // Connected to ModbusFunction

    /**
     * Constants for managing the ModbusFunction protocol
     */
    static final String PROTOCOL_NAME = "modbusFunction";

    private ModbusActionFeed<ModbusAction> actionFeed = new ModbusActionFeed<ModbusAction>();
    private ModbusHandler<ModbusMessage> modbusHandler = new ModbusHandler<ModbusMessage>();
    private ModbusFactory<ModbusState> modbusFactory = new ModbusFactory<ModbusState>();

    private ConnectableFlowable<ModbusAction> hotAction;
    private ConnectableFlowable<ModbusMessage> hotMessage;
    private ConnectableFlowable<ModbusState> hotStateFromModbus;
    private ConnectableFlowable<ModbusState> hotStateToModbus;

    public Flowable<ModbusAction> modbusActionQueue = Flowable.create(emitter -> {
        ActionListener listener = new ActionListener() {
            @Override
            public void modbusAction(ModbusAction event) {
                emitter.onNext(event);
                if (event.isLast()) {
                    emitter.onComplete();
                }
            }

            @Override
            public void error(Throwable e) {
                emitter.onError(e);
            }
        };
        actionFeed.register(listener);
        modbusHandler.register(listener);
        modbusFactory.register(listener);
    }, BackpressureStrategy.BUFFER);

    public Flowable<ModbusMessage> modbusMessageQueue = Flowable.create(emitter -> {
        MessageListener listener = new MessageListener() {
            @Override
            public void modbusMessage(ModbusMessage event) {
                emitter.onNext(event);
                if (event.isLast()) {
                    emitter.onComplete();
                }
            }

            @Override
            public void error(Throwable e) {
                emitter.onError(e);
            }
        };
        modbusHandler.register(listener);
    }, BackpressureStrategy.BUFFER);

    public Flowable<ModbusState> modbusStateFromModbusQueue = Flowable.create(emitter -> {
        StateListener listener = new StateListener() {
            @Override
            public void modbusState(ModbusState event) {
                emitter.onNext(event);
                if (event.isLast()) {
                    emitter.onComplete();
                }
            }

            @Override
            public void error(Throwable e) {
                emitter.onError(e);
            }
        };
        modbusFactory.register(listener);
    }, BackpressureStrategy.BUFFER);

    public Flowable<ModbusState> modbusStateToModbusQueue = Flowable.create(emitter -> {
        StateListener listener = new StateListener() {
            @Override
            public void modbusState(ModbusState event) {
                emitter.onNext(event);
                if (event.isLast()) {
                    emitter.onComplete();
                }
            }

            @Override
            public void error(Throwable e) {
                emitter.onError(e);
            }
        };
        bridgeHandler.register(listener);
    }, BackpressureStrategy.BUFFER);

    /**
     * Creates a new instance of the ModbusFunction controller class.
     *
     * @param bridgeHandler  the io handler to use for communication with the ModbusFunction controller interface
     *
     */
    public ModbusController(ModbusBridgeHandler bridgeHandler) {
        logger.info("Starting ModbusFunction controller {} - {}", bridgeHandler);
        this.bridgeHandler = bridgeHandler;
        this.modbusHandler.setBridgeHandler(bridgeHandler);
 
        // If we are not the controller, then get device information populated
        /*
         * if (nodeId != CONTROLLER_NODE_ID) {
         * ModbusAction action = new ModbusAction(nodeId, 0, ModbusMessageClass.GetDeviceInfo, ModbusActionClass.Read,
         * ModbusFeedRepeat.Once, 0, 0, 0, ModbusReportOn.Always);
         * getActionFeed().addAction(action);
         * }
         */

    }

    public ModbusBridgeHandler getBridgeHandler() {
        return bridgeHandler;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean getListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public void startListening() {
        logger.debug("Controller start listening");
        
        if (!this.listening) {

            logger.debug("Start listening ModbusActionQueue");
            hotAction = modbusActionQueue.publish();
            hotAction.connect();
            hotAction.subscribe(modbusAction -> getModbusHandler().ModbusCommunicator().onNext(modbusAction));

            logger.debug("Start listening ModbusMessageQueue");
            hotMessage = modbusMessageQueue.publish();
            hotMessage.connect();
            hotMessage.filter(modbusMessage -> !modbusMessage.isInternal())
                    .subscribe(modbusMessage -> getModbusFactory().messageListener().onNext(modbusMessage));

            logger.debug("Start listening ModbusStateFromModbusQueue");
            hotStateFromModbus = modbusStateFromModbusQueue.publish();
            hotStateFromModbus.connect();
            hotStateFromModbus.subscribe(modbusState -> bridgeHandler.handleUpdate(modbusState));

            logger.debug("Start listening ModbusStateToModbusQueue");
            hotStateToModbus = modbusStateToModbusQueue.publish();
            hotStateToModbus.connect();
            hotStateToModbus.subscribe(modbusState -> getModbusFactory().stateListener().onNext(modbusState));

            setListening(true);
        }
    }

    public void updateChannelFromModbus(ModbusThingChannel channel) {
        logger.debug("Controller received update Channel {} {} {}", channel.getUID(), channel.getDataSetKey(),
                channel.getState());
        bridgeHandler.handleUpdate(channel.getUID(), channel.getState());
    }

    public void stopListening() {
        // Signal to listener to stop
        logger.debug("Stop listening for actions");

        hotAction.connect().dispose();
        hotAction.subscribe().dispose();

        hotMessage.connect().dispose();
        hotMessage.subscribe().dispose();

        hotStateFromModbus.connect().dispose();
        hotStateFromModbus.subscribe().dispose();

        hotStateToModbus.connect().dispose();
        hotStateToModbus.subscribe().dispose();

        getModbusHandler().terminate();
        setListening(false);
    }

    public ModbusFactory<ModbusState> getModbusFactory() {
        return modbusFactory;
    }

    public ModbusActionFeed<ModbusAction> getActionFeed() {
        return actionFeed;
    }

    public ModbusHandler<ModbusMessage> getModbusHandler() {
        return modbusHandler;
    }
    @Nullable public ConnectableFlowable<ModbusMessage> getHotMessage() {
        return hotMessage;
    }
}
