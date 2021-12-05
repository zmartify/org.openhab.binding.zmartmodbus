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
package org.openhab.binding.zmartmodbus.handler;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zmartmodbus.ModbusBindingConstants;
import org.openhab.binding.zmartmodbus.internal.config.ModbusBridgeConfiguration;
import org.openhab.binding.zmartmodbus.internal.config.ModbusSerialConfiguration;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusController;
import org.openhab.binding.zmartmodbus.internal.discovery.ModbusSlaveDiscoveryService;
import org.openhab.binding.zmartmodbus.internal.exceptions.ModbusProtocolException;
import org.openhab.binding.zmartmodbus.internal.listener.StateListener;
import org.openhab.binding.zmartmodbus.internal.protocol.IModbusIOHandler;
import org.openhab.binding.zmartmodbus.internal.protocol.ModbusCounters;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;
import org.openhab.binding.zmartmodbus.internal.transceiver.ModbusSerialTransceiver;
import org.openhab.binding.zmartmodbus.internal.transceiver.ModbusTransceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * The {@link ModbusBridgeHandler} is responsible for handling commands, which
 * are sent to one of the channels.
 *
 * @author Peter Kristensen - Initial contribution
 */
public class ModbusBridgeHandler extends BaseBridgeHandler implements IModbusIOHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusBridgeHandler.class);

    protected ModbusBridgeConfiguration modbusBridgeConfig;
    protected ModbusTransceiver transceiver = null;
    protected ModbusController controller;
    protected ModbusCounters counters = new ModbusCounters();

    // Checks if msgCounter should update to SmartHome - can be set from UI
    private Disposable updateCounterDisposable = null;

    private StateListener stateSubscriber = null;

    private ScheduledFuture<?> connectorTask;

    private ScheduledFuture<?> slowPollTask;
    private ScheduledFuture<?> fastPollTask;

    private ModbusSerialConfiguration modbusSerialConfig;

    private SerialPortManager serialPortManager;

    private ModbusSlaveDiscoveryService discoveryService;

    public ModbusBridgeHandler(Bridge thing, SerialPortManager serialPortManager) {
        super(thing);
        this.serialPortManager = serialPortManager;
    }

    public ModbusSlaveDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public void setDiscoveryService(ModbusSlaveDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING, "trying to connect to gateway...");

        logger.debug("Initializing Modbus BridgeHandler : {}", thing.getUID());
        modbusBridgeConfig = getConfigAs(ModbusBridgeConfiguration.class);
        modbusSerialConfig = getConfigAs(ModbusSerialConfiguration.class);

        // Create a new IOController for this Bridge
        controller = new ModbusController(this);

        if (connectorTask == null || connectorTask.isDone()) {
            connectorTask = scheduler.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {
                    if (thing.getStatus() != ThingStatus.ONLINE) {
                        initTransceiver();
                    }
                }

            }, 0, 60, TimeUnit.SECONDS);
        }
    }

    protected void initTransceiver() {

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING, "Opening serial port");
        if (transceiver != null) {
            transceiver.disconnect();
        }
        transceiver = new ModbusSerialTransceiver(serialPortManager, modbusSerialConfig, counters);

        try {
            logger.debug("We wil now connect and then initialize the Network");
            transceiver.connect();
            initializeCounters();
            initializeActionFeeds();
            updateStatus(ThingStatus.ONLINE);
        } catch (ModbusProtocolException e) {
            logger.error("IOException {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
            return;
        }
    }

    @Override
    public void dispose() {
        logger.debug("Dispose Bridge called : {}", thing.getBridgeUID());

        // Cancel all slow running Modbus actions
        if (slowPollTask != null && !slowPollTask.isDone()) {
            slowPollTask.cancel(true);
        }

        // Cancel all fast running Modbus actions
        if (fastPollTask != null && !fastPollTask.isDone()) {
            fastPollTask.cancel(true);
        }

        if ((updateCounterDisposable != null) && !updateCounterDisposable.isDisposed()) {
            updateCounterDisposable.dispose();
        }

        if (transceiver != null) {
            transceiver.disconnect();
            transceiver = null;
        }

        if (connectorTask != null && !connectorTask.isDone()) {
            connectorTask.cancel(true);
            connectorTask = null;
        }

        if (getController() != null) {
            getController().stopListening();
            controller = null;
        }

        super.dispose();
    }

    /**
     * Common initialization point for all ModbusFunction controllers. Called by
     * bridges after they have initialized their interfaces.
     *
     * @throws ModbusProtocolException
     *
     */
    private void initializeActionFeeds() {
        logger.debug("Starting action feeds slow = {} - fast = {}", modbusBridgeConfig.getSlowPoll(),
                modbusBridgeConfig.getFastPoll());

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING, "Starting action feeds");

        // Make an initial slow run to get data filled in
        getController().getActionFeed().execSlowActions();

        // Hereafter we schedule it with SlowPool delay
        slowPollTask = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                getController().getActionFeed().execSlowActions();
            }
        }, 0, modbusBridgeConfig.getSlowPoll(), TimeUnit.SECONDS);

        // Make an initial fast run to get data filled in
        getController().getActionFeed().execFastActions();

        // Hereafter we schedule it with FastPoll delay
        fastPollTask = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                getController().getActionFeed().execFastActions();
            }
        }, 0, modbusBridgeConfig.getFastPoll(), TimeUnit.SECONDS);

        getController().startListening();
    }

    private void initializeCounters() {
        if (updateCounterDisposable != null) {
            if (!updateCounterDisposable.isDisposed())
                updateCounterDisposable.dispose();
        }
        counters.clearCounters();
        updateCounterDisposable = Observable
                .interval(modbusSerialConfig.getTimeBetweenCounterUpdates(), TimeUnit.SECONDS)
                .doOnNext(n -> refreshCounters()).subscribe();
    }

    /**
     * Refresh counters in OpenHAB
     */
    private void refreshCounters() {
        updateState(new ChannelUID(getThing().getUID(), ModbusBindingConstants.CHANNEL_MESSAGE_COUNT),
                new DecimalType(counters.getMessageCounter()));
        updateState(new ChannelUID(getThing().getUID(), ModbusBindingConstants.CHANNEL_TIMEOUT_COUNT),
                new DecimalType(counters.getTimeOutCounter()));
        updateState(new ChannelUID(getThing().getUID(), ModbusBindingConstants.CHANNEL_FAILED_COUNT),
                new DecimalType(counters.getFailedCounter()));
    }

    public boolean isConnected() {
        return (transceiver != null) ? transceiver.isConnected() : false;
    }

    public boolean getListening() {
        return getController().getListening();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("ModbusBridgeHandler.handlecommand: {} {}", channelUID.getIdWithoutGroup(), command);
        // Push it on the queue
        if (command instanceof RefreshType) {
            // We do not support REFRESH
        } else {
            stateSubscriber.modbusState(new ModbusState(channelUID, (State) command));
        }
    }

    public void handleUpdate(ChannelUID uid, State state) {
        updateState(uid, state);
    }

    // Function to convert from ModbusState package to SmartHome state
    public void handleUpdate(ModbusState modbusState) {
        handleUpdate(modbusState.getUid(), modbusState.getState());
    }

    @Override
    public void handleRemoval() {
        logger.debug("Called handleRemoval");
        dispose();
        updateStatus(ThingStatus.REMOVED);
    }

    protected void incomingMessage() {
        if (getController() == null) {
            return;
        }
    }

    /**
     *
     * @param listener
     */
    public void register(StateListener listener) {
        stateSubscriber = listener;
    }

    public void setConnected(boolean state) {
        getController().setConnected(state);
    }

    public void setListening(boolean listening) {
        getController().setListening(listening);
    }

    public void stopDeviceDiscovery() {
        if (getController() == null) {
            return;
        }
    }

    public ModbusBridgeConfiguration getModbusBridgeConfig() {
        return modbusBridgeConfig;
    }

    public ModbusController getController() {
        return controller;
    }

    protected ModbusBridgeHandler getBridgeHandler() {
        return this;
    }

    public ModbusCounters getCounters() {
        return counters;
    }

    public ModbusThingHandler getThingHandlerByUID(ThingUID thingUID) {
        Thing thing = getThing().getThing(thingUID);
        return (thing != null) ? (ModbusThingHandler) thing.getHandler() : null;
    }

    protected void onSuccessfulOperation() {
        // update without error -> we're back online
        if (getThing().getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public ModbusTransceiver getTransceiver() {
        return transceiver;
    }

    public ModbusSerialConfiguration getModbusSerialConfig() {
        return modbusSerialConfig;
    }

    /**
     * Finds and returns a child thing for a given UID of this bridge.
     *
     * @param uid uid of the child thing
     * @return child thing with the given uid or null if thing was not found
     */
    public @Nullable Thing getThingByUID(ThingUID uid) {
        Bridge bridge = getThing();
        List<Thing> things = bridge.getThings();
        for (Thing thing : things) {
            if (thing.getUID().equals(uid)) {
                return thing;
            }
        }
        return null;
    }
}
