/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.zmartmodbus.internal;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusNodeClass;
import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusThingHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusThingHandlerJablotron;
import org.openhab.binding.zmartmodbus.internal.discovery.ModbusSlaveDiscoveryService;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusHandlerFactory} is responsible for creating things and thing
 * handlers. It also sets up the discovery service to track things from the
 * bridge when the bridge is created.
 *
 * @author Peter Kristensen - Initial contribution
 * @param <ThingUID>
 *
 */

@NonNullByDefault
@Component(immediate = true, service = ThingHandlerFactory.class, configurationPid = CONFIGURATION_PID)
public class ModbusHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(ModbusHandlerFactory.class);

    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private @NonNullByDefault({}) SerialPortManager serialPortManager;

    @Reference
    protected void setSerialPortManager(final SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    protected void unsetSerialPortManager(final SerialPortManager serialPortManager) {
        this.serialPortManager = null;
    }

    /**
     * The things this factory supports creating
     */
    @Override
    public boolean supportsThingType(final ThingTypeUID thingTypeUID) {
        return SUPPORTED_BRIDGE_TYPES_UIDS.contains(thingTypeUID) ? true
                : SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    /**
     * Creates a handler for the specific thing. THis also creates the discovery
     * service when the bridge is created.
     */
    @Override
    @Nullable
    public ThingHandler createHandler(final Thing thing) {
        logger.debug("CreateHandler for Thing {}", thing.getUID());

        ModbusBridgeHandler controller = null;

        final ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPPORTED_BRIDGE_TYPES_UIDS.contains(thingTypeUID)) {
            // Handle Bridge controllers here
            if (thingTypeUID.equals(BRIDGE_TYPE_SERIAL)) {
                controller = new ModbusBridgeHandler((Bridge) thing, serialPortManager);
            }

            if (controller != null) {
                final ModbusSlaveDiscoveryService discoveryService = new ModbusSlaveDiscoveryService(controller, 60);
                discoveryService.activate();

                discoveryServiceRegs.put(controller.getThing().getUID(), bundleContext.registerService(
                        DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));

                controller.setDiscoveryService(discoveryService);

                return controller;
            }
        } else if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {

            switch (ModbusNodeClass.fromString(thingTypeUID.getId())) {
            case JablotronAC116:
            case JablotronActuator:
            case JablotronTP150:
                return new ModbusThingHandlerJablotron(thing);
            default:
            logger.info("NEW STANDARD MODBUS");
                // Everything else gets handled in a single handler
                return new ModbusThingHandler(thing);
            }
        }
        return null;
    }

}
