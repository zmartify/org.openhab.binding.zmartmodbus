/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.discovery;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_ELEMENTID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_NODECLASS;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_PARENTNODEID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.SUPPORTED_SLAVES_THING_TYPES_UIDS;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for discovering Modbus devices attached to the controller
 *
 * @author Peter Kristensen
 *
 *
 */
// @Component(service = { ModbusSlaveDiscoveryService.class }, immediate = true, configurationPid = "discovery.modbusslavediscoveryservice")
@NonNullByDefault
public class ModbusSlaveDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(ModbusSlaveDiscoveryService.class);
    private static final int searchTime = 5;

    private ModbusBridgeHandler controllerHandler;

    public ModbusSlaveDiscoveryService(ModbusBridgeHandler  controllerHandler) {
        super(ModbusBindingConstants.SUPPORTED_SLAVES_THING_TYPES_UIDS, searchTime);
        this.controllerHandler = controllerHandler;
        logger.debug("Creating ZmartModbus discovery service for {} with scan time of {}",
            controllerHandler.getThing().getUID(), searchTime);
    }

    private ModbusController getController() {
        return controllerHandler.getController();
    }

    public void activate() {
        logger.debug("ZmartModbus discovery: Active {}", controllerHandler.getThing().getUID());
    }

    @Override
    public void deactivate() {
        logger.debug("ZmartModbus discovery: Deactivate {}", controllerHandler.getThing().getUID());
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_SLAVES_THING_TYPES_UIDS;
    }

    @Override
    public void startScan() {
        logger.debug("ZmartModbus discovery: Start {}", controllerHandler.getThing().getUID());

        // Start the search for new devices
        discoverModbus();
    }

    @Override
    public synchronized void abortScan() {
        logger.debug("ZmartModbus discovery: Abort {}", controllerHandler.getThing().getUID());
        controllerHandler.stopDeviceDiscovery();
        super.abortScan();
    }

    @Override
    protected synchronized void stopScan() {
        logger.debug("ZmartModbus discovery: Stop {}", controllerHandler.getThing().getUID());
        controllerHandler.stopDeviceDiscovery();
        super.stopScan();
    }


    /**
     * Constructs the ThingUID based on unitAddress, channelId and elementId if available
     *
     * @param thingTypeUID
     * @param unitAddress
     * @param channelId
     * @param elementId
     * @return
     */
    private ThingUID makeThingUID(ThingTypeUID thingTypeUID, int unitAddress, int channelId, int elementId) {
        String subAddress;
        if (channelId != ID_NOT_USED) {
            if (elementId != ID_NOT_USED) {
                subAddress = String.format("adr%dc%de%d", unitAddress, channelId, elementId);
            } else {
                subAddress = String.format("adr%dc%d", unitAddress, channelId);
            }
        } else {
            subAddress = String.format("adr%d", unitAddress);
        }
        return new ThingUID(thingTypeUID, controllerHandler.getThing().getUID(), subAddress);
    }

    /**
     * Get called when a device is discovered
     *
     * @param thingTypeUID
     * @param unitAddress
     * @param channelId (ID_NOT_USED if not Jablotron)
     * @param elementId (ID_NOT_USED if not Jablotron)
     */
    public void deviceDiscovered(ThingTypeUID thingTypeUID, int parentNodeId, int channelId, int elementId) {
        logger.debug("DeviceDiscovered: {} - parentNodeId {}", thingTypeUID, parentNodeId);

        try {
            String nodeClassLabel = thingTypeUID.getId();

            // Initialize it (create if absent)
            ThingUID thingUID = makeThingUID(thingTypeUID, parentNodeId, channelId, elementId);
            String label = thingUID.getId().toString();

            if (thingUID != null) {
                logger.trace("Adding new Modbus Slave Thing {} to smarthome inbox", thingUID);
                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                        .withProperty(PROPERTY_NODECLASS, nodeClassLabel)
                        .withProperty(PROPERTY_PARENTNODEID, String.valueOf(parentNodeId))
                        .withProperty(PROPERTY_CHANNELID, String.valueOf(channelId))
                        .withProperty(PROPERTY_ELEMENTID, String.valueOf(elementId)).withLabel(label)
                        .withBridge(controllerHandler.getThing().getUID()).build();
                thingDiscovered(discoveryResult);
            }
        } catch (Exception e) {
            logger.debug("Error occurred during device discovery", e);
        }
    }

    /**
     * Used when a slave has been discovered
     *
     * @param thingType
     * @param unitAddress
     */
    public void deviceDiscovered(ThingTypeUID thingType, int parentNodeId) {
        deviceDiscovered(thingType, parentNodeId, ID_NOT_USED, ID_NOT_USED);
    }

    private synchronized void discoverModbus() {

        if (getController() == null) {
            logger.error("no controller");
            return;
        }
        
        // Initiate discovery for any subdevices on the node
        getController().getNodes().forEach(node -> {
            logger.info("Initiate discovery for any subdevices on the node");
            if (node.getNodeClass().supportDiscovery()) {
                node.getModbusFunction().startSubDeviceDiscovery(node.getNodeId());
            }
        });
    }

    public void stopDeviceDiscovery() {
        if (getController() == null) {
            return;
        }
    }
}
