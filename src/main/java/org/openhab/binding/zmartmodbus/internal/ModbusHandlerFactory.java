/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal;

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.BRIDGE_TYPE_RTU;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.BRIDGE_TYPE_SERIAL;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.BRIDGE_TYPE_TCP;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.SUPPORTED_BRIDGE_TYPES_UIDS;
import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.zmartmodbus.handler.ModbusBridgeHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusSerialHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusTcpHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusThingHandler;
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
 *
 */

@Component(immediate = true, service = ThingHandlerFactory.class, configurationPid = "binding.zmartmodbus")
public class ModbusHandlerFactory extends BaseThingHandlerFactory {
    private Logger logger = LoggerFactory.getLogger(BaseThingHandlerFactory.class);

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
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.info("Supported: {} = {}", thingTypeUID, SUPPORTED_THING_TYPES_UIDS);
        return SUPPORTED_BRIDGE_TYPES_UIDS.contains(thingTypeUID) ? true
                : SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    /**
     * Creates a handler for the specific thing. THis also creates the discovery service
     * when the bridge is created.
     */
    @Override
    protected ThingHandler createHandler(Thing thing) {
        logger.debug("CreateHandler for Thing {}", thing.getUID());

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPPORTED_BRIDGE_TYPES_UIDS.contains(thingTypeUID)) {
            // Handle Bridget controllers here
            ModbusBridgeHandler bridge = null;
            if (thingTypeUID.equals(BRIDGE_TYPE_SERIAL)) {
                bridge = new ModbusSerialHandler((Bridge) thing, serialPortManager);
            } else if (thingTypeUID.equals(BRIDGE_TYPE_TCP) || thingTypeUID.equals(BRIDGE_TYPE_RTU)) {
                bridge = new ModbusTcpHandler((Bridge) thing);
            }
            return bridge;
        } else if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            // Everything else gets handled in a single handler
            return new ModbusThingHandler(thing);
        }
        return null;
    }

}
