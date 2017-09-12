/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.handler;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
// import java.util.TooManyListenersException;
import java.util.TooManyListenersException;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.validation.ConfigValidationException;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.discovery.ModbusThingDiscoveryService;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.listener.StateListener;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusIoHandler;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusNode;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

/**
 * The {@link ZmartModbusHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Peter Kristensen
 */
public class ZmartModbusHandler extends BaseBridgeHandler implements ModbusIoHandler {

    private Logger logger = LoggerFactory.getLogger(ZmartModbusHandler.class);

    private int nodeId = NODE_NOT_CONFIGURED;
    int searchTime = 30;
    int transactionIndex = 0;
    int msgCounter = 0;

    // Response Time Out (respTout) handling
    int respToutCounter = 0;
    int respTout = 400;

    // Delay between modbus messages inorder to avoid loss of messages
    int delayBetweenMessages = 200;

    int txMode = DEFAULT_TXMODE;
    int slowPoll = DEFAULT_POLLS * 10;
    int fastPoll = DEFAULT_POLLS;
    ThingTypeUID thingTypeUID = BRIDGE_UNKNOWN;
    // Checks if msgCounter should update to smarthome - can be set from UI
    boolean updateCounter = false;

    ModbusThingDiscoveryService discoveryService = null;
    ServiceRegistration discoveryRegistration = null;

    StateListener stateSubscriber = null;

    ModbusController controller = null;

    public ZmartModbusHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void connect() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException,
            TooManyListenersException, IOException {
    }

    @Override
    public void deviceDiscovered(ThingTypeUID thingTypeUID, int unitAddress, int channelId, int elementId) {
        if (discoveryService == null) {
            return;
        }
        discoveryService.deviceDiscovered(thingTypeUID, unitAddress, channelId, elementId);
    }

    public void deviceDiscovered(ThingTypeUID thingType, int unitAddress) {
        deviceDiscovered(thingType, unitAddress, ID_NOT_USED, ID_NOT_USED);
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void dispose() {
        logger.info("Dispose called");
        // Remove the discovery service

        if (discoveryService != null) {
            discoveryService.deactivate();
        }
        if (discoveryRegistration != null) {
            discoveryRegistration.unregister();
        }

        // if (this.converterHandler != null) {
        // this.converterHandler = null;
        // }

        if (controller != null) {

            controller.stopListening();
        }
    }

    @Override
    public boolean isConnected() {
        return controller.isConnected();
    }

    public int getConfigParamInt(String keyword, int defValue) {
        Object param = getConfig().get(keyword);
        if (param instanceof BigDecimal && param != null) {
            return ((BigDecimal) param).intValue();
        }
        return defValue;
    }

    public String getConfigParamStr(String keyword, String defValue) {
        Object param = getConfig().get(keyword);
        if (param instanceof String && param != null) {
            return (String) param;
        }
        return defValue;
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
        logger.info("ModbusControllerHandler.handlecommand: {} {}", channelUID.getIdWithoutGroup(), command);
        // Push it on the queue
        if (command instanceof RefreshType) {
            // We do not support REFRESH
        } else {
            switch (channelUID.getIdWithoutGroup()) {
                case "update_counter":
                    setUpdateCounter(command == OnOffType.ON);
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

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters)
            throws ConfigValidationException {
        logger.info("Controller Configuration update received");

        // Perform checking on the configuration
        validateConfigurationParameters(configurationParameters);

        boolean reinitialise = false;

        Configuration configuration = editConfiguration();
        for (Entry<String, Object> configurationParameter : configurationParameters.entrySet()) {
            Object value = configurationParameter.getValue();
            logger.debug("Controller Configuration update {} to {}", configurationParameter.getKey(), value);
            String[] cfg = configurationParameter.getKey().split("_");
            switch (cfg[0]) {
                case "controller":
                    if (controller == null) {
                        logger.info("Trying to send controller command, but controller is not initialised");
                        continue;
                    }

                    if (cfg[1].equals("softreset") && value instanceof BigDecimal
                            && ((BigDecimal) value).intValue() == ZmartModbusBindingConstants.ACTION_CHECK_VALUE) {
                        controller.requestSoftReset();

                        value = new BigDecimal(0);
                    } else if (cfg[1].equals("hardreset") && value instanceof BigDecimal
                            && ((BigDecimal) value).intValue() == ZmartModbusBindingConstants.ACTION_CHECK_VALUE) {
                        controller.requestHardReset();

                        value = new BigDecimal(0);
                    } else if (cfg[1].equals("exclude") && value instanceof BigDecimal
                            && ((BigDecimal) value).intValue() == ZmartModbusBindingConstants.ACTION_CHECK_VALUE) {
                        controller.requestRemoveNodesStart();

                        value = new BigDecimal(0);
                    } else if (cfg[1].equals("sync") && value instanceof BigDecimal
                            && ((BigDecimal) value).intValue() == ZmartModbusBindingConstants.ACTION_CHECK_VALUE) {
                        controller.requestRequestNetworkUpdate();

                        value = new BigDecimal(0);
                    } else if (cfg[1].equals("inclusiontimeout") && value instanceof BigDecimal) {
                        reinitialise = true;
                    }
                case "slave": {
                    reinitialise = true;
                    switch (cfg[1]) {
                        case THING_TYPE_JABLOTRON_AC116:
                            break;
                        case THING_TYPE_NILAN_COMFORT_300:
                            break;
                        default:
                            break;
                    }
                }
                    break;
                case "port":
                    reinitialise = true;
                    break;
                default:
                    break;
            }

            configuration.put(configurationParameter.getKey(), value);
        }

        // Persist changes
        updateConfiguration(configuration);

        if (reinitialise == true) {
            dispose();
            initialize();
        }
    }

    protected void incomingMessage() {
        if (controller == null) {
            return;
        }
    }

    public void increaseMsgCounter() {
        msgCounter++;
        if (updateCounter) {
            updateState(new ChannelUID(getThing().getUID(), CHANNEL_MESSAGE_COUNT), new DecimalType(msgCounter));
        }
    }

    public void increaseRespToutCounter() {
        respToutCounter++;
        if (updateCounter) {
            updateState(new ChannelUID(getThing().getUID(), CHANNEL_RESPTOUT_COUNT), new DecimalType(respToutCounter));
        }
    }

    public void setUpdateCounter(boolean updateCounter) {
        this.updateCounter = updateCounter;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Zmartify ModbusFunction Controller.");

        // We must set the state
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Offline - controller offline");
    }

    /**
     * Common initialization point for all ModbusFunction controllers.
     * Called by bridges after they have initialized their interfaces.
     *
     * @throws ModbusProtocolException
     *
     */
    public void initializeNetwork() {
        logger.info("Initialising Network Zmartify ModbusFunction controller {}", getUID());

        controller = new ModbusController(this);
        setNodeId(getOwnNodeId());
        controller.getActionFeed().setSlowPoll(slowPoll);
        controller.getActionFeed().setFastPoll(fastPoll);
        controller.startListening();

        // Start the discovery service
        // TODO
        discoveryService = new ModbusThingDiscoveryService(this, searchTime);
        discoveryService.activate();

        // And register it as an OSGi service
        discoveryRegistration = bundleContext.registerService(DiscoveryService.class.getName(), discoveryService,
                new Hashtable<String, Object>());

        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Zmartify ModbusFunction controller online");

    }

    public void register(StateListener listener) {
        stateSubscriber = listener;
    }

    @Override
    public byte[] msgTransaction(byte[] msg) throws ModbusProtocolException {
        return msgTransaction(msg, CUSTOMCODE_STANDARD);
    }

    @Override
    public byte[] msgTransaction(byte[] msg, int customCode) throws ModbusProtocolException {
        return null;
    }

    public void requestRemoveNodesStart() {
    }

    public void setConnected(boolean state) {
        controller.setConnected(state);
    }

    public void setListening(boolean listening) {
        controller.setListening(listening);
    }

    public void startDeviceDiscovery() {
        int unitAddress;

        if (controller == null) {
            return;
        }

        for (String supportedSlave : SUPPORTED_SLAVES) {
            unitAddress = getConfigParamInt("slave_" + supportedSlave, SLAVE_UNAVAILABLE);
            if (unitAddress != SLAVE_UNAVAILABLE) {
                deviceDiscovered(new ThingTypeUID(BINDING_ID, supportedSlave), unitAddress);
            }
        }

        controller.getNodes().forEach(node -> {
            if (node.getNodeClass().supportDiscovery()) {
                node.getModbusFunction().startSubDeviceDiscovery(node.getNodeId());
            }
        });
    }

    public void stopDeviceDiscovery() {
        if (controller == null) {
            return;
        }
    }
}
