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
package org.openhab.binding.zmartmodbus.internal.protocol.converter;

import static org.openhab.binding.zmartmodbus.internal.util.Register.floatToRegisters;
import static org.openhab.binding.zmartmodbus.internal.util.Register.floatToRegistersSwap;
import static org.openhab.binding.zmartmodbus.internal.util.Register.intToRegisters;
import static org.openhab.binding.zmartmodbus.internal.util.Register.intToRegistersSwap;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registerToShort;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registerToUnsignedShort;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registersToFloat;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registersToFloatSwap;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registersToInt;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registersToIntSwap;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registersToUnsignedInt;
import static org.openhab.binding.zmartmodbus.internal.util.Register.registersToUnsignedIntSwap;
import static org.openhab.binding.zmartmodbus.internal.util.Register.shortToRegister;
import static org.openhab.binding.zmartmodbus.internal.util.Register.unsignedByteToInt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.ModbusValueClass;
import org.openhab.binding.zmartmodbus.ModbusBindingClass.WeekDayClass;
import org.openhab.binding.zmartmodbus.internal.controller.ModbusThingChannel;
import org.openhab.binding.zmartmodbus.internal.util.BitVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Peter Kristensen - Initial contribution
 *
 */

public class ModbusBaseConverter {

    private final static Logger logger = LoggerFactory.getLogger(ModbusBaseConverter.class);

    public static State fromModbusToState(ModbusValueClass valueClass, Object payload, int channelIndex) {
        int index = channelIndex * valueClass.size();
        State state = UnDefType.UNDEF;

        switch (valueClass) {
            case Bit:
                if (((BitVector) payload).getBit(channelIndex) == true) {
                    state = OnOffType.ON;
                } else {
                    state = OnOffType.OFF;
                }
                break;
            case Int8:
                state = new DecimalType(unsignedByteToInt(((byte[]) payload)[index]));
                break;
            case Uint16:
                state = new DecimalType(registerToUnsignedShort((byte[]) payload, index));
                break;
            case Int16:
                state = new DecimalType(registerToShort((byte[]) payload, index));
                break;
            case Int16dec:
                state = new DecimalType(new BigDecimal(registerToShort((byte[]) payload, index)).divide(BigDecimal.TEN,
                        1, BigDecimal.ROUND_HALF_UP));
                break;
            case Int16cen:
                state = new DecimalType(new BigDecimal(registerToShort((byte[]) payload, index))
                        .divide(new BigDecimal(100), 1, BigDecimal.ROUND_HALF_UP));
                break;
            case Uint32:
                state = new DecimalType(registersToUnsignedInt((byte[]) payload, index));
                break;
            case Int32:
                state = new DecimalType(registersToInt((byte[]) payload, index));
                break;
            case Float32:
                state = new DecimalType(registersToFloat((byte[]) payload, index));
                break;
            case Int32_swap:
                state = new DecimalType(registersToIntSwap((byte[]) payload, index));
                break;
            case Uint32_swap:
                state = new DecimalType(registersToUnsignedIntSwap((byte[]) payload, index));
                break;
            case Float32_swap:
                state = new DecimalType(registersToFloatSwap((byte[]) payload, index));
                break;
            case Custom16_power:
                state = new DecimalType((float) registerToUnsignedShort((byte[]) payload, index) * 24 / 0.54);
                break;
            case Custom32_power:
                state = new DecimalType((float) registersToIntSwap((byte[]) payload, index) * 24 / 0.54);
                break;
            case Custom8_4bit:
                state = new DecimalType(unsignedByteToInt(((byte[]) payload)[index]) & 0x0F);
                break;
            case Custom8_5bit:
                state = new DecimalType(unsignedByteToInt(((byte[]) payload)[index]) & 0x1F);
                break;
            case Custom8_6bit:
                state = new DecimalType(unsignedByteToInt(((byte[]) payload)[index]) & 0x3F);
                break;
            case Jablotron_RSSI:
                state = new DecimalType(unsignedByteToInt(((byte[]) payload)[index]) * 0.5 - 74);
                break;
            case Jablotron_battery:
                state = new DecimalType(unsignedByteToInt(((byte[]) payload)[index]) * 10);
                break;
            case Jablotron_schedule:
                JsonObject schedule = new JsonObject();
                schedule.addProperty("kind", unsignedByteToInt(((byte[]) payload)[index]) & 0x7F);
                for (WeekDayClass day : WeekDayClass.values()) {
                    int startIndexInclusive = 2 + day.ordinal() * 6;
                    int endIndexExclusive = startIndexInclusive + 6;

                    BitVector bv = BitVector.createBitVector(
                            ArrayUtils.subarray(((byte[]) payload), startIndexInclusive, endIndexExclusive));
                    schedule.addProperty(day.getDay(), bv.toString());
                }
                state = new StringType(schedule.toString());
                break;
            case Jablotron_elementChangeFlags:
            case Jablotron_channelChangeFlags:
                // We should never arrive here - it's handled in ModbusFactory
                break;
            case Nilan_text:
                int startIndex = index + 4;
                int endIndex = startIndex + 10;

                byte[] textAsByte = ArrayUtils.subarray((byte[]) payload, startIndex, endIndex);
                // Swap the bytes
                for (int i = 0; i < 8; i = i + 2) {
                    byte b = textAsByte[i];
                    textAsByte[i] = textAsByte[i + 1];
                    textAsByte[i + 1] = b;
                }

                // Do character set conversion
                byte b;
                for (int i = 0; i < 8; i++) {
                    switch ((byte) textAsByte[i]) {
                        case (byte) 0xDF:
                            b = (byte) 186;
                            break; //
                        case (byte) 0x09:
                            b = (byte) 216;
                            break; // Ø
                        default:
                            b = textAsByte[i];

                    }
                    textAsByte[i] = b;
                }
                BitVector attrib = new BitVector(16);
                attrib.setBytes(ArrayUtils.subarray(textAsByte, 8, 2));
                // logger.info("Attrib: {}", attrib.toString());

                state = new StringType(new String(ArrayUtils.subarray(textAsByte, 0, 8), StandardCharsets.ISO_8859_1));
                break;
            case Nilan_time:
                int second = registerToShort((byte[]) payload, index);
                int minute = registerToShort((byte[]) payload, index + 2);
                int hour = registerToShort((byte[]) payload, index + 4);
                int day = registerToShort((byte[]) payload, index + 6);
                int month = registerToShort((byte[]) payload, index + 8);
                int year = registerToShort((byte[]) payload, index + 10);
                state = new DateTimeType(
                        LocalDateTime.of(year, month, day, hour, minute, second).atZone(ZoneId.of("Europe/Paris")));
                break;
            default:
                break;
        }
        return state;
    }

    @Nullable
    public static Object fromStateToModbus(ChannelUID uid, State state, ModbusThingChannel channel) {
        Object payload = null;
        if (state.getClass().equals(org.eclipse.smarthome.core.library.types.OnOffType.class)) {
            DecimalType onOffAsDecimalType = new DecimalType((((OnOffType) state) == OnOffType.ON) ? 0xFF : 0);
            switch (channel.getValueClass()) {
                case Bit:
                    payload = (boolean) (((OnOffType) state) == OnOffType.ON);
                    break;
                case Int8:
                    payload = onOffAsDecimalType.byteValue();
                    break;
                case Uint16:
                case Int16:
                    payload = shortToRegister((short) onOffAsDecimalType.intValue());
                    break;
                case Int16dec:
                    payload = shortToRegister((short) (onOffAsDecimalType.floatValue() * 10));
                    break;
                case Int16cen:
                    payload = shortToRegister((short) (onOffAsDecimalType.floatValue() * 100));
                    break;
                case Uint32:
                case Int32:
                    payload = intToRegisters(onOffAsDecimalType.intValue());
                    break;
                case Float32:
                    payload = floatToRegisters(onOffAsDecimalType.floatValue());
                    break;
                case Int32_swap:
                case Uint32_swap:
                    payload = intToRegistersSwap(onOffAsDecimalType.intValue());
                    break;
                default:
                    logger.error("ValueClass not found - return null");
                    return null;
            }
        } else {
            switch (channel.getValueClass()) {
                case Bit:
                    payload = (boolean) (((OnOffType) state) == OnOffType.ON);
                    break;
                case Int8:
                    payload = ((DecimalType) state).byteValue();
                    break;
                case Uint16:
                    payload = shortToRegister((short) ((DecimalType) state).intValue());
                    break;
                case Int16:
                    payload = shortToRegister((short) ((DecimalType) state).intValue());
                    break;
                case Int16dec:
                    payload = shortToRegister((short) (((DecimalType) state).floatValue() * 10));
                    break;
                case Int16cen:
                    payload = shortToRegister((short) (((DecimalType) state).floatValue() * 100));
                    break;
                case Uint32:
                    payload = intToRegisters(((DecimalType) state).intValue());
                    break;
                case Int32:
                    payload = intToRegisters(((DecimalType) state).intValue());
                    break;
                case Float32:
                    payload = floatToRegisters(((DecimalType) state).floatValue());
                    break;
                case Int32_swap:
                    payload = intToRegistersSwap(((DecimalType) state).intValue());
                    break;
                case Uint32_swap:
                    payload = intToRegistersSwap(((DecimalType) state).intValue());
                    break;
                case Float32_swap:
                    payload = floatToRegistersSwap(((DecimalType) state).floatValue());
                    break;
                case Custom16_power:
                    logger.warn("'Custom16_power' - write not supported, read-only channel");
                    return null;
                case Custom8_4bit:
                    payload = ((DecimalType) state).byteValue() & 0x0F;
                    break;
                case Custom8_5bit:
                    payload = ((DecimalType) state).byteValue() & 0x1F;
                    break;
                case Custom8_6bit:
                    payload = ((DecimalType) state).byteValue() & 0x3F;
                    break;
                case Jablotron_schedule:
                    JsonParser parser = new JsonParser();
                    JsonObject schedule = parser.parse(state.toFullString()).getAsJsonObject();
                    payload = shortToRegister(schedule.get("kind").getAsShort());
                    for (WeekDayClass day : WeekDayClass.values()) {
                        BitVector bv = BitVector.createBitVector(schedule.get(day.getDay()).getAsString());
                        payload = ArrayUtils.addAll((byte[]) payload, bv.getBytes());
                    }
                    break;
                case Nilan_time:
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ZonedDateTime dateTime = ((DateTimeType) state).getZonedDateTime();
                    try {
                        outputStream.write(shortToRegister((short) dateTime.getSecond()));
                        outputStream.write(shortToRegister((short) dateTime.getMinute()));
                        outputStream.write(shortToRegister((short) dateTime.getHour()));
                        outputStream.write(shortToRegister((short) dateTime.getDayOfMonth()));
                        outputStream.write(shortToRegister((short) dateTime.getMonthValue()));
                    } catch (IOException e) {
                        logger.error("Unable to set nilan time");
                    }
                    payload = outputStream.toByteArray();
                    break;
                default:
                    logger.error("ValueClass not found - return null");
                    return null;
            }
        }
        if (channel.getValueClass() != ModbusValueClass.Bit) {
            payload = Arrays.copyOfRange((byte[]) payload, 0, ((channel.getValueClass().size() + 1) / 2 * 2));
        }
        return payload;
    }

}
