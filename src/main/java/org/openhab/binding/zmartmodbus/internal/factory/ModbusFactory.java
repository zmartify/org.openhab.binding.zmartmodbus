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

import static org.openhab.binding.zmartmodbus.ModbusBindingConstants.ID_NOT_USED;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusActionClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusValueClass;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusThingChannel;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.listener.StateListener;
import org.openhab.binding.zmartmodbus.internal.protocol.converter.ModbusBaseConverter;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 *
 * @author Peter Kristensen - Initial contribution
 *
 * @param <T>
 */

public class ModbusFactory {

    private final Logger logger = LoggerFactory.getLogger(ModbusFactory.class);

    private final ModbusDataSets dataSets = new ModbusDataSets();

    @Nullable
    private StateListener stateSubscriber = null;
    @Nullable
    private ActionListener actionSubscriber = null;

    public ModbusFactory() {
    }

    public void requestDataSetUpdateByElementId(final int elementId) {
        for (final ModbusDataSet dataSet : dataSets.getDataSets()) {
            if (dataSet.getElementId() == elementId) {
                // Initiate a read of all the dataSet information to check for updates
                actionSubscriber.modbusAction(new ModbusAction(dataSet, ModbusActionClass.Read, ModbusFeedRepeat.Once));
            }
        }
    }

    public void requestDataSetUpdateByChannelId(final int channelId) {
        for (final ModbusDataSet dataSet : dataSets.getDataSets()) {
            if (dataSet.getChannelId() == channelId) {
                // only update pure channels, elements will get update anyway via ElementID
                if (dataSet.getElementId() == ID_NOT_USED) {
                    // Initiate a read of all the dataSet information to check for updates
                    actionSubscriber
                            .modbusAction(new ModbusAction(dataSet, ModbusActionClass.Read, ModbusFeedRepeat.Once));
                }
            }
        }
    }

    public void requestDataSetUpdateController() {
        // Update all channels, which are not linked to element or channel --->
        // controller
        for (final ModbusDataSet dataSet : dataSets.getDataSets()) {
            if (dataSet.getChannelId() == ID_NOT_USED) {
                // only update pure channels, elements will get update anyway via ElementID
                if (dataSet.getElementId() == ID_NOT_USED) {
                    // Initiate a read of all the dataSet information to check for updates
                    actionSubscriber
                            .modbusAction(new ModbusAction(dataSet, ModbusActionClass.Read, ModbusFeedRepeat.Once));
                }
            }
        }
    }

    public Observer<ModbusState> stateListener() {
        return new Observer<ModbusState>() {

            @Override
            public void onSubscribe(final Disposable d) {
                logger.debug("StateListener onSubscribe : {}", d.isDisposed());
            }

            @Override
            public void onComplete() {
                logger.debug("StateListener completed");
            }

            @Override
            public void onError(final Throwable arg0) {
                logger.error("StateListener caught an error : {}", arg0.getMessage());
            }

            @Override
            public void onNext(final ModbusState modbusState) {
                logger.debug("Factory received state change {}", modbusState.getState());

                synchronized (dataSets.getChannels()) {
                    final ModbusThingChannel channel = dataSets.getChannel(modbusState.getUid());
                    if (channel != null) {
                        ModbusAction action;

                        action = new ModbusAction(dataSets.getDataSet(channel.getDataSetKey()), channel.getIndex(),
                                ModbusActionClass.Write, ModbusFeedRepeat.Once,
                                ModbusBaseConverter.fromStateToModbus(modbusState.getState(), channel));

                        if (ModbusValueClass.Jablotron_modeset.equals(channel.getValueClass())) {
                            // In case modeset we need to write 4 bits as coils
                            // - work around, not the most clever way to do it ;-)
                            action.setLength(4);
                        }

                        actionSubscriber.modbusAction(action);
                    }
                }

            }
        };
    }

    /**
     * Observer - handles all modbusMessages and decides, whether there is a need to
     * initiate updates within the dataset
     *
     * @return
     */
    public Observer<ModbusMessage> messageListener() {
        return new Observer<ModbusMessage>() {

            @Override
            public void onSubscribe(final Disposable d) {
                logger.debug("MessageListener onSubscribe : {}", d.isDisposed());
            }

            @Override
            public void onComplete() {
                logger.debug("MessageListener completed");
            }

            @Override
            public void onError(final Throwable arg0) {
                logger.error("MessageListener caught an error : {}", arg0.getMessage());
            }

            @Override
            public void onNext(final ModbusMessage modbusMessage) {
                final int dataSetId = modbusMessage.getDataSetId();
                if (dataSets.getDataSet(dataSetId).getReportOn().equals(ModbusReportOn.Change)) {
                    if (!dataSets.getDataSet(dataSetId).getPayload().equals(modbusMessage.getPayload())) {
                        // Update as there is a change
                        updateDataSet(modbusMessage);
                    }
                } else {
                    // Always update
                    updateDataSet(modbusMessage);
                }
            }
        };
    }

    /**
     * updateDataSet - performs the actual update of datasets received by the
     * Observer from Modbus
     *
     * @param modbusMessage
     */
    public void updateDataSet(final ModbusMessage modbusMessage) {

        synchronized (dataSets) {
            dataSets.getDataSet(modbusMessage.getDataSetId()).getChannels().forEach(uid -> {
                final ModbusThingChannel channel = dataSets.getChannel(uid);
                BitVector payload = null;
                // Handle special datasets
                switch (channel.getValueClass()) {
                case Jablotron_elementChangeFlags:
                    payload = (BitVector) modbusMessage.getPayload();
                    logger.debug("elementChangeFlags: {}", payload.toString());
                    for (int elementId = 0; elementId < 48; elementId++) {
                        if (payload.getBit(elementId)) {
                            requestDataSetUpdateByElementId(elementId);
                        }
                    }
                    break;
                case Jablotron_channelChangeFlags:
                    payload = (BitVector) modbusMessage.getPayload();
                    logger.debug("channelChangeFlags: {}", payload.toString());
                    for (int channelId = 0; channelId < 16; channelId++) {
                        if (payload.getBit(channelId)) {
                            requestDataSetUpdateByChannelId(channelId);
                        }
                    }
                    if (payload.getBit(16)) {
                        requestDataSetUpdateController();
                    }
                    break;
                case Jablotron_packetdataChangeFlags:
                    payload = (BitVector) modbusMessage.getPayload();
                    logger.debug("packetdataChangeFlags: {}", payload.toString());
                    for (int channelId = 0; channelId < 16; channelId++) {
                        if (payload.getBit(channelId)) {
                            // requestDataSetUpdateByChannelId(channelId);
                        }
                    }
                    if (payload.getBit(16)) {
                        logger.info("*** PACKETDATA CHANGE ON CONTROLLER SIDE ***");
                    }

                    break;

                default:
                    // Handle normal situations
                    channel.updateState(ModbusBaseConverter.fromModbusToState(channel, modbusMessage.getPayload()));
                    if (channel.stateChanged() || channel.getReportOn().equals(ModbusReportOn.Always)) {
                        stateSubscriber.modbusState(new ModbusState(uid, channel.getState()));
                    }
                }
            });
            // Save payload for change control
            dataSets.getDataSet(modbusMessage.getDataSetId()).setPayload(modbusMessage.getPayload());
        }
    }

    public int getDataSetId(final String dataSetKey) {
        return dataSets.getDataSetList().getOrDefault(dataSetKey, -1);
    }

    public ModbusDataSets getDataSets() {
        return dataSets;
    }

    /*
     * register ModbusMessages to listen for
     *
     */
    public void register(final StateListener listener) {
        logger.debug("Factory register State listener registered");
        stateSubscriber = listener;
    }

    public void register(final ActionListener listener) {
        logger.debug("Factory register Action listener registered");
        actionSubscriber = listener;
    }

}
