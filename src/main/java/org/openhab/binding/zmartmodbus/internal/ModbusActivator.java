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
package org.openhab.binding.zmartmodbus.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator to register the zwave service providers
 *
 * @author Chris Jackson
 * @author Peter Kristensen - adapted for ZmartModbus
 */
public final class ModbusActivator implements BundleActivator {

    private final Logger logger = LoggerFactory.getLogger(ModbusActivator.class);

    private static BundleContext context;

    /**
     * Called whenever the OSGi framework starts our bundle
     *
     * @param bc the bundle's execution context within the framework
     */
    @Override
    public void start(BundleContext bc) throws Exception {
        context = bc;
        logger.debug("ZmartModbus binding started. Version {}", ModbusActivator.getVersion());
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     *
     * @param bc the bundle's execution context within the framework
     */
    @Override
    public void stop(BundleContext bc) throws Exception {
        context = null;
        logger.debug("ZmartModbus binding stopped.");
    }

    /**
     * Returns the bundle context of this bundle
     *
     * @return the bundle context
     */
    public static BundleContext getContext() {
        return context;
    }

    /**
     * Returns the current version of the bundle.
     *
     * @return the current version of the bundle.
     */
    public static Version getVersion() {
        return context.getBundle().getVersion();
    }

}
