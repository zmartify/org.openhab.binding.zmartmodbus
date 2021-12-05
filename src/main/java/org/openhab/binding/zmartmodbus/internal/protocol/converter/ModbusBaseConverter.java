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
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import javax.measure.Unit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
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

    public static State fromModbusToState(ModbusThingChannel channel, Object payload) {
        int index = channel.getIndex() * channel.getValueClass().size();
        State state = UnDefType.UNDEF;
        // logger.debug("{} {} {}", state, channel.getUID().getClass(),
        // channel.getValueClass());
        switch (channel.getValueClass()) {
        case Bit:
            state = (OnOffType) (((BitVector) payload).getBit(channel.getIndex()) == true ? OnOffType.ON
                    : OnOffType.OFF);
            break;
        case OnOff16:
            state = (OnOffType) ((registerToShort((byte[]) payload, index) == 0) ? OnOffType.OFF : OnOffType.ON);
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
        case Jablotron_power16:
            state = new DecimalType((float) registerToUnsignedShort((byte[]) payload, index) * 24 / 0.54);
            break;
        case Jablotron_power32:
            state = new DecimalType((float) registersToIntSwap((byte[]) payload, index) * 24 / 0.54);
            break;
        case Jablotron_modeset:
            state = new DecimalType((int) ((BitVector) payload).getByte(index) & 0x0F);
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
        case Custom8_7bit:
            state = new DecimalType(unsignedByteToInt(((byte[]) payload)[index]) & 0x7F);
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
                        Arrays.copyOfRange(((byte[]) payload), startIndexInclusive, endIndexExclusive));
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

            byte[] textAsByte = Arrays.copyOfRange((byte[]) payload, startIndex, endIndex);
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
            attrib.setBytes(Arrays.copyOfRange(textAsByte, 8, 2));
            // logger.info("Attrib: {}", attrib.toString());

            state = new StringType(new String(Arrays.copyOfRange(textAsByte, 0, 8), StandardCharsets.ISO_8859_1));
            break;
        case Nilan_time:
            int second = registerToShort((byte[]) payload, index);
            int minute = registerToShort((byte[]) payload, index + 2);
            int hour = registerToShort((byte[]) payload, index + 4);
            int day = registerToShort((byte[]) payload, index + 6);
            int month = registerToShort((byte[]) payload, index + 8);
            int year = registerToShort((byte[]) payload, index + 10);
            try {
                state = new DateTimeType(
                        LocalDateTime.of(year, month, day, hour, minute, second).atZone(ZoneId.of("Europe/Paris")));
            } catch (DateTimeException e) {
                state = UnDefType.UNDEF;
            }
            break;
        case DOS_time:
            long dosTime = registersToUnsignedInt((byte[]) payload, index);
            try {
                state = new DateTimeType(LocalDateTime
                        .of((int) (((dosTime >> 25) & 0x7f) + 1980), (int) (((dosTime >> 21) & 0x0f) - 1),
                                (int) ((dosTime >> 16) & 0x1f), (int) ((dosTime >> 11) & 0x1f),
                                (int) ((dosTime >> 5) & 0x3f), (int) ((dosTime << 1) & 0x3e))
                        .atZone(ZoneId.of("Europe/Paris")));
            } catch (DateTimeException e) {
                state = UnDefType.UNDEF;
            }
            break;
        default:
            break;
        }
        if ((channel.getUnitsOfMeasure() != null) && (state instanceof Number)) {
            state = (new QuantityType<>(
                    BigDecimal.valueOf(((Number) state).longValue()).movePointLeft(channel.getScale()),
                    channel.getUnitsOfMeasure().getApi()));
        }
        return state;
    }

    @Nullable
    public static Object fromStateToModbus(State state, ModbusThingChannel channel) {
        Object payload = null;
        BigDecimal value;

        if (state.getClass().equals(org.openhab.core.library.types.OnOffType.class)) {
            value = new BigDecimal((((OnOffType) state) == OnOffType.ON) ? 1 : 0);
        } else {
            // Handle Units of Measure channels
            if ((state instanceof QuantityType) && (channel.getUnitsOfMeasure() != null)) {
                QuantityType<?> quantity = (QuantityType<?>) state;
                value = quantity.toUnit(channel.getUnitsOfMeasure().getApi()).toBigDecimal()
                        .movePointRight(channel.getScale());
            } else {
                if (state instanceof Number) {
                    // Normal channel with numeric value
                    value = BigDecimal.valueOf(((Number) state).doubleValue());
                } else {
                    logger.warn("QuantityType is not Number: {}", channel.getUID());
                    value = null;
                }
            }
        }

        switch (channel.getValueClass()) {
        case Bit:
            payload = (boolean) (((OnOffType) state) == OnOffType.ON);
            break;
        case OnOff16:
            payload = shortToRegister((short) ((((OnOffType) state) == OnOffType.ON) ? 1 : 0));
            break;
        case Int8:
            payload = value.byteValue();
            break;
        case Uint16:
        case Int16:
            payload = shortToRegister(value.shortValue());
            break;
        case Uint32:
        case Int32:
            payload = intToRegisters(value.intValue());
            break;
        case Float32:
            payload = floatToRegisters(value.floatValue());
            break;
        case Int32_swap:
        case Uint32_swap:
            payload = intToRegistersSwap(value.intValue());
            break;
        case Float32_swap:
            payload = floatToRegistersSwap(value.floatValue());
            break;
        case Jablotron_power16:
        case Jablotron_power32:
            logger.warn("'Jablotron_power16 and Jablotron_power32' - write not supported, read-only channel");
            return null;
        case Jablotron_modeset:
            logger.debug("----->Jablotron_modeset {}", value.shortValue() & 0x0F);
            payload = shortToRegister(value.shortValue());
            break;
        case Custom8_4bit:
            payload = value.byteValue() & 0x0F;
            break;
        case Custom8_5bit:
            payload = value.byteValue() & 0x1F;
            break;
        case Custom8_6bit:
            payload = value.byteValue() & 0x3F;
            break;
        case Custom8_7bit:
            payload = value.byteValue() & 0x7F;
            break;
        case Jablotron_schedule:
            JsonObject schedule = JsonParser.parseString(state.toFullString()).getAsJsonObject();
            payload = shortToRegister(schedule.get("kind").getAsShort());
            for (WeekDayClass day : WeekDayClass.values()) {
                BitVector bv = BitVector.createBitVector(schedule.get(day.getDay()).getAsString());
                payload = ByteBuffer.allocate(((byte[]) payload).length + bv.byteSize())
                .put((byte[]) payload).put(bv.getBytes()).array();
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
        logger.debug("payload = {}", (byte) ((byte[]) payload)[1]);
        if (channel.getValueClass() != ModbusValueClass.Bit){
            payload = Arrays.copyOfRange((byte[]) payload, 0, ((channel.getValueClass().size() + 1) / 2 * 2));
        }
        logger.debug("payload = {}", (byte) ((byte[]) payload)[1]);
        return payload;
    }

    /**
     * Converts an integer value into a {@link QuantityType}. The temperature as an
     * integer is assumed to be multiplied by 100 as per the ZigBee standard format.
     *
     * @param value the integer value to convert
     * @return the {@link QuantityType}
     */
    protected QuantityType<?> valueToTemperature(int value, Unit<?> api) {
        return new QuantityType<>(BigDecimal.valueOf(value, 2), api);
    }

    /**
     * Converts a {@link Command} to a Modbus temperature integer
     *
     * @param state the {@link Command} to convert
     * @return the {@link Command} or null if the conversion was not possible
     */
    protected Integer temperatureToValue(State state, int scale) {
        BigDecimal value = null;
        if (state instanceof QuantityType) {
            QuantityType<?> quantity = (QuantityType<?>) state;
            if (quantity.getUnit() == SIUnits.CELSIUS) {

                value = quantity.toBigDecimal();
            } else if (quantity.getUnit() == ImperialUnits.FAHRENHEIT) {
                QuantityType<?> celsius = quantity.toUnit(SIUnits.CELSIUS);
                if (celsius == null) {
                    return null;
                }
                value = celsius.toBigDecimal();
            } else {
                return null;
            }
        } else if (state instanceof Number) {
            // No scale, so assumed to be Celsius
            value = BigDecimal.valueOf(((Number) state).doubleValue());
        }
        return value.setScale(2, RoundingMode.CEILING).scaleByPowerOfTen(scale).intValue();
    }

}
