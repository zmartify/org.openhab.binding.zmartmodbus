/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
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
import org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants;
import org.openhab.binding.zmartmodbus.handler.ZmartModbusHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Peter Kristensen
 */
public class ModbusThingDiscoveryService extends AbstractDiscoveryService implements ExtendedDiscoveryService {

    private Logger logger = LoggerFactory.getLogger(ModbusThingDiscoveryService.class);

    private ZmartModbusHandler controllerHandler;
    private DiscoveryServiceCallback discoveryServiceCallback;

    public ModbusThingDiscoveryService(ZmartModbusHandler modbusControllerHandler, int searchTime) {
        super(searchTime);
        this.controllerHandler = modbusControllerHandler;
    }

    public void activate() {

    }

    @Override
    public void deactivate() {
    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {
        this.discoveryServiceCallback = discoveryServiceCallback;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return ZmartModbusBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS;
    }

    @Override
    protected void startScan() {
        logger.info("Starting ModbusFunction Node inclusion scan for {}", controllerHandler.getThing().getUID());
        // Start the search for new devices
        controllerHandler.startDeviceDiscovery();
    }

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
                        .withBridge(controllerHandler.getThing().getUID()).build();
                thingDiscovered(discoveryResult);
            }
        } catch (Exception e) {
            logger.debug("Error occurred during device discovery", e);
        }
    }

    public void deviceDiscovered(ThingTypeUID thingTypeUID, int unitAddress) {
        deviceDiscovered(thingTypeUID, unitAddress, ID_NOT_USED, ID_NOT_USED);
    }

    @Override
    protected void startBackgroundDiscovery() {
    }

    @Override
    protected void stopBackgroundDiscovery() {
    }

    public void discoverSomething() {
        ThingUID thingUID = null;

        if (discoveryServiceCallback.getExistingDiscoveryResult(thingUID) != null) {
        }

        if (discoveryServiceCallback.getExistingThing(thingUID) != null) {
        }

    }
}
