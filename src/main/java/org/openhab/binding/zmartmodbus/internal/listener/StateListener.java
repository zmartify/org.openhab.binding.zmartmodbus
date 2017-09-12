package org.openhab.binding.zmartmodbus.internal.listener;

import java.util.concurrent.atomic.AtomicInteger;

import org.openhab.binding.zmartmodbus.internal.streams.ModbusState;

public abstract class StateListener {
    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private final int ID;

    public StateListener() {
        ID = COUNTER.getAndIncrement();
    }

    public abstract void modbusState(ModbusState event);

    public abstract void error(Throwable throwable);

    @Override
    public String toString() {
        return String.format("Listener ID:%d:%s", ID, super.toString());
    }
}
