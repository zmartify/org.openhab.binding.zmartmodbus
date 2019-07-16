/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.handler;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.META_HEATUNITID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.META_THERMOSTATID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.NODE_NOT_CONFIGURED;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELCFG_DATASET;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELCFG_INDEX;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELCFG_REPORTON;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELCFG_VALUETYPE;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_TYPE_JABLOTRON_ACTUATOR;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.THING_TYPE_JABLOTRON_TP150;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusDataSetClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusMessageClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusNodeClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusValueClass;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.config.ModbusSlaveConfiguration;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.discovery.ModbusSlaveDiscoveryService;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusDataSet;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusDeviceInfo;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusNode;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
import org.openhab.binding.zmartmodbus.internal.util.Jablotron;
import org.openhab.binding.zmartmodbus.internal.util.Register;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 *
 * @author Peter Kristensen
 *
 */
public class ModbusThingHandler extends ConfigStatusThingHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusThingHandler.class);

    private ModbusBridgeHandler bridgeHandler;

    protected ModbusSlaveConfiguration config;

    private ModbusSlaveDiscoveryService discoveryService;

    private int nodeId = NODE_NOT_CONFIGURED;

    public ModbusThingHandler(Thing modbusDevice) {
        super(modbusDevice);
    }

    @Override
    public void initialize() {
        logger.info("Initializing modbus thing handler {}", getThing().getUID());
        setBridgeHandler((ModbusBridgeHandler) getBridge().getHandler());

        // Get slave id from thing configuration
        config = getConfigAs(ModbusSlaveConfiguration.class);
        logger.info("Modbus slave id = {}", config.getId());

        // We need to set the status to OFFLINE so that the framework calls our
        // notification handlers
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                ModbusBindingConstants.OFFLINE_CTLR_OFFLINE);

        // Make sure the thingType is set correctly from the database
        if (updateThingType() == true) {
            // The thing will have been disposed of so let's exit!
            return;
        }

        initializeBridge((getBridge() == null) ? null : getBridge().getHandler(),
                (getBridge() == null) ? null : getBridge().getStatus());

        logger.debug("Starting to register deviceDiscovery");
        discoveryService = new ModbusSlaveDiscoveryService(getBridgeHandler());

        bridgeHandler.registerDeviceDiscoveryService(discoveryService, getThing().getUID());

    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.info("NODE {}: Controller status changed to {}.", nodeId, bridgeStatusInfo.getStatus());

        initializeBridge((getBridge() == null) ? null : getBridge().getHandler(), bridgeStatusInfo.getStatus());
    }

    private void initializeBridge(ThingHandler thingHandler, ThingStatus controllerStatus) {
        logger.info("NODE {}: Initialize Bridge {} for thing {}", nodeId, controllerStatus, getThing().getUID());

        if (thingHandler != null && controllerStatus != null) {

            setBridgeHandler((ModbusBridgeHandler) thingHandler);

            if ((getBridge().getStatus() == ThingStatus.ONLINE)) {
                // Check if our node is configured, otherwise its time to do it now.
                if (nodeId == NODE_NOT_CONFIGURED) {
                    nodeId = bridgeHandler.getController().nextNodeId();
                    initializeNode(nodeId);
                }
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    public void initializeNode(int nodeId) {
        logger.debug("NODE {}: initializeNode {}", nodeId, getThing().getUID());

        getController().initializeNode(nodeId, config.getId(),
                ModbusNodeClass.fromString(getThing().getThingTypeUID().getId()));

        getController().getNode(nodeId).setChannelId(getChannelId());
        getController().getNode(nodeId).setElementId(getElementId());

        initializeDataSets();
        initializeChannels();

        if (getNode(nodeId).getNodeClass().equals(ModbusNodeClass.JablotronAC116)) {
            logger.debug("Registering JablotronAC116 to receive internal messages i.e. discovery information");
            getController().hotMessage.filter(modbusMessage -> modbusMessage.isInternal())
                    .subscribe(modbusMessage -> this.MessageListener().onNext(modbusMessage));
        }

        logger.debug("NODE {}: Initialized the modbusId {} {}", nodeId, config.getId(), this.getThing().getUID());
    }

    public ModbusNode getNode(int nodeId) {
        return bridgeHandler.getController().getNode(nodeId);
    }

    public ModbusController getController() {
        return bridgeHandler.getController();
    }

    private int addressWizard(String property, int channelId, int elementId) {
        switch (property) {
        case "channel":
            return channelId;
        case "element":
            return elementId;
        default:
            return Integer.decode(property);
        }
    }

    private String makeDataSetKey(String key, int nodeId) {
        return String.format("%d:%s", nodeId, key);
    }

    private void initializeDataSets() {
        String dataSetKey;
        int nodeChannelId = getNode(nodeId).getChannelId();
        int nodeElementId = getNode(nodeId).getElementId();

        Map<String, String> properties = getThing().getProperties();
        // logger.debug("NODE {}: Initializing {} with {} DataSets", nodeId,
        // getThing().getUID(), properties.size());
        for (Entry<String, String> property : properties.entrySet()) {
            String[] keys = property.getKey().split("_");
            if (keys[0].equals("dataset")) {

                // Add nodeId to dataSetKey to keep it local to this node
                dataSetKey = makeDataSetKey(keys[1], nodeId);

                String[] cfg = property.getValue().split(":");

                ModbusMessageClass messageClass = ModbusMessageClass.fromString(cfg[0]);
                ModbusFeedRepeat feedRepeat = ModbusFeedRepeat.fromString(cfg[1]);
                ModbusReportOn reportOn = ModbusReportOn.fromString(cfg[2]);

                int start = 0;
                int offset = 0;

                int length = addressWizard(cfg[3], nodeChannelId, nodeElementId);
                switch (cfg.length) {
                // Jablotron AC-116
                case 8:
                    offset = addressWizard(cfg[7], nodeChannelId, nodeElementId);
                case 7:
                    start = Jablotron.getAddress(/* category */ addressWizard(cfg[4], nodeChannelId, nodeElementId),
                            /* index */ addressWizard(cfg[5], nodeChannelId, nodeElementId),
                            /* page */ addressWizard(cfg[6], nodeChannelId, nodeElementId));
                    break;
                // Standard Modbus
                case 6:
                    offset = addressWizard(cfg[5], nodeChannelId, nodeElementId);
                case 5:
                    start = addressWizard(cfg[4], nodeChannelId, nodeElementId);
                    break;
                default:
                    logger.warn("NODE {}: Illegal number of property parameters", nodeId);
                    return;
                }
                if (messageClass.equals(ModbusMessageClass.Unknown)) {
                    logger.error("NODE {}: Unknown ModbusMessageClass '{}'", nodeId, cfg[0]);
                } else {
                    ModbusDataSet dataSet = new ModbusDataSet(nodeId, messageClass, start, length, offset,
                            nodeChannelId, nodeElementId, reportOn, feedRepeat, ModbusDataSetClass.SmartHome,
                            bridgeHandler.getController().getNode(nodeId).getNodeClass());

                    bridgeHandler.getController().getModbusFactory().getDataSets().addDataSet(dataSetKey, dataSet);
                    bridgeHandler.getController().getActionFeed()
                            .addAction(new ModbusAction(dataSet, ModbusActionClass.Read));
                    logger.debug("NODE {}: DataSet {} added", nodeId, dataSetKey);
                }
            }
        }
    }

    private void initializeChannels() {
        logger.debug("NODE {}: Initializing channels", nodeId);
        Map<String, String> properties = new HashMap<String, String>();
        for (Channel channel : getThing().getChannels()) {
            properties = channel.getProperties();

            // Check if elementId and channelId has been put into configuration, otherwise
            // add it
            // if (!channel.getConfiguration().containsKey("elementId")) {
            if (getNode(nodeId).getElementId() != ID_NOT_USED) {
                channel.getConfiguration().put(META_THERMOSTATID, getNode(nodeId).getElementId());
            }

            if (getNode(nodeId).getChannelId() != ID_NOT_USED) {
                channel.getConfiguration().put(META_HEATUNITID, getNode(nodeId).getChannelId());
            }

            logger.debug("NODE {}: CONFIG: ElementId {} ChannelId {} added to channel '{}' configuration:", nodeId,
                    getNode(nodeId).getElementId(), getNode(nodeId).getChannelId(), channel.getUID().getId());

            if (properties.containsKey(PROPERTY_CHANNELCFG_INDEX)) {
                bridgeHandler.getController().getModbusFactory().getDataSets()
                        .addChannel(new ModbusThingChannel(nodeId, channel.getUID(),
                                makeDataSetKey(properties.get(PROPERTY_CHANNELCFG_DATASET), nodeId),
                                ModbusValueClass.fromString(properties.get(PROPERTY_CHANNELCFG_VALUETYPE)),
                                addressWizard(properties.get(PROPERTY_CHANNELCFG_INDEX), getNode(nodeId).getChannelId(),
                                        getNode(nodeId).getElementId()),
                                ModbusReportOn.fromString(properties.get(PROPERTY_CHANNELCFG_REPORTON))));
            }
        }
    }

    /**
     * Check the thing type and change it if it's wrong
     */
    private boolean updateThingType() {
        // If the thing type is still the default, then see if we can change
        if (getThing().getThingTypeUID().equals(ModbusBindingConstants.MODBUS_THING_UID) == false) {
            return false;
        }
        return true;
    }

    @Override
    public void dispose() {
        if (nodeId != 0) {
            logger.debug("Disposing node {}", getThing().getUID());
            if (bridgeHandler != null) {
                bridgeHandler.getController().getActionFeed().removeActions(nodeId);
                bridgeHandler.getController().getModbusFactory().getDataSets().removeChannels(nodeId);
                bridgeHandler.getController().getModbusFactory().getDataSets().removeDataSets(nodeId);
                bridgeHandler.getController().removeNode(nodeId);
            }
            nodeId = NODE_NOT_CONFIGURED;
        }
        bridgeHandler.removeDeviceDiscoveryService(getThing().getUID());

        bridgeHandler = null;
        logger.debug("Handler disposed. Listeners unregistered.");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Let the bridge handle it
        if (bridgeHandler != null) {
            bridgeHandler.handleCommand(channelUID, command);
        } else {
            logger.warn("Node {}: handleCommand {} - bridgeHandler not set!!", nodeId, command);
        }
    }

    @Override
    public void handleRemoval() {
        dispose();
        updateStatus(ThingStatus.REMOVED);
    }

    public void setBridgeHandler(ModbusBridgeHandler bridgeHandler) {

        if (bridgeHandler == null) {
            logger.debug("BridgeHandler = null - UPS!!");
        }
        this.bridgeHandler = bridgeHandler;
    }

    protected synchronized ModbusBridgeHandler getBridgeHandler() {
        if (bridgeHandler != null) {
            return bridgeHandler;
        } else {
            Bridge bridge = getBridge();
            if (bridge != null) {
                ThingHandler handler = bridge.getHandler();
                if (handler instanceof ModbusBridgeHandler) {
                    bridgeHandler = (ModbusBridgeHandler) handler;
                    if (bridgeHandler.getOwnNodeId() != 0) {
                        bridgeStatusChanged(bridge.getStatusInfo());
                    }
                    return bridgeHandler;
                } else {
                    logger.error("This Bridge is not a ModbusBridge - {}", handler.getThing().getUID());
                    return null;
                }
            } else {
                logger.warn("Bridge not found...");
                return null;
            }
        }
    }

    public ModbusDeviceInfo getDeviceInfo() {
        return getNode(nodeId).getDeviceInfo();
    }

    /**
     * Return an ISO 8601 combined date and time string for current date/time
     *
     * @return String with format "yyyy-MM-dd'T'HH:mm:ss'Z'"
     */
    public static String getISO8601StringForCurrentDate() {
        Date now = new Date();
        return getISO8601StringForDate(now);
    }

    /**
     * Return an ISO 8601 combined date and time string for specified date/time
     *
     * @param date Date
     * @return String with format "yyyy-MM-dd'T'HH:mm:ss'Z'"
     */
    private static String getISO8601StringForDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public Observer<ModbusMessage> MessageListener() {
        return new Observer<ModbusMessage>() {

            @Override
            public void onSubscribe(Disposable d) {
                logger.info(" onSubscribe : {}", d.isDisposed());
            }

            @Override
            public void onComplete() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onError(Throwable arg0) {
                logger.error("We have received an error :: {}", arg0.getMessage());

            }

            @Override
            public void onNext(ModbusMessage modbusMessage) {
                receivedDiscovery(modbusMessage);
            }
        };
    }

    public void receivedDiscovery(ModbusMessage modbusMessage) {
        logger.debug("Received discovery message from Message Listener ThingHandler");
        int dataSetId = modbusMessage.getDataSetId();
        int unitAddress = config.getId();
        int elementAddress = Register.registersToIntSwap((byte[]) modbusMessage.getPayload(), 0);

        if (elementAddress != 0) {
            BitVector assignmentMap = BitVector
                    .createBitVectorSwap(Arrays.copyOfRange(((byte[]) modbusMessage.getPayload()), 4, 8));
            // assignmentMap.setMSBAccess();
            assignmentMap.forceSize(17);
            BitVector status = BitVector
                    .createBitVectorSwap(Arrays.copyOfRange(((byte[]) modbusMessage.getPayload()), 16, 18), 16);
            // status.setMSBAccess();
            if (!status.getBit(9)) {
                // We assume it is a Thermostat as it is not a magnetic contact
                logger.debug("We have discovered a Thermostat");
                int lowestChannel = 16;
                for (int i = 0; i < 16; i++) {
                    if (assignmentMap.getBit(i)) {
                        // First channel found, set lowestchannel equal it.
                        if (lowestChannel == 16) {
                            lowestChannel = i;
                        }
                        logger.debug("We have discovered an actuator");
                        discoveryService.deviceDiscovered(THING_TYPE_JABLOTRON_ACTUATOR, unitAddress, i, ID_NOT_USED);
                    }
                }
                discoveryService.deviceDiscovered(THING_TYPE_JABLOTRON_TP150, unitAddress, lowestChannel, Jablotron
                        .getPage(getController().getModbusFactory().getDataSets().getDataSet(dataSetId).getStart()));
            }
        }
    }

    private int getChannelId() {
        String idStr = this.getThing().getProperties().get(ModbusBindingConstants.PROPERTY_CHANNELID);
        if (idStr == null) {
            return ID_NOT_USED;
        } else {
            return Integer.decode(idStr);
        }
    }

    private int getElementId() {
        String idStr = this.getThing().getProperties().get(ModbusBindingConstants.PROPERTY_ELEMENTID);
        if (idStr == null) {
            return ID_NOT_USED;
        } else {
            return Integer.decode(idStr);
        }
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        Collection<ConfigStatusMessage> configStatus = new ArrayList<>();
        return configStatus;
    }
}
