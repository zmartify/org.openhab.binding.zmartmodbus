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
package org.openhab.binding.zmartmodbus.handler;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.META_HEATUNITID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.META_THERMOSTATID;
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
import org.eclipse.smarthome.core.thing.ThingUID;
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
import org.openhab.binding.zmartmodbus.internal.config.ModbusThingConfiguration;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusThingChannel;
import org.openhab.binding.zmartmodbus.internal.discovery.ModbusSlaveDiscoveryService;
import org.openhab.binding.zmartmodbus.internal.factory.ModbusDataSet;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusDeviceInfo;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunction;
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
 * @author Peter Kristensen - Initial contribution
 *
 */
public class ModbusThingHandler extends ConfigStatusThingHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusThingHandler.class);

    protected ModbusThingConfiguration modbusThingConfig;

    private ModbusSlaveDiscoveryService discoveryService;
    private ModbusNodeClass nodeClass;
    private ModbusFunction modbusFunction = null;

    // Special configuration parameters needed for jablotron
    private int channelId = ID_NOT_USED; // Used for Jablotron special addressing
    private int elementId = ID_NOT_USED; // Used for Jablotron special addressing

    private boolean configured = false;

    private ModbusDeviceInfo deviceInfo = new ModbusDeviceInfo("", "", "", "");

    public ModbusThingHandler(Thing modbusDevice) {
        super(modbusDevice);
    }

    @Override
    public void initialize() {
        logger.info("Initializing modbus thing handler {}", getThing().getUID());

        // Get slave id from thing configuration
        modbusThingConfig = getConfigAs(ModbusThingConfiguration.class);

        configured = false;

        // We need to set the status to OFFLINE so that the framework calls our
        // notification handlers
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                ModbusBindingConstants.OFFLINE_CTLR_OFFLINE);

        // Make sure the thingType is set correctly from the database
        // if (updateThingType() == true) {
        // The thing will have been disposed of so let's exit!
        // return;
        // b }

        initializeBridge((getBridge() == null) ? null : getBridge().getHandler(),
                (getBridge() == null) ? null : getBridge().getStatus());

        logger.debug("Starting to register deviceDiscovery");
        discoveryService = new ModbusSlaveDiscoveryService(getBridgeHandler());

        getBridgeHandler().registerDeviceDiscoveryService(discoveryService, getThing().getUID());

    }

    @SuppressWarnings("null")
    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("{} : Controller status changed to {}.", thing.getUID(), bridgeStatusInfo.getStatus());

        initializeBridge((getBridge() == null) ? null : getBridge().getHandler(), bridgeStatusInfo.getStatus());
    }

    @SuppressWarnings("null")
    private void initializeBridge(ThingHandler thingHandler, ThingStatus controllerStatus) {
        logger.info("Initialize Bridge {} for thing {}", controllerStatus, getThing().getUID());

        if (thingHandler != null && controllerStatus != null) {

            if ((getBridge().getStatus() == ThingStatus.ONLINE)) {
                if (!configured) {
                    Thread configThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            initializeThing();
                            updateStatus(ThingStatus.ONLINE);
                        }
                    });
                    // Set configured to avoid double configuring
                    configured = true;
                    configThread.start();
                }
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    public void initializeThing() {
        logger.debug("initializeThing {}", getThing().getUID());

        nodeClass = ModbusNodeClass.fromString(getThing().getThingTypeUID().getId());
        modbusFunction = getBridgeHandler().newModbusFunction(nodeClass);

        String idStr = this.getThing().getProperties().get(ModbusBindingConstants.PROPERTY_CHANNELID);
        if (idStr == null) {
            channelId = ID_NOT_USED;
        } else {
            channelId = Integer.decode(idStr);
        }

        idStr = this.getThing().getProperties().get(ModbusBindingConstants.PROPERTY_ELEMENTID);
        if (idStr == null) {
            elementId = ID_NOT_USED;
        } else {
            elementId = Integer.decode(idStr);
        }

        initializeDataSets();
        initializeChannels();

        if (nodeClass.equals(ModbusNodeClass.JablotronAC116)) {
            logger.debug("Registering JablotronAC116 to receive internal messages i.e. discovery information");
            getBridgeHandler().getController().hotMessage
                    .filter(modbusMessage -> modbusMessage.isInternal())
                    .subscribe(modbusMessage -> this.MessageListener().onNext(modbusMessage));
        }

        logger.debug("Initialized the modbusId {} {}", modbusThingConfig.getId(), this.getThing().getUID());
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

    private String makeDataSetKey(String key, ThingUID thingUID) {
        return String.format("%s:%s", thingUID.getAsString(), key);
    }

    private void initializeDataSets() {
        String dataSetKey;

        Map<String, String> properties = getThing().getProperties();
        // logger.debug("NODE {}: Initializing {} with {} DataSets", nodeId,
        // getThing().getUID(), properties.size());
        for (Entry<String, String> property : properties.entrySet()) {
            String[] keys = property.getKey().split("_");
            if (keys[0].equals("dataset")) {

                // Add nodeId to dataSetKey to keep it local to this node
                dataSetKey = makeDataSetKey(keys[1], thing.getUID());

                String[] cfg = property.getValue().split(":");

                ModbusMessageClass messageClass = ModbusMessageClass.fromString(cfg[0]);
                ModbusFeedRepeat feedRepeat = ModbusFeedRepeat.fromString(cfg[1]);
                ModbusReportOn reportOn = ModbusReportOn.fromString(cfg[2]);

                int start = 0;
                int offset = 0;

                int length = addressWizard(cfg[3], channelId, elementId);
                switch (cfg.length) {
                // Jablotron AC-116
                case 8:
                    offset = addressWizard(cfg[7], channelId, elementId);
                case 7:
                    start = Jablotron.getAddress(/* category */ addressWizard(cfg[4], channelId, elementId),
                            /* index */ addressWizard(cfg[5], channelId, elementId),
                            /* page */ addressWizard(cfg[6], channelId, elementId));
                    break;
                // Standard Modbus
                case 6:
                case 5:
                    offset = addressWizard(cfg[cfg.length - 1], channelId, elementId);
                    break;
                default:
                    logger.warn("Thing {}: Illegal number of property parameters", thing.getUID());
                    return;
                }
                if (messageClass.equals(ModbusMessageClass.Unknown)) {
                    logger.error("Thing {}: Unknown ModbusMessageClass '{}'", thing.getUID(), cfg[0]);
                } else {
                    ModbusDataSet dataSet = new ModbusDataSet(thing.getUID(), messageClass, start, length, offset,
                            channelId, elementId, reportOn, feedRepeat, ModbusDataSetClass.SmartHome,
                            nodeClass);

                    getBridgeHandler().getController().getModbusFactory().getDataSets()
                            .addDataSet(dataSetKey, dataSet);

                    getBridgeHandler().getController().getActionFeed()
                            .addAction(new ModbusAction(dataSet, ModbusActionClass.Read));

                    logger.debug("Thing {}: DataSet {} added", thing.getUID(), dataSetKey);
                }
            }
        }
    }

    private void initializeChannels() {
        logger.debug("Thing {}: Initializing channels", thing.getUID());
        Map<String, String> properties = new HashMap<String, String>();

        for (Channel channel : getThing().getChannels()) {
            properties = channel.getProperties();

            // Check if elementId and channelId has been put into configuration, otherwise
            // add it
            // if (!channel.getConfiguration().containsKey("elementId")) {
            if (elementId != ID_NOT_USED) {
                channel.getConfiguration().put(META_THERMOSTATID, elementId);
            }

            if (channelId != ID_NOT_USED) {
                channel.getConfiguration().put(META_HEATUNITID, channelId);
            }

            logger.debug("Thing {}: CONFIG: ElementId {} ChannelId {} added to channel '{}' configuration:",
                    thing.getUID(), elementId, channelId, channel.getUID().getId());

            if (properties.containsKey(PROPERTY_CHANNELCFG_INDEX)) {
                getBridgeHandler().getController().getModbusFactory().getDataSets()
                        .addChannel(new ModbusThingChannel(thing.getUID(), channel.getUID(),
                                makeDataSetKey(properties.get(PROPERTY_CHANNELCFG_DATASET), thing.getUID()),
                                ModbusValueClass.fromString(properties.get(PROPERTY_CHANNELCFG_VALUETYPE)),
                                addressWizard(properties.get(PROPERTY_CHANNELCFG_INDEX), channelId, elementId),
                                ModbusReportOn.fromString(properties.get(PROPERTY_CHANNELCFG_REPORTON))));
            }
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing node {}", getThing().getUID());
        if (getBridgeHandler() != null) {
            getBridgeHandler().getController().getActionFeed().removeActions(thing.getUID());
            getBridgeHandler().getController().getModbusFactory().getDataSets()
                    .removeChannels(thing.getUID());
            getBridgeHandler().getController().getModbusFactory().getDataSets()
                    .removeDataSets(thing.getUID());

            getBridgeHandler().removeDeviceDiscoveryService(thing.getUID());
        }
        logger.debug("Handler disposed. Listeners unregistered.");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Let the bridge handle it
        if (getBridgeHandler() != null) {
            getBridgeHandler().handleCommand(channelUID, command);
        } else {
            logger.warn("Node {}: handleCommand {} - bridgeHandler not set!!", thing.getUID(), command);
        }
    }

    @Override
    public void handleRemoval() {
        dispose();
        updateStatus(ThingStatus.REMOVED);
    }

    @SuppressWarnings("null")
    protected synchronized ModbusBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof ModbusBridgeHandler) {
                return (ModbusBridgeHandler) handler;
            } else {
                logger.error("This Bridge is not a ModbusBridge - {}", handler.getThing().getUID());
                return null;
            }
        } else {
            logger.warn("Bridge not found...");
            return null;
        }
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
                logger.debug(" onSubscribe : {}", d.isDisposed());
            }

            @Override
            public void onComplete() {
                logger.debug("MessageListener completed");
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

    /**
     * Received discovery message from Message Listener ThingHandler
     * 
     * @param modbusMessage
     */
    public void receivedDiscovery(ModbusMessage modbusMessage) {
        logger.debug("Received discovery message");
        int dataSetId = modbusMessage.getDataSetId();
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
                logger.debug("Thing {} : We have discovered a Thermostat", thing.getUID());
                int lowestChannel = 16;
                for (int i = 0; i < 16; i++) {
                    if (assignmentMap.getBit(i)) {
                        // First channel found, set lowestchannel equal it.
                        if (lowestChannel == 16) {
                            lowestChannel = i;
                        }
                        logger.debug("NODE {} : We have discovered an actuator", thing.getUID());
                        discoveryService.deviceDiscovered(THING_TYPE_JABLOTRON_ACTUATOR, thing.getUID(), i,
                                ID_NOT_USED);
                    }
                }
                discoveryService.deviceDiscovered(THING_TYPE_JABLOTRON_TP150, thing.getUID(), lowestChannel, Jablotron
                        .getPage(getController().getModbusFactory().getDataSets().getDataSet(dataSetId).getStart()));
            }
        }
    }

    public ModbusController getController() {
        return getBridgeHandler().getController();
    }

    public int getId() {
        ThingUID parentThingUID = getParentThingUID();
        if (parentThingUID != null) {
            // if parentThingUID defined return the Id of the parent (this is only relevant
            // for subslave things)
            Thing thing = getBridgeHandler().getThingByUID(parentThingUID);
            return (thing != null) ? ((ModbusThingHandler) thing.getHandler()).getId() : null;
        } else
            return modbusThingConfig.getId();
    }

    private ThingUID getParentThingUID() {
        String idStr = this.getThing().getProperties().get(ModbusBindingConstants.PROPERTY_PARENTTHINGUID);
        if (idStr == null) {
            return null;
        } else {
            return new ThingUID(idStr);
        }
    }

    public int getChannelId() {
        return channelId;
    }

    public int getElementId() {
        return elementId;
    }

    public ModbusFunction getModbusFunction() {
        return modbusFunction;
    }

    public ModbusNodeClass getNodeClass() {
        return nodeClass;
    }

    public ModbusThingConfiguration getModbusThingConfig() {
        return modbusThingConfig;
    }
    public void setDeviceInfo(ModbusDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public ModbusDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean state) {
        configured = state;
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        Collection<ConfigStatusMessage> configStatus = new ArrayList<>();
        return configStatus;
    }
}
