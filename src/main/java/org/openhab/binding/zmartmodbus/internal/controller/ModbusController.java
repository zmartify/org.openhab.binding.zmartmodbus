/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.zmartmodbus.internal.controller;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.CONTROLLER_NODE_ID;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusMessageClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusNodeClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusThingChannel;
import org.openhab.binding.zmartmodbus.internal.ModbusHandler;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusInterfaceException;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusActionFeed;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusFactory;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.listener.MessageListener;
import org.openhab.binding.zmartmodbus.internal.listener.StateListener;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusNode;
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
 * @author Peter Kristensen
 *
 */
public class ModbusController {

    private Logger logger = LoggerFactory.getLogger(ModbusController.class);

    private AtomicInteger countNodeId = new AtomicInteger(0);

    private ModbusBridgeHandler bridgeHandler;

    /*
     * Configuration parameters
     */
    private int ownNodeId = CONTROLLER_NODE_ID; // MasterController always registered as 0

    private boolean connected = false; // Connected to ModbusFunction
    private boolean listening = false; // Connected to ModbusFunction

    /**
     * Constants for managing the ModbusFunction protocol
     */
    static final String PROTOCOL_NAME = "modbusFunction";

    private final ConcurrentHashMap<Integer, ModbusNode> modbusNodes = new ConcurrentHashMap<Integer, ModbusNode>();

    private ModbusActionFeed<ModbusAction> actionFeed;
    private ModbusHandler<ModbusMessage> modbusHandler;
    private ModbusFactory<ModbusState> modbusFactory;

    public ConnectableFlowable<ModbusAction> hotAction;
    public ConnectableFlowable<ModbusMessage> hotMessage;
    public ConnectableFlowable<ModbusState> hotStateFromModbus;
    public ConnectableFlowable<ModbusState> hotStateToModbus;

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
     * @param handler the io handler to use for communication with the ModbusFunction controller interface
     * @param config a map of configuration parameters
     *
     * @throws ModbusInterfaceException
     *             when a connection error occurs
     *             Controller connection parameters (e.g. serial port name or IP
     *             address).
     */
    public ModbusController(ModbusBridgeHandler handler) {
        ownNodeId = nextNodeId();
        logger.info("Starting ModbusFunction controller {} - {}", getOwnNodeId(), handler);
        
        actionFeed = new ModbusActionFeed<>(handler.getBaseConfig().getSlowPoll(), handler.getBaseConfig().getFastPoll());
        modbusHandler = new ModbusHandler<>();
        modbusFactory = new ModbusFactory<>();

        setBridgeHandler(handler);
        initializeNode(ownNodeId, 0, ModbusNodeClass.Master);
    }

    public void initializeNode(int nodeId, int unitAddress, ModbusNodeClass nodeClass) {
        logger.info("NODE {}: initializeNode id = {} - class = {}" , nodeId, unitAddress, nodeClass);

        ModbusNode node = new ModbusNode(nodeId, this);
        node.setUnitAddress(unitAddress);
        node.setNodeClass(nodeClass);

        // Place nodes in the local ModbusFunction Controller (create if it doesn't exist)
        modbusNodes.putIfAbsent(nodeId, node);

        // If we are not the controller, then get device information populated
        if (nodeId != CONTROLLER_NODE_ID) {
            ModbusAction action = new ModbusAction(nodeId, 0, ModbusMessageClass.GetDeviceInfo, ModbusActionClass.Read,
                    ModbusFeedRepeat.Once, 0, 0, 0, ModbusReportOn.Allways);
            getActionFeed().addAction(action);
        }
    }

    public ModbusNode getNode(int nodeId) {
        return this.modbusNodes.get(nodeId);
    }

    public void removeNode(int nodeId) {
        this.modbusNodes.remove(nodeId);
    }

    public Collection<ModbusNode> getNodes() {
        return this.modbusNodes.values();
    }

    public void reinitialiseNode(int nodeId) {
        int unitAddress = getNode(nodeId).getUnitAddress();
        ModbusNodeClass nodeClass = getNode(nodeId).getNodeClass();
        ThingTypeUID thingTypeUID = getNode(nodeId).getThingTypeUID();
        removeNode(nodeId);

        //thing was already created,
        if (thingTypeUID != null) {
            this.bridgeHandler.getDiscoveryService().deviceDiscovered(thingTypeUID, unitAddress, getChannelId(nodeId),
                    getElementId(nodeId));
        }
        initializeNode(nodeId, unitAddress, nodeClass);
    }

    public int getOwnNodeId() {
        return ownNodeId;
    }

    public void setOwnNodeId(int ownNodeId) {
        this.ownNodeId = ownNodeId;
    }

    public int getUnitAddress(int nodeId) {
        return getNode(nodeId).getUnitAddress();
    }

    public int getChannelId(int nodeId) {
        return getNode(nodeId).getChannelId();
    }

    public int getElementId(int nodeId) {
        return getNode(nodeId).getElementId();
    }

    public ModbusBridgeHandler getBridgeHandler() {
        return bridgeHandler;
    }

    public void setBridgeHandler(ModbusBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        modbusHandler.setBridgeHandler(bridgeHandler);
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

    public int nextNodeId() {
        return countNodeId.getAndIncrement();
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
}
