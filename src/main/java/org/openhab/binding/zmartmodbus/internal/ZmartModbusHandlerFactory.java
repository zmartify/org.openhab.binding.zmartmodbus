/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants.*;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants;
import org.openhab.binding.zmartmodbus.handler.ModbusSerialHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusTcpHandler;
import org.openhab.binding.zmartmodbus.handler.ModbusThingHandler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZmartModbusHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.zmartmodbus")
public class ZmartModbusHandlerFactory extends BaseThingHandlerFactory {
    private Logger logger = LoggerFactory.getLogger(BaseThingHandlerFactory.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.debug("Z: {}", thingTypeUID);
        if (thingTypeUID.equals(ZmartModbusBindingConstants.MODBUS_THING_UID)) {
            return true;
        }
        return ZmartModbusBindingConstants.BINDING_ID.equals(thingTypeUID.getBindingId());
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        logger.debug("Creating thing {}", thing.getUID());

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        // Handle controllers here
        if (thingTypeUID.equals(CONTROLLER_SERIAL)) {
            return new ModbusSerialHandler((Bridge) thing);
        } else if (thingTypeUID.equals(CONTROLLER_TCP) || thingTypeUID.equals(CONTROLLER_RTU)) {
            return new ModbusTcpHandler((Bridge) thing);
        }
        // Everything else gets handled in a single handler
        return new ModbusThingHandler(thing);
    }

}
