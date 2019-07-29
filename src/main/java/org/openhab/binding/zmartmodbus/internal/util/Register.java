/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zmartmodbus.internal.util;

import java.util.Arrays;

/**
 * Helper class that provides utility methods.
 *
 * @author Dieter Wimberger
 * @author John Charlton
 * @author Peter Kristensen
 *
 * @version @version@ (@date@)
 */
public final class Register {

    /**
     * booleans do not have a specified data order
     */
    public static final String MODBUS_BOOLEAN_ORDER = "none";
    /**
     * this is the ModbusFunction default (note only 16 bit or 2 byte data)
     */
    public static final String MODBUS_WORD_ORDER_BIG_ENDIAN = "12";
    public static final String MODBUS_WORD_ORDER_LITTLE_ENDIAN = "21";

    public static byte[] fromBigEndian(byte[] data) {
        // Default just return the data untouched
        return data;
    }

    public static byte[] fromLittleEndian(byte[] data) {
        byte[] ret = new byte[2];
        ret[0] = data[1];
        ret[1] = data[0];
        return ret;
    }

    public static byte[] toBigEndian(byte[] data) {
        // Default just return the data untouched
        return data;
    }

    public static byte[] toLittleEndian(byte[] data) {
        byte[] ret = new byte[2];
        ret[1] = data[0];
        ret[0] = data[1];
        return ret;
    }

    /**
     * this is the most common 32 bit arrangement used by many devices
     */
    public static final String MODBUS_LONG_ORDER_BIG_BIG_ENDIAN = "1234";
    public static final String MODBUS_LONG_ORDER_BIG_LITTLE_ENDIAN = "2143";
    public static final String MODBUS_LONG_ORDER_LITTLE_BIG_ENDIAN = "3412";
    public static final String MODBUS_LONG_ORDER_LITTLE_LITTLE_ENDIAN = "4321";

    public static byte[] fromBigBigEndian(byte[] data) {
        // Default just return the data untouched
        return data;
    }

    public static byte[] fromBigLittleEndian(byte[] data) {
        byte[] ret = new byte[4];
        ret[0] = data[1];
        ret[1] = data[0];
        ret[2] = data[3];
        ret[3] = data[2];
        return ret;
    }

    public static byte[] fromLittleBigEndian(byte[] data) {
        byte[] ret = new byte[4];
        ret[0] = data[2];
        ret[1] = data[3];
        ret[2] = data[0];
        ret[3] = data[1];
        return ret;
    }

    public static byte[] fromLittleLittleEndian(byte[] data) {
        byte[] ret = new byte[4];
        ret[0] = data[3];
        ret[1] = data[2];
        ret[2] = data[1];
        ret[3] = data[0];
        return ret;
    }

    public static byte[] toBigBigEndian(byte[] data) {
        // Default just return the data untouched
        return data;
    }

    public static byte[] toBigLittleEndian(byte[] data) {
        byte[] ret = new byte[4];
        ret[1] = data[0];
        ret[0] = data[1];
        ret[3] = data[2];
        ret[2] = data[3];
        return ret;
    }

    public static byte[] toLittleBigEndian(byte[] data) {
        byte[] ret = new byte[4];
        ret[2] = data[0];
        ret[3] = data[1];
        ret[0] = data[2];
        ret[1] = data[3];
        return ret;
    }

    public static byte[] toLittleLittleEndian(byte[] data) {
        byte[] ret = new byte[4];
        ret[3] = data[0];
        ret[2] = data[1];
        ret[1] = data[2];
        ret[0] = data[3];
        return ret;
    }

    /**
     * Returns the given byte[] as hex encoded string.
     *
     * @param data a byte[] array.
     * @return a hex encoded String.
     */
    public static final String toHex(byte[] data) {
        return toHex(data, 0, data.length);
    }// toHex

    /**
     * Returns a <tt>String</tt> containing unsigned hexadecimal
     * numbers as digits.
     * The <tt>String</tt> will coontain two hex digit characters
     * for each byte from the passed in <tt>byte[]</tt>.<br>
     * The bytes will be separated by a space character.
     * <p/>
     *
     * @param data the array of bytes to be converted into a hex-string.
     * @param off the offset to start converting from.
     * @param length the number of bytes to be converted.
     *
     * @return the generated hexadecimal representation as <code>String</code>.
     */
    public static final String toHex(byte[] data, int off, int length) {
        // double size, two bytes (hex range) for one byte
        StringBuffer buf = new StringBuffer(data.length * 2);
        for (int i = off; i < length; i++) {
            // don't forget the second hex digit
            if ((data[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString(data[i] & 0xff, 16));
            if (i < data.length - 1) {
                buf.append(" ");
            }
        }
        return buf.toString();
    }// toHex

    /**
     * Returns a <tt>byte[]</tt> containing the given
     * byte as unsigned hexadecimal number digits.
     * <p/>
     *
     * @param i the int to be converted into a hex string.
     * @return the generated hexadecimal representation as <code>byte[]</code>.
     */
    public static final byte[] toHex(int i) {
        StringBuffer buf = new StringBuffer(2);
        // don't forget the second hex digit
        if ((i & 0xff) < 0x10) {
            buf.append("0");
        }
        buf.append(Long.toString(i & 0xff, 16).toUpperCase());
        return buf.toString().getBytes();
    }// toHex

    /**
     * Converts the register (a 16 bit value) into an unsigned short.
     * The value returned is:
     * <p>
     *
     * <pre>
     * <code>(((a &amp; 0xff) &lt;&lt; 8) | (b &amp; 0xff))
     * </code>
     * </pre>
     * <p/>
     * This conversion has been taken from the documentation of
     * the <tt>DataInput</tt> interface.
     *
     * @param bytes a register as <tt>byte[2]</tt>.
     * @return the unsigned short value as <tt>int</tt>.
     * @see java.io.DataInput
     */
    public static final int registerToUnsignedShort(byte[] bytes) {
        return ((bytes[0] & 0xff) << 8 | (bytes[1] & 0xff));
    }// registerToUnsignedShort

    public static final int registerToUnsignedShort(byte[] bytes, int from) {
        return ((bytes[from + 0] & 0xff) << 8 | (bytes[from + 1] & 0xff));
    }// registerToUnsignedShort

    /**
     * Converts the given unsigned short into a register
     * (2 bytes).
     * The byte values in the register, in the order
     * shown, are:
     * <p/>
     *
     * <pre>
     * <code>
     * (byte)(0xff &amp; (v &gt;&gt; 8))
     * (byte)(0xff &amp; v)
     * </code>
     * </pre>
     * <p/>
     * This conversion has been taken from the documentation of
     * the <tt>DataOutput</tt> interface.
     *
     * @param v
     * @return the register as <tt>byte[2]</tt>.
     * @see java.io.DataOutput
     */
    public static final byte[] unsignedShortToRegister(int v) {
        byte[] register = new byte[2];
        register[0] = (byte) (0xff & (v >> 8));
        register[1] = (byte) (0xff & v);
        return register;
    }// unsignedShortToRegister

    /**
     * Converts the given register (16-bit value) into
     * a <tt>short</tt>.
     * The value returned is:
     * <p/>
     *
     * <pre>
     * <code>
     * (short)((a &lt;&lt; 8) | (b &amp; 0xff))
     * </code>
     * </pre>
     * <p/>
     * This conversion has been taken from the documentation of
     * the <tt>DataInput</tt> interface.
     *
     * @param bytes bytes a register as <tt>byte[2]</tt>.
     * @return the signed short as <tt>short</tt>.
     */
    public static final short registerToShort(byte[] bytes) {
        return (short) ((bytes[0] << 8) | (bytes[1] & 0xff));
    }// registerToShort

    /**
     * Converts the register (16-bit value) at the given index
     * into a <tt>short</tt>.
     * The value returned is:
     * <p/>
     *
     * <pre>
     * <code>
     * (short)((a &lt;&lt; 8) | (b &amp; 0xff))
     * </code>
     * </pre>
     * <p/>
     * This conversion has been taken from the documentation of
     * the <tt>DataInput</tt> interface.
     *
     * @param bytes a <tt>byte[]</tt> containing a short value.
     * @param idx an offset into the given byte[].
     * @return the signed short as <tt>short</tt>.
     */
    public static final short registerToShort(byte[] bytes, int from) {
        return (short) ((bytes[from] << 8) | (bytes[from + 1] & 0xff));
    }// registerToShort

    /**
     * Converts the given <tt>short</tt> into a register
     * (2 bytes).
     * The byte values in the register, in the order
     * shown, are:
     * <p/>
     *
     * <pre>
     * <code>
     * (byte)(0xff &amp; (v &gt;&gt; 8))
     * (byte)(0xff &amp; v)
     * </code>
     * </pre>
     *
     * @param s
     * @return a register containing the given short value.
     */
    public static final byte[] shortToRegister(short s) {
        byte[] register = new byte[2];
        register[0] = (byte) (0xff & (s >> 8));
        register[1] = (byte) (0xff & s);
        return register;
    }// shortToRegister

    /**
     * Converts a byte[4] binary int value to a primitive int.<br>
     * The value returned is:
     * <p>
     *
     * <pre>
     * <code>
     * (((a &amp; 0xff) &lt;&lt; 24) | ((b &amp; 0xff) &lt;&lt; 16) |
     * &#32;((c &amp; 0xff) &lt;&lt; 8) | (d &amp; 0xff))
     * </code>
     * </pre>
     *
     * @param bytes registers as <tt>byte[4]</tt>.
     * @return the integer contained in the given register bytes.
     */
    public static final int registersToInt(byte[] bytes) {
        return (((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff));
    }// registersToInt

    public static final int registersToInt(byte[] bytes, int from) {
        return registersToInt(Arrays.copyOfRange(bytes, from, from + 4));
    }// registersToInt

    public static final int registersToIntSwap(byte[] bytes) {
        return registersToInt(fromLittleBigEndian(bytes));
    }// registersToInt

    public static final int registersToIntSwap(byte[] bytes, int from) {
        return registersToInt(fromLittleBigEndian(Arrays.copyOfRange(bytes, from, from + 4)));
    }// registersToInt

    /**
     * Converts an int value to a byte[4] array.
     *
     * @param v the value to be converted.
     * @return a byte[4] containing the value.
     */
    public static final byte[] intToRegisters(int v) {
        byte[] registers = new byte[4];
        registers[0] = (byte) (0xff & (v >> 24));
        registers[1] = (byte) (0xff & (v >> 16));
        registers[2] = (byte) (0xff & (v >> 8));
        registers[3] = (byte) (0xff & v);
        return registers;
    }

    public static final byte[] intToRegistersSwap(int v) {
        return toLittleBigEndian(intToRegisters(v));
    }

    // unsignedIntToRegisters
    /**
     * Converts a byte[4] binary int value to a primitive int.<br>
     * The value returned is:
     * <p>
     *
     * <pre>
     * <code>
     * (((a &amp; 0xff) &lt;&lt; 24) | ((b &amp; 0xff) &lt;&lt; 16) |
     * &#32;((c &amp; 0xff) &lt;&lt; 8) | (d &amp; 0xff))
     * </code>
     * </pre>
     *
     * @param bytes registers as <tt>byte[4]</tt>.
     * @return the integer contained in the given register bytes.
     */
    public static final int registersToUnsignedInt(byte[] bytes) {
        return (((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff));
    }// registersToInt

    public static final int registersToUnsignedInt(byte[] bytes, int from) {
        return registersToInt(Arrays.copyOfRange(bytes, from, from + 4));
    }// registersToInt

    public static final int registersToUnsignedIntSwap(byte[] bytes) {
        return registersToInt(fromLittleBigEndian(bytes));
    }// registersToInt

    public static final int registersToUnsignedIntSwap(byte[] bytes, int from) {
        return registersToInt(fromLittleBigEndian(Arrays.copyOfRange(bytes, from, from + 4)));
    }// registersToInt

    /**
     * Converts an int value to a byte[4] array.
     *
     * @param v the value to be converted.
     * @return a byte[4] containing the value.
     */
    public static final byte[] unsignedIntToRegisters(int v) {
        byte[] registers = new byte[4];
        registers[0] = (byte) (0xff & (v >> 24));
        registers[1] = (byte) (0xff & (v >> 16));
        registers[2] = (byte) (0xff & (v >> 8));
        registers[3] = (byte) (0xff & v);
        return registers;
    }// intToRegisters

    public static final byte[] unsignedIntToRegistersSwap(int v) {
        return toLittleBigEndian(unsignedIntToRegisters(v));
    }// intToRegisters

    /**
     * Converts a byte[8] binary long value into a long
     * primitive.
     *
     * @param bytes a byte[8] containing a long value.
     * @return a long value.
     */
    public static final long registersToLong(byte[] bytes) {
        return ((((long) (bytes[0] & 0xff) << 56) | ((long) (bytes[1] & 0xff) << 48) | ((long) (bytes[2] & 0xff) << 40)
                | ((long) (bytes[3] & 0xff) << 32) | ((long) (bytes[4] & 0xff) << 24) | ((long) (bytes[5] & 0xff) << 16)
                | ((long) (bytes[6] & 0xff) << 8) | (bytes[7] & 0xff)));
    }// registersToLong

    /**
     * Converts a long value to a byte[8].
     *
     * @param v the value to be converted.
     * @return a byte[8] containing the long value.
     */
    public static final byte[] longToRegisters(long v) {
        byte[] registers = new byte[8];
        registers[0] = (byte) (0xff & (v >> 56));
        registers[1] = (byte) (0xff & (v >> 48));
        registers[2] = (byte) (0xff & (v >> 40));
        registers[3] = (byte) (0xff & (v >> 32));
        registers[4] = (byte) (0xff & (v >> 24));
        registers[5] = (byte) (0xff & (v >> 16));
        registers[6] = (byte) (0xff & (v >> 8));
        registers[7] = (byte) (0xff & v);
        return registers;
    }// longToRegisters

    /**
     * Converts a byte[4] binary float value to a float primitive.
     *
     * @param bytes the byte[4] containing the float value.
     * @return a float value.
     */
    public static final float registersToFloat(byte[] bytes) {
        return Float.intBitsToFloat(
                (((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff)));
    }// registersToFloat

    public static final float registersToFloat(byte[] bytes, int from) {
        return registersToFloat(Arrays.copyOfRange(bytes, from, from + 4));
    }

    public static final float registersToFloatSwap(byte[] bytes) {
        return registersToFloat(fromLittleBigEndian(bytes));
    }// registersToFloatSwap

    public static final float registersToFloatSwap(byte[] bytes, int from) {
        return registersToFloat(fromLittleBigEndian(Arrays.copyOfRange(bytes, from, from + 4)));
    }// registersToFloatSwap

 
    /**
     * Converts a float value to a byte[4] binary float value.
     *
     * @param f the float to be converted.
     * @return a byte[4] containing the float value.
     */
    public static final byte[] floatToRegisters(float f) {
        return intToRegisters(Float.floatToIntBits(f));
    }// floatToRegisters

    public static final byte[] floatToRegistersSwap(float f) {
        return toLittleBigEndian(intToRegisters(Float.floatToIntBits(f)));
    }// floatToRegisters

    /**
     * Converts a byte[8] binary double value into a double primitive.
     *
     * @param bytes a byte[8] to be converted.
     * @return a double value.
     */
    public static final double registersToDouble(byte[] bytes) {
        return Double.longBitsToDouble(((((long) (bytes[0] & 0xff) << 56) | ((long) (bytes[1] & 0xff) << 48)
                | ((long) (bytes[2] & 0xff) << 40) | ((long) (bytes[3] & 0xff) << 32) | ((long) (bytes[4] & 0xff) << 24)
                | ((long) (bytes[5] & 0xff) << 16) | ((long) (bytes[6] & 0xff) << 8) | (bytes[7] & 0xff))));
    }// registersToDouble

    public static final double registersToDouble(byte[] bytes, int from) {
        return registersToDouble(Arrays.copyOfRange(bytes, from, from + 8));
    }

    /*
     * public static final double registersToDoubleSwap(byte[] bytes) {
     * return registersToDouble(fromLittleBigEndian(bytes));
     * }
     *
     * public static final double registersToDoubleSwap(byte[] bytes, int from) {
     * return registersToDouble(fromLittleBigEndian(bytes), from);
     * }
     */
    /**
     * Converts a double value to a byte[8].
     *
     * @param d the double to be converted.
     * @return a byte[8].
     */
    public static final byte[] doubleToRegisters(double d) {
        return longToRegisters(Double.doubleToLongBits(d));
    }// doubleToRegisters

    public static final byte[] doubleToRegistersSwap(double d) {
        return toLittleBigEndian(longToRegisters(Double.doubleToLongBits(d)));
    }// doubleToRegisters

    /**
     * Converts an unsigned byte to an integer.
     *
     * @param b the byte to be converted.
     * @return an integer containing the unsigned byte value.
     */
    public static final int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }// unsignedByteToInt

    /**
     * Returs the low byte of an integer word.
     *
     * @param wd
     * @return the low byte.
     */
    public static final byte lowByte(int wd) {
        return (new Integer(0xff & wd).byteValue());
    }// lowByte

    /**
     * Returns the high byte of an integer word.
     *
     * @param wd
     * @return the hi byte.
     */
    public static final byte hiByte(int wd) {
        return (new Integer(0xff & (wd >> 8)).byteValue());
    }// hiByte

    // TODO: John description.
    /**
     * Make one integer word out of a high byte and a low byte
     * 
     * @param hibyte
     * @param lowbyte
     * @return a word.
     */
    public static final int makeWord(int hibyte, int lowbyte) {
        int hi = 0xFF & hibyte;
        int low = 0xFF & lowbyte;
        return ((hi << 8) | low);
    }// makeWord

}// class ModBusUtil
