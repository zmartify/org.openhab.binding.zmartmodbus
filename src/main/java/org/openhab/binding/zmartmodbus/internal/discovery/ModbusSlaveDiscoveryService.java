/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.discovery;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.zmartmodbus.handler.ZmartModbusHandler;
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

public class ModbusSlaveDiscoveryService extends AbstractDiscoveryService implements ExtendedDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(ModbusSlaveDiscoveryService.class);

    DiscoveryServiceCallback discoveryServiceCallback;

    private ZmartModbusHandler modbusHandler;

    public ModbusSlaveDiscoveryService(ZmartModbusHandler modbusHandler, int searchTime) {
        super(SUPPORTED_SLAVE_THING_TYPES_UIDS, searchTime, false);
        this.modbusHandler = modbusHandler;
    }

    private ModbusController getController() {
        return modbusHandler.getController();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_SLAVE_THING_TYPES_UIDS;
    }

    @Override
    public void startScan() {
        logger.debug("Start discovery scan for Modbus connected devices");
        discoverModbus();
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
        return new ThingUID(thingTypeUID, modbusHandler.getThing().getUID(), subAddress);
    }

    /**
     * Get called when a device is discovered
     *
     * @param thingTypeUID
     * @param unitAddress
     * @param channelId
     * @param elementId
     */
    public void deviceDiscovered(ThingTypeUID thingTypeUID, int unitAddress, int channelId, int elementId) {
        logger.info("DeviceDiscovered: {}", thingTypeUID);

        try {
            String nodeClassLabel = thingTypeUID.getId();

            // Initialize it (create if absent)
            ThingUID thingUID = makeThingUID(thingTypeUID, unitAddress, channelId, elementId);
            String label = thingUID.getId().toString();

            if (discoveryServiceCallback != null
                    && discoveryServiceCallback.getExistingDiscoveryResult(thingUID) != null) {
                // Ignore this node as we already know about it
                logger.info("Device already known. {}", label);
                return;
            } else if (thingUID != null) {
                logger.trace("Adding new ModbusFunction Thing {} to smarthome inbox", thingUID);
                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                        .withProperty(PROPERTY_NODECLASS, nodeClassLabel)
                        .withProperty(PROPERTY_UNITADDRESS, String.valueOf(unitAddress))
                        .withProperty(PROPERTY_CHANNELID, String.valueOf(channelId))
                        .withProperty(PROPERTY_ELEMENTID, String.valueOf(elementId)).withLabel(label)
                        .withBridge(modbusHandler.getThing().getUID()).build();
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
    public void slaveDiscovered(ThingTypeUID thingType, int unitAddress) {
        deviceDiscovered(thingType, unitAddress, ID_NOT_USED, ID_NOT_USED);
    }

    private synchronized void discoverModbus() {
        int unitAddress;

        if (getController() == null) {
            return;
        }

        for (String supportedSlave : SUPPORTED_SLAVES) {
            unitAddress = modbusHandler.getConfigParamInt("slave_" + supportedSlave, SLAVE_UNAVAILABLE);
            if (unitAddress != SLAVE_UNAVAILABLE) {
                // If unitAddress is a valid number, we define the slave as discovered and will try to set it online
                slaveDiscovered(new ThingTypeUID(BINDING_ID, supportedSlave), unitAddress);
            }
        }

        // Initiate discovery for any subdevices on the node
        getController().getNodes().forEach(node -> {
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

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {
        this.discoveryServiceCallback = discoveryServiceCallback;
    }
}
