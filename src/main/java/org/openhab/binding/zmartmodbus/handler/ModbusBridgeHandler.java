/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.handler;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.NODE_NOT_CONFIGURED;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.config.ModbusBaseConfiguration;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.discovery.ModbusSlaveDiscoveryService;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.listener.StateListener;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusIoHandler;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusNode;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;
import org.openhab.binding.zmartmodbus.internal.transceiver.ModbusTransceiver;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusBridgeHandler} is responsible for handling commands,
 * which are sent to one of the channels.
 *
 * @author Peter Kristensen
 */
public class ModbusBridgeHandler extends BaseBridgeHandler implements ModbusIoHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusBridgeHandler.class);

    protected ModbusBaseConfiguration baseConfig;
    protected ModbusTransceiver transceiver = null;

    private final Map<ThingUID, @Nullable ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private int nodeId = NODE_NOT_CONFIGURED;
    int searchTime = 30;
    int transactionIndex = 0;

    ThingTypeUID thingTypeUID = null;

    // Checks if msgCounter should update to smarthome - can be set from UI
    boolean updateCounter = false;

    StateListener stateSubscriber = null;

    ModbusController controller = null;

    public ModbusBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Zmartify ModbusFunction Controller.");

        baseConfig = getConfigAs(ModbusBaseConfiguration.class);

        // We must set the state
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, ModbusBindingConstants.OFFLINE_CTLR_OFFLINE);
    }

     @Override
    public void dispose() {
        logger.info("Node {} : Dispose Bridge called", nodeId);

        if (isConnected()) {
            transceiver.disconnect();
        }

        // Remove the discovery service
        if (discoveryServiceRegs.size() > 0) {
            discoveryServiceRegs.entrySet().forEach(discoveryService -> {
                removeDeviceDiscoveryService(discoveryService.getKey());
            });
        }

          if (controller != null) {
            controller.stopListening();
        }
    }

    public boolean isConnected() {
        return transceiver != null ? transceiver.isConnected() : false;
    }

    public boolean getListening() {
        return controller.getListening();
    }

    @Override
    public ModbusController getController() {
        return controller;
    }

    @Override
    public ModbusNode getNode(int nodeId) {
        if (controller == null) {
            return null;
        }
        return controller.getNode(nodeId);
    }

    /**
     * @return the nodeId
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * @param nodeId the nodeId to set
     */
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public Collection<ModbusNode> getNodes() {
        if (controller == null) {
            return null;
        }
        return controller.getNodes();
    }

    public int getOwnNodeId() {
        if (controller == null) {
            return 0;
        }
        return controller.getOwnNodeId();
    }

    public String getProtocolName() {
        return "modbus";
    }

    public UID getUID() {
        return thing.getUID();
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
                // setUpdateCounter(command == OnOffType.ON);
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
        if (controller == null) {
            return;
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
        logger.info("Initializing Network Zmartify ModbusFunction controller {}", getUID());

        controller = new ModbusController(this);
        setNodeId(getOwnNodeId());
        controller.getActionFeed().setSlowPoll(baseConfig.getSlowPoll());
        controller.getActionFeed().setFastPoll(baseConfig.getFastPoll());
        controller.startListening();

        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.BRIDGE_OFFLINE, ModbusBindingConstants.OFFLINE_CTLR_ONLINE);

    }

    /**
     *
     * @param listener
     */
    public void register(StateListener listener) {
        stateSubscriber = listener;
    }

    public void setConnected(boolean state) {
        controller.setConnected(state);
    }

    public void setListening(boolean listening) {
        controller.setListening(listening);
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
        this.discoveryServiceRegs.put(uid, bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    public void stopDeviceDiscovery() {
        if (controller == null) {
            return;
        }
        controller.requestInclusionStop();
    }

    public ModbusBaseConfiguration getBaseConfig() {
        return baseConfig;
    }

    @Override
    public ModbusTransceiver getTransceiver() {
        return transceiver;
    }
}
