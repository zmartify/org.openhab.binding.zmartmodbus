/**
 * Copyright (c) 2014-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.factory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusReportOn;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusValueClass;
import org.openhab.binding.zmartmodbus.handler.ModbusThingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusDataSets {

    private Logger logger = LoggerFactory.getLogger(ModbusDataSets.class);

    private AtomicInteger dataSetCounter = new AtomicInteger(0);

    // Maintains a list of dataSetKeys (string name of dataset)
    private static ConcurrentHashMap<String, Integer> dataSetList = new ConcurrentHashMap<String, Integer>();

    // The dataSets lookup list
    private ConcurrentHashMap<Integer, ModbusDataSet> dataSets = new ConcurrentHashMap<Integer, ModbusDataSet>();

    // The channel to modbus channel lookup
    private ConcurrentHashMap<ChannelUID, ModbusThingChannel> channels = new ConcurrentHashMap<ChannelUID, ModbusThingChannel>();

    public ModbusDataSets() {

    }

    /**
     * Add a new channel
     * 
     * @param channel
     */
    public void addChannel(ModbusThingChannel channel) {
        int dataSetId = getDataSetId(channel.getDataSetKey());
        // If the dataSet is not created discard and return
        if (dataSetId == -1) {
            logger.error("NODE {}: DataSet {} not found", channel.getNodeId(), channel.getDataSetKey());
            return;
        }
        // Avoid duplication
        if (!channels.containsKey(channel.getUID())) {
            synchronized (channels) {
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
                // Add it to the dataset for lookup and save it in the channels map
                getDataSet(dataSetId).addChannel(channel.getUID());
                channels.put(channel.getUID(), channel);
            }
        }
    }

    public ModbusThingChannel getChannel(ChannelUID uid) {
        return channels.get(uid);
    }

    public void removeChannel(ChannelUID uid) {
        ModbusThingChannel channel = getChannel(uid);
        
        if (channel == null)
            return; // Channel not found

        getDataSet(channel.getDataSetKey()).removeChannel(uid);
        channels.remove(uid);
    }

    public ConcurrentHashMap<ChannelUID, ModbusThingChannel> getChannels() {
        synchronized (channels) {
            return (ConcurrentHashMap<ChannelUID, ModbusThingChannel>) Collections.unmodifiableMap(channels);
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

    public ConcurrentHashMap<String, Integer> getDataSetList() {
        return dataSetList;
    }

    /**
     * Return DataSetId based on dataSetKey
     * 
     * @param dataSetKey
     * @return
     */
    public static int getDataSetId(String dataSetKey) {
        return dataSetList.getOrDefault(dataSetKey, -1);
    }

    /**
     * Return all dataSets
     *
     * @return
     */
    public Collection<ModbusDataSet> getDataSets() {
        return dataSets.values();
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
}
