/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.factory;

import static org.openhab.binding.zmartmodbus.ZmartModbusBindingConstants.ID_NOT_USED;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusActionClass;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusFeedRepeat;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.ZmartModbusBindingClass.ModbusValueClass;
import org.openhab.binding.zmartmodbus.internal.listener.ActionListener;
import org.openhab.binding.zmartmodbus.internal.listener.StateListener;
import org.openhab.binding.zmartmodbus.internal.protocol.converter.ModbusBaseConverter;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusAction;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusMessage;
import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class ModbusFactory<T> {

    private Logger logger = LoggerFactory.getLogger(ModbusFactory.class);

    private StateListener stateSubscriber = null;
    private ActionListener actionSubscriber = null;

    public ModbusFactory() {
    }

    private AtomicInteger dataSetCounter = new AtomicInteger(0);

    // Maintains a list of dataSetKeys (string name of dataset)
    private static ConcurrentHashMap<String, Integer> dataSetList = new ConcurrentHashMap<String, Integer>();

    // The datasets
    private ConcurrentHashMap<Integer, ModbusDataSet> dataSets = new ConcurrentHashMap<Integer, ModbusDataSet>();

    private HashMultimap<Integer, ChannelUID> dataSetChannels = HashMultimap.create();

    // The channels
    private ConcurrentHashMap<ChannelUID, ModbusChannel> channels = new ConcurrentHashMap<ChannelUID, ModbusChannel>();

    public void addChannel(ModbusChannel channel) {
        int dataSetId = getDataSetId(channel.getDataSetKey());
        // If the dataSet is not created discard and return
        if (dataSetId == -1) {
            logger.error("NODE {}: DataSet {} not found", channel.getNodeId(), channel.getDataSetKey());
            return;
        }
        // Avoid duplication
        if (!channels.containsKey(channel.getUID())) {
            synchronized (dataSetChannels) {
                if (channel.getValueClass().equals(ModbusValueClass.Bit)) {
                    if ((channel.getValueClass().size() > getDataSet(dataSetId).getLength())
                            || (channel.getValueClass().size() <= 0)) {
                        logger.error("NODE {}: Channel coil index out-of-bound {}", channel.getNodeId(),
                                channel.getDataSetKey());
                        return;
                    }
                } else {
                    if ((channel.getValueClass().size() * channel.getIndex()) > (getDataSet(dataSetId).getLength()
                            * 2)) {
                        logger.error("NODE {}: Channel index out-of-bound {}", channel.getNodeId(),
                                channel.getDataSetKey());
                        return;
                    }
                }
                if (channel.getReportOn().equals(ModbusReportOn.Unknown)) {
                    channel.setReportOn(getDataSet(dataSetId).getReportOn());
                }
                dataSetChannels.put(dataSetId, channel.getUID());
                channels.put(channel.getUID(), channel);
            }
        }
    }

    public ModbusChannel getChannel(ChannelUID uid) {
        return channels.get(uid);
    }

    public ImmutableSet<ModbusChannel> getChannels() {
        synchronized (channels) {
            return ImmutableSet.copyOf(channels.values());
        }
    }

    public void removeChannels(int nodeId) {
        channels.values().forEach(channel -> {
            if (channel.getNodeId() == nodeId) {
                channels.remove(channel.getUID());
            }
        });
    }

    /**
     * addDataSet - adds the dataSet to the list of datasets for modbushandling
     *
     * @param dataSetKey
     * @param dataSet
     */
    public void addDataSet(String dataSetKey, ModbusDataSet dataSet) {
        synchronized (dataSetList) {
            if (!dataSetList.containsKey(dataSetKey)) {
                synchronized (dataSets) {
                    int dataSetId = dataSetCounter.getAndIncrement();
                    dataSetList.put(dataSetKey, dataSetId);
                    dataSet.setDataSetId(dataSetId);
                    dataSets.put(dataSetId, dataSet);
                }
            }

        }
    }

    /**
     * Return dataSet based on dataSetId
     *
     * @param dataSetId
     * @return
     */
    public ModbusDataSet getDataSet(int dataSetId) {
        return dataSets.get(dataSetId);
    }

    /**
     * Return daaSet based on dataSetKey
     *
     * @param dataSetKey
     * @return
     */
    public ModbusDataSet getDataSet(String dataSetKey) {
        return dataSets.get(dataSetList.get(dataSetKey));
    }

    /**
     * Return all dataSets
     *
     * @return
     */
    public Collection<ModbusDataSet> getDataSets() {
        return dataSets.values();
    }

    public void requestDataSetUpdateByElementId(int elementId) {
        for (ModbusDataSet dataSet : dataSets.values()) {
            if (dataSet.getElementId() == elementId) {
                // Initiate a read of all the dataSet information to check for updates
                actionSubscriber.modbusAction(new ModbusAction(dataSet, ModbusActionClass.Read, ModbusFeedRepeat.Once));
            }
        }
    }

    public void requestDataSetUpdateByChannelId(int channelId) {
        for (ModbusDataSet dataSet : dataSets.values()) {
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
        // Update all channels, which are not linked to element or channel ---> controller
        for (ModbusDataSet dataSet : dataSets.values()) {
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

    /**
     * Remove all the nodes dataSets from the list of Modbus managed dataSets
     *
     * @param nodeId
     */
    public void removeDataSets(int nodeId) {
        synchronized (dataSets) {
            dataSets.values().forEach(dataSet -> {
                if (dataSet.getNodeId() == nodeId) {
                    for (Entry<String, Integer> dataSetListEntry : dataSetList.entrySet()) {
                        if (dataSetListEntry.getValue() == dataSet.getDataSetId()) {
                            dataSetList.remove(dataSetListEntry.getKey());
                        }
                    }
                    dataSets.remove(dataSet.getDataSetId());
                }
            });
        }
    }

    public Observer<ModbusState> stateListener() {
        return new Observer<ModbusState>() {

            @Override
            public void onSubscribe(Disposable d) {
                logger.info(" onSubscribe : {}", d.isDisposed());
            }

            @Override
            public void onComplete() {
                // TODO Auto-generated method stub
            }

            @Override
            public void onError(Throwable arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onNext(ModbusState modbusState) {
                logger.debug("Factory received state change {}", modbusState.getState());

                synchronized (channels) {
                    ModbusChannel channel = getChannel(modbusState.getUid());
                    if (channel != null) {
                        actionSubscriber.modbusAction(new ModbusAction(getDataSet(channel.getDataSetKey()),
                                channel.getIndex(), ModbusActionClass.Write, ModbusFeedRepeat.Once, ModbusBaseConverter
                                        .fromStateToModbus(modbusState.getUid(), modbusState.getState(), channel)));
                    }
                }

            }
        };
    }

    /**
     * Observer - handles all modbusMessages and decides, whether there is a need to initiate updates within the dataset
     *
     * @return
     */
    public Observer<ModbusMessage> messageListener() {
        return new Observer<ModbusMessage>() {

            @Override
            public void onSubscribe(Disposable d) {
                logger.info(" onSubscribe : {}", d.isDisposed());
            }

            @Override
            public void onComplete() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(Throwable arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onNext(ModbusMessage modbusMessage) {
                int dataSetId = modbusMessage.getDataSetId();
                if (getDataSet(dataSetId).getReportOn().equals(ModbusReportOn.Change)) {
                    if (!getDataSet(dataSetId).getPayload().equals(modbusMessage.getPayload())) {
                        // Update as there is a change
                        updateDataSet(modbusMessage);
                    }
                } else {
                    // Allways update
                    updateDataSet(modbusMessage);
                }
            }
        };
    }

    /**
     * updateDataSet - performs the actual update of datasets received by the Observer from Modbus
     *
     * @param modbusMessage
     */
    public void updateDataSet(ModbusMessage modbusMessage) {
        synchronized (dataSetChannels) {
            dataSetChannels.get(modbusMessage.getDataSetId()).forEach(uid -> {
                ModbusChannel channel = getChannel(uid);
                BitVector payload = null;

                // Handle special datasets
                switch (channel.getValueClass()) {
                    case Jablotron_elementChangeFlags:
                        payload = (BitVector) modbusMessage.getPayload();
                        // logger.debug("elementChangeFlags: {}", payload.toString());
                        for (int elementId = 0; elementId < 48; elementId++) {
                            if (payload.getBit(elementId)) {
                                requestDataSetUpdateByElementId(elementId);
                            }

                        }
                        break;
                    case Jablotron_channelChangeFlags:
                        payload = (BitVector) modbusMessage.getPayload();
                        // logger.debug("channelChangeFlags: {}", payload.toString());
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
                        // logger.debug("packetdataChangeFlags: {}", payload.toString());
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
                        channel.updateState(ModbusBaseConverter.fromModbusToState(channel.getValueClass(),
                                modbusMessage.getPayload(), channel.getIndex()));
                        if (channel.stateChanged() || channel.getReportOn().equals(ModbusReportOn.Allways)) {
                            stateSubscriber.modbusState(new ModbusState(uid, channel.getState()));
                        }
                }
            });
            // Save payload for change control
            getDataSet(modbusMessage.getDataSetId()).setPayload(modbusMessage.getPayload());
        }
    }

    public static int getDataSetId(String dataSetKey) {
        return dataSetList.getOrDefault(dataSetKey, -1);
    }

    /*
     * register ModbusMessages to listen for
     *
     */
    public void register(StateListener listener) {
        logger.info("Factory register State listener");
        stateSubscriber = listener;
    }

    public void register(ActionListener listener) {
        logger.info("Factory register Action listener");
        actionSubscriber = listener;
    }

}
