/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.factory;

import java.util.ArrayList;
import java.util.List;

import org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Peter Kristensen
 *
 * @param <T>
 */
public class ModbusActionFeed<T> {

    private Logger logger = LoggerFactory.getLogger(ModbusActionFeed.class);

    private List<ModbusAction> slowActions = new ArrayList<ModbusAction>();
    private List<ModbusAction> fastActions = new ArrayList<ModbusAction>();

    private ActionListener subscriber = null;

    private int slowPoll = ZmartModbusBindingConstants.DEFAULT_POLLS * 10;
    private int fastPoll = ZmartModbusBindingConstants.DEFAULT_POLLS;

    private transient boolean running = false;

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
                logger.debug("Action FeedRepeat not found {}", action.getDataSetId());
                break;
        }
    }

    public void removeActions(int nodeId) {
        synchronized (slowActions) {
            slowActions.removeIf(action -> action.getNodeId() == nodeId);
        }
        synchronized (fastActions) {
            fastActions.removeIf(action -> action.getNodeId() == nodeId);
        }
    }

    public ModbusActionFeed() {
        launchPublisher();
    }

    private class ActionThread extends Thread {

        ActionThread() {
            super("ModbusActionThread");
        }

        @Override
        public void run() {
            int fastTicker = 0;
            try {
                while (running) {
                    // Slow Actions are running every slowPoll <= (fastPoll * n) interval
                    synchronized (slowActions) {
                        slowActions.forEach(slowAction -> {
                            subscriber.modbusAction(slowAction);
                        });
                    }
                    fastTicker = 0;
                    while (running && (fastTicker < slowPoll)) {
                        synchronized (fastActions) {
                            fastActions.forEach(fastAction -> {
                                subscriber.modbusAction(fastAction);
                            });
                            fastTicker = fastTicker + fastPoll;
                        }
                        Thread.sleep(fastPoll);
                    }
                }
            } catch (InterruptedException e) {
                // TODO: Auto-generated catch block
                e.printStackTrace();
            }
            logger.info("Leaving THREAD");
        }
    }

    void launchPublisher() {
        logger.info("LaunchPublisher");
        running = true;
        Thread actionThread = new ActionThread();
        actionThread.start();
    }

    public void setSlowPoll(int slowPoll) {
        this.slowPoll = slowPoll;
    }

    public void setFastPoll(int fastPoll) {
        this.fastPoll = fastPoll;
    }

    public void terminate() {
        running = false;
    }

    public void register(ActionListener listener) {
        logger.info("register actionListener");
        subscriber = listener;
    }
}
