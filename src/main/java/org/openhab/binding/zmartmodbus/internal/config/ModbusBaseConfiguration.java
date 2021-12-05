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
package org.openhab.binding.zmartmodbus.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ModbusSlaveConfiguration} is the base class for configuration
 * information held by the device
 *
 * @author Peter Kristensen - Initial contribution
 *
 */
@NonNullByDefault
public abstract class ModbusBaseConfiguration {
    private int id;
    
    private boolean enableDiscovery = false;

    public int getId() {
        return id;
    }

    public boolean isDiscoveryEnabled() {
        return enableDiscovery;
    }

    public void setDiscoveryEnabled(boolean enableDiscovery) {
        this.enableDiscovery = enableDiscovery;
    }

}
