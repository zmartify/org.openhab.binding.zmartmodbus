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
package org.openhab.binding.zmartmodbus.internal.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openhab.core.thing.ThingUID;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 * @param <T>
 */

public class ModbusActionFeed<T> {

    private Logger logger = LoggerFactory.getLogger(ModbusActionFeed.class);

    private List<ModbusAction> slowActions = Collections.synchronizedList(new ArrayList<ModbusAction>());
    private List<ModbusAction> fastActions = Collections.synchronizedList(new ArrayList<ModbusAction>());

    private ActionListener subscriber = null;

    public ModbusActionFeed() {
    }

    public void addAction(ModbusAction action) {
        switch (action.getFeedRepeat()) {
            case Once:
                // Put it on to the queue and run immediately
                subscriber.modbusAction(action);
                break;
            case Slow:
                synchronized (slowActions) {
                    slowActions.add(action);
                }
                break;
            case Fast:
                synchronized (fastActions) {
                    fastActions.add(action);
                }
                break;
            default:
                logger.error("Action FeedRepeat not found {}", action.getDataSetId());
                break;
        }
    }

    public void removeActions(ThingUID thingUID) {
        synchronized (slowActions) {
            slowActions.removeIf(action -> action.getThingUID().equals(thingUID));
        }
        synchronized (fastActions) {
            fastActions.removeIf(action -> action.getThingUID().equals(thingUID));
        }
    }

    public void execSlowActions() {
        logger.debug("execSlowActions ({})", slowActions.size());
        synchronized(slowActions) {
            slowActions.forEach(action -> {
                subscriber.modbusAction(action);
            });
        }
    }

    public void execFastActions() {
        logger.debug("execFastActions ({})", fastActions.size());
        synchronized(fastActions) {
            fastActions.forEach(action -> {
                subscriber.modbusAction(action);
            });
        }
    }

    public void register(ActionListener listener) {
        subscriber = listener;
    }

}
