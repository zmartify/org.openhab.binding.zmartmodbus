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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
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
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunction;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusFunctionJablotron;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
import org.openhab.binding.zmartmodbus.internal.util.Jablotron;
import org.openhab.binding.zmartmodbus.internal.util.Register;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 */

public class ModbusThingHandlerJablotron extends ModbusThingHandler {


    private Logger logger = LoggerFactory.getLogger(ModbusThingHandler.class);

    protected ModbusFunction modbusFunction = new ModbusFunctionJablotron();

    public ModbusThingHandlerJablotron(Thing modbusDevice) {
        super(modbusDevice);
        logger.debug("ModbusThingHandler Jablotron");
    }
}