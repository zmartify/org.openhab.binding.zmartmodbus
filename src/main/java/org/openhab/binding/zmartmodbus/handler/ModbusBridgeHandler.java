/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.handler;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusNodeClass;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.config.ModbusBridgeConfiguration;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.discovery.ModbusSlaveDiscoveryService;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.listener.StateListener;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusCounters;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunction;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunctionJablotron;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusIOHandler;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusBridgeHandler} is responsible for handling commands, which
 * are sent to one of the channels.
 *
 * @author Peter Kristensen
 */
public class ModbusBridgeHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusBridgeHandler.class);

    protected ModbusBridgeConfiguration modbusBridgeConfig;
    protected ModbusIOHandler modbusIO;
    protected ModbusCounters counters = new ModbusCounters();

    private final Map<ThingUID, @Nullable ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    int searchTime = 30;
    int transactionIndex = 0;

    ThingTypeUID thingTypeUID = null;

    // Checks if msgCounter should update to SmartHome - can be set from UI
    boolean updateCounter = false;

    StateListener stateSubscriber = null;

    public ModbusBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Modbus BridgeHandler : {}", thing.getBridgeUID());
        modbusBridgeConfig = getConfigAs(ModbusBridgeConfiguration.class);

        // Create a new IOController for this Bridge
        modbusIO = new ModbusIOHandler(new ModbusController(this));

        // We must set the state
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                ModbusBindingConstants.OFFLINE_CTLR_OFFLINE);
    }

    @Override
    public void dispose() {
        logger.info("Dispose Bridge called : {}", thing.getBridgeUID());

        if (isConnected()) {
            getModbusIO().getTransceiver().disconnect();
        }

        // Remove the discovery service
        if (discoveryServiceRegs.size() > 0) {
            discoveryServiceRegs.entrySet().forEach(discoveryService -> {
                removeDeviceDiscoveryService(discoveryService.getKey());
            });
        }

        if (getController() != null) {
            getController().stopListening();
        }
    }

    /**
     * Common initialization point for all ModbusFunction controllers. Called by
     * bridges after they have initialized their interfaces.
     *
     * @throws ModbusProtocolException
     *
     */
    public void initializeNetwork() {
        logger.info("Initializing ModbusBridge");

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING, "Starting action feeds");

        getController().getActionFeed().setSlowPoll(modbusBridgeConfig.getSlowPoll());
        getController().getActionFeed().setFastPoll(modbusBridgeConfig.getFastPoll());
        getController().startListening();

        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.BRIDGE_OFFLINE, ModbusBindingConstants.OFFLINE_CTLR_ONLINE);
    }

    public boolean isConnected() {
        return getModbusIO().getTransceiver() != null ? getModbusIO().getTransceiver().isConnected() : false;
    }

    public boolean getListening() {
        return getController().getListening();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("ModbusBridgeHandler.handlecommand: {} {}", channelUID.getIdWithoutGroup(), command);
        // Push it on the queue
        if (command instanceof RefreshType) {
            // We do not support REFRESH
        } else {
            switch (channelUID.getIdWithoutGroup()) {
            case "update_counter":
                updateCounter = (command == OnOffType.ON);
                break;
            default:
                stateSubscriber.modbusState(new ModbusState(channelUID, (State) command));
                break;
            }
        }
    }

    @Override
    public void handleUpdate(ChannelUID uid, State state) {
        updateState(uid, state);
    }

    // Function to convert from ModbusState package to SmartHome state
    public void handleUpdate(ModbusState modbusState) {
        handleUpdate(modbusState.getUid(), modbusState.getState());
    }

    @Override
    public void handleRemoval() {
        dispose();
        updateStatus(ThingStatus.REMOVED);
    }

    protected void incomingMessage() {
        if (getController() == null) {
            return;
        }
    }

    /**
     * Based on nodeClass the corresponding set of ModbusFunction calls are selected
     * 
     * @param nodeClass
     * @return
     */
    public ModbusFunction newModbusFunction(ModbusNodeClass nodeClass) {
        switch (nodeClass) {
        case JablotronAC116:
        case JablotronActuator:
        case JablotronTP150:
            return new ModbusFunctionJablotron((ModbusBridgeHandler) getBridgeHandler());
        default:
            return new ModbusFunction((ModbusBridgeHandler) getBridgeHandler());
        }
    }

    /**
     *
     * @param listener
     */
    public void register(StateListener listener) {
        stateSubscriber = listener;
    }

    public void setConnected(boolean state) {
        getController().setConnected(state);
    }

    public void setListening(boolean listening) {
        getController().setListening(listening);
    }

    protected void removeDeviceDiscoveryService(ThingUID uid) {
        if (this.discoveryServiceRegs != null) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(uid);
            if (serviceReg != null) {
                serviceReg.unregister();
                discoveryServiceRegs.remove(uid);
            }
        }
    }

    public void registerDeviceDiscoveryService(ModbusSlaveDiscoveryService discoveryService, ThingUID uid) {
        logger.debug("Registering DiscoveryService for {}", uid);

        discoveryService.activate();
        this.discoveryServiceRegs.put(uid, bundleContext.registerService(DiscoveryService.class.getName(),
                discoveryService, new Hashtable<String, Object>()));
    }

    public void stopDeviceDiscovery() {
        if (getController() == null) {
            return;
        }
    }

    public ModbusBridgeConfiguration getModbusBridgeConfig() {
        return modbusBridgeConfig;
    }

    public ModbusIOHandler getModbusIO() {
        return modbusIO;
    }

    public ModbusController getController() {
        return modbusIO.getController();
    }

    protected ModbusBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        return bridge != null ? (ModbusBridgeHandler) bridge.getHandler() : null;
    }

    public ModbusCounters getCounters() {
        return counters;
    }

    public ModbusThingHandler getThingHandlerByUID(ThingUID thingUID) {
        return (ModbusThingHandler) getBridge().getThing(thingUID).getHandler();
    }

    protected void onSuccessfulOperation() {
        // update without error -> we're back online
        if (getThing().getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }
}
