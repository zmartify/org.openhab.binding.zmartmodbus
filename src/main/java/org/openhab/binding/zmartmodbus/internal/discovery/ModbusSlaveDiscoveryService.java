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
package org.openhab.binding.zmartmodbus.internal.discovery;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_CHANNELID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_ELEMENTID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_NODECLASS;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.PROPERTY_PARENTTHINGUID;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.SUPPORTED_SLAVES_THING_TYPES_UIDS;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for discovering Modbus devices attached to the controller
 *
 * @author Peter Kristensen - Initial contribution
 *
 *
 */
@NonNullByDefault
public class ModbusSlaveDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(ModbusSlaveDiscoveryService.class);

    private final ModbusBridgeHandler bridgeHandler;

    public ModbusSlaveDiscoveryService(ModbusBridgeHandler bridgeHandler, int searchTime) {
        super(searchTime);
        logger.debug("ModbusSlaveDiscovery started");
        this.bridgeHandler = bridgeHandler;
    }

    public void activate() {
        super.activate(null);
        logger.debug("ZmartModbus discovery: Active {}", bridgeHandler.getThing().getUID());
    }

    @Override
    public void deactivate() {
        super.deactivate();
        logger.debug("ZmartModbus discovery: Deactivate {}", bridgeHandler.getThing().getUID());
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_SLAVES_THING_TYPES_UIDS;
    }

    @Override
    public void startScan() {
        logger.debug("ZmartModbus discovery: Start {}", bridgeHandler.getThing().getUID());

        // Start the search for new devices
        discoverModbus();
    }

    @Override
    public synchronized void abortScan() {
        logger.debug("ZmartModbus discovery: Abort {}", bridgeHandler.getThing().getUID());
        bridgeHandler.stopDeviceDiscovery();
        super.abortScan();
    }

    @Override
    protected synchronized void stopScan() {
        logger.debug("ZmartModbus discovery: Stop {}", bridgeHandler.getThing().getUID());
        bridgeHandler.stopDeviceDiscovery();
        super.stopScan();
    }

    /**
     * Constructs the ThingUID based on unitAddress, channelId and elementId if available
     *
     * @param thingTypeUID
     * @param unitAddress
     * @param channelId
     * @param elementId
     * 
     * @return ThingUID
     */
    private ThingUID makeThingUID(ThingTypeUID thingTypeUID, int channelId, int elementId) {
        String subAddress;
        if (channelId != ID_NOT_USED) {
            if (elementId != ID_NOT_USED) {
                subAddress = String.format("chn%de%d", channelId, elementId);
            } else {
                subAddress = String.format("chn%d", channelId);
            }
        } else {
            subAddress = "subslave";
        }
        return new ThingUID(thingTypeUID, bridgeHandler.getThing().getUID(), subAddress);
    }

    /**
     * Constructs the Label based on unitAddress, channelId and elementId if available
     * 
     * @param thingTypeUID
     * @param channelId
     * @param elementId
     * 
     * @return String
     */
    private String makeLabel(ThingTypeUID thingTypeUID, int channelId, int elementId) {
        String subAddress;
        if (channelId != ID_NOT_USED) {
            if (elementId != ID_NOT_USED) {
                subAddress = String.format("channel %d element %d", channelId, elementId);
            } else {
                subAddress = String.format("channel %d", channelId);
            }
        } else {
            subAddress = "(sub-slave)";
        }
        return thingTypeUID + " "  + subAddress;
    }
    /**
     * Get called when a device is discovered
     *
     * @param thingTypeUID
     * @param unitAddress
     * @param channelId    (ID_NOT_USED if not Jablotron)
     * @param elementId    (ID_NOT_USED if not Jablotron)
     */
    public void deviceDiscovered(ThingTypeUID thingTypeUID, ThingUID parentThingUID, int channelId, int elementId) {
        logger.debug("DeviceDiscovered: {} - parentNodeId {}", thingTypeUID, parentThingUID);

        try {
            String nodeClassLabel = thingTypeUID.getId();
   
            // Initialize it (create if absent)
            ThingUID thingUID = makeThingUID(thingTypeUID, channelId, elementId);
            String label = makeLabel(thingTypeUID, channelId, elementId);

            logger.debug("Adding new Modbus Slave Thing {} to smarthome inbox", thingUID);

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                    .withProperty(PROPERTY_NODECLASS, nodeClassLabel)
                    .withProperty(PROPERTY_PARENTTHINGUID, parentThingUID.getAsString())
                    .withProperty(PROPERTY_CHANNELID, String.valueOf(channelId))
                    .withProperty(PROPERTY_ELEMENTID, String.valueOf(elementId)).withLabel(label)
                    .withBridge(bridgeHandler.getThing().getUID()).build();
            thingDiscovered(discoveryResult);
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
    public void deviceDiscovered(ThingTypeUID thingType, ThingUID parentThingUID) {
        deviceDiscovered(thingType, parentThingUID, ID_NOT_USED, ID_NOT_USED);
    }

    private synchronized void discoverModbus() {

        // Initiate discovery for any subdevice on the node

        bridgeHandler.getThing().getThings().forEach(thing -> {
            ModbusThingHandler thingHandler = (ModbusThingHandler) thing.getHandler();
            logger.debug("Initiate discovery for any subdevice on the node {}", thing.getUID());
            if (thingHandler.getNodeClass().supportDiscovery()) {
                thingHandler.getModbusFunction().startSubDeviceDiscovery(thing.getUID());
            }
        });
    }

    public void stopDeviceDiscovery() {
    }
}
