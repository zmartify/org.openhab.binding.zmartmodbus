/***
 * Copyright 2002-2010 jamod development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

package org.openhab.binding.zmartmodbus.internal.util;

import java.util.Arrays;

/**
 * Class that implements a collection for
 * bits, storing them packed into bytes.
 * Per default the access operations will index from
 * the LSB (rightmost) bit.
 *
 * @author Dieter Wimberger
 * @version @version@ (@date@)
 */
public final class BitVector {

    // instance attributes
    private int m_Size;
    private byte[] m_Data;
    private boolean m_MSBAccess = false;

    /**
     * Constructs a new <tt>BitVector</tt> instance
     * with a given size.
     * <p>
     *
     * @param size the number of bits the <tt>BitVector</tt>
     *            should be able to hold.
     */
    public BitVector(int size) {
        // store bits
        m_Size = size;
        // calculate size in bytes
        m_Data = new byte[(size + 7) / 8];
        // make sure its all zeros
        Arrays.fill(m_Data, (byte) 0x00);
    }// constructor

    /**
     * Toggles the flag deciding whether the LSB
     * or the MSB of the byte corresponds to the
     * first bit (index=0).
     *
     * @param b true if LSB=0 up to MSB=7, false otherwise.
     */
    public void toggleAccess(boolean b) {
        m_MSBAccess = !m_MSBAccess;
    }// toggleAccess

    /**
     * Set the flag to the MSB of the byte corresponds to the
     * first bit (index=0).
     *
     * LSB=0 up to MSB=7
     */
    public void setMSBAccess() {
        m_MSBAccess = true;
    }// toggleAccess

    /**
     * Set the flag to the MSB of the byte corresponds to the
     * first bit (index=0).
     *
     * LSB=0 up to MSB=7
     */
    public void setLSBAccess() {
        m_MSBAccess = true;
    }// toggleAccess

    /**
     * Tests if this <tt>BitVector</tt> has
     * the LSB (rightmost) as the first bit
     * (i.e. at index 0).
     *
     * MSB=0 up to LSB=7
     */
    public boolean isLSBAccess() {
        return !m_MSBAccess;
    }// isLSBAccess

    /**
     * Tests if this <tt>BitVector</tt> has
     * the MSB (leftmost) as the first bit
     * (i.e. at index 0).
     *
     * @return true if LSB=0 up to MSB=7, false otherwise.
     */
    public boolean isMSBAccess() {
        return m_MSBAccess;
    }// isMSBAccess

    /**
     * Returns the <tt>byte[]</tt> which is used to store
     * the bits of this <tt>BitVector</tt>.
     * <p>
     *
     * @return the <tt>byte[]</tt> used to store the bits.
     */
    public final byte[] getBytes() {
        return m_Data;
    }// getBytes

    /**
     * Returns the <tt>byte[]</tt> which is used to store
     * the bits of this <tt>BitVector</tt>.
     * <p>
     *
     * @return the <tt>byte[]</tt> used to store the bits.
     */
    public final byte getByte(int index) {
        return m_Data[index];
    }// getByte

    public final void setByte(int index, byte data) {
        m_Data[index] = data;
    }// setByte

    public final int toInt() {
        // return first bits up to 16 as int;
        byte[] m_Bytes = getBytes();
        int ret = m_Bytes[0];
        if (m_Bytes.length > 1) {
            ret = ret | (m_Bytes[1] << 8);
        }
        return ret;
    }

    public final int toInt(int offset) {
        // return first bits up to 16 as int starting from offset in bits;
        int length = size();
        if ((length - offset) > 16) {
            length = 16 + offset;
        }
        if (length <= 0) {
            return 0;
        }
        BitVector bv = rangeOf(offset, length - offset);
        if (bv.byteSize() > 1) {
            return (bv.getByte(0) + (bv.getByte((1) << 8)));
        } else {
            return bv.getByte(0);
        }
    }

    /**
     * Sets the <tt>byte[]</tt> which stores
     * the bits of this <tt>BitVector</tt>.
     * <p>
     *
     * @param data a <tt>byte[]</tt>.
     */
    public final void doBytesSwap(byte[] data) {
        // Only works with even sets of bytes
        for (int i = 1; i < data.length; i = i + 2) {
            m_Data[i - 1] = data[i];
            m_Data[i] = data[i - 1];
        }
    }// setBytes

    /**
     * Sets the <tt>byte[]</tt> which stores
     * the bits of this <tt>BitVector</tt>.
     * <p>
     *
     * @param data a <tt>byte[]</tt>.
     */
    public final void setBytes(byte[] data) {
        System.arraycopy(data, 0, m_Data, 0, data.length);
    }// setBytes

    /**
     * Sets the <tt>byte[]</tt> which stores
     * the bits of this <tt>BitVector</tt>.
     * <p>
     *
     * @param data a <tt>byte[]</tt>.
     */
    public final void setBytes(byte[] data, int size) {
        System.arraycopy(data, 0, m_Data, 0, data.length);
        m_Size = size;
    }// setBytes

    /**
     * Returns the state of the bit at the given index of this
     * <tt>BitVector</tt>.
     * <p>
     *
     * @param index the index of the bit to be returned.
     *
     * @return true if the bit at the specified index is set,
     *         false otherwise.
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public final boolean getBit(int index) throws IndexOutOfBoundsException {
        index = translateIndex(index);
        return ((m_Data[byteIndex(index)] & (0x01 << bitIndex(index))) != 0) ? true : false;
    }// getBit

    /**
     * Returns a range of a BitVector
     * <tt>BitVector</tt>.
     * <p>
     *
     * @param start where the range starts (incusive)
     * @param stop where the range stops (exclusive)
     *
     * @return true if the bit at the specified index is set,
     *         false otherwise.
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public final BitVector rangeOf(int start, int stop) throws IndexOutOfBoundsException {
        BitVector ret_Data = new BitVector(stop - start);
        for (int i = 0; i < ret_Data.size(); i++) {
            if (i < m_Size) {
                ret_Data.setBit(i, getBit(i + start));
            } else {
                ret_Data.setBit(i, false);
            }
        }
        return ret_Data;
    }// rangeOf

    /**
     * Sets the state of the bit at the given index of
     * this <tt>BitVector</tt>.
     * <p>
     *
     * @param index the index of the bit to be set.
     * @param b true if the bit should be set, false if it should be reset.
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public final void setBit(int index, boolean b) throws IndexOutOfBoundsException {
        index = translateIndex(index);
        int value = ((b) ? 1 : 0);
        int byteNum = byteIndex(index);
        int bitNum = bitIndex(index);
        m_Data[byteNum] = (byte) ((m_Data[byteNum] & ~(0x01 << bitNum)) | ((value & 0x01) << bitNum));
    }// setBit

    /**
     * Returns the number of bits in this <tt>BitVector</tt>
     * as <tt>int</tt>.
     * <p>
     *
     * @return the number of bits in this <tt>BitVector</tt>.
     */
    public final int size() {
        return m_Size;
    }// size

    /**
     * Forces the number of bits in this <tt>BitVector</tt>.
     *
     * @param size
     * @throws IllegalArgumentException if the size exceeds
     *             the byte[] store size multiplied by 8.
     */
    public final void forceSize(int size) {
        if (size > m_Data.length * 8) {
            throw new IllegalArgumentException("Size exceeds byte[] store.");
        } else {
            m_Size = size;
        }
    }// forceSize

    /**
     * Returns the number of bytes used to store the
     * collection of bits as <tt>int</tt>.
     * <p>
     *
     * @return the number of bytes in this <tt>BitVector</tt>.
     */
    public final int byteSize() {
        return m_Data.length;
    }// byteSize

    /**
     * Returns the number of registers used to store the
     * collection of bits as <tt>int</tt>.
     * <p>
     *
     * @return the number of registers (16 bit) in this <tt>BitVector</tt>.
     */
    public final int regSize() {
        return (m_Data.length + 1) / 2;
    }// regSize

    /**
     * Returns a <tt>String</tt> representing the
     * contents of the bit collection in a way that
     * can be printed to a screen or log.
     * <p>
     * Note that this representation will <em>ALLWAYS</em>
     * show the MSB to the left and the LSB to the right
     * in each byte.
     *
     * @return a <tt>String</tt> representing this <tt>BitVector</tt>.
     */
    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        for (int i = 0; i < size(); i++) {
            sbuf.append((getBit(i) ? true : false) ? '1' : '0');
            if (((i + 1) % 8) == 0) {
                sbuf.append(" ");
            }
        }
        return sbuf.toString();
    }// toString

    /**
     * Returns a <tt>BitVector</tt> decoding string
     *
     * @param sbuf
     * @return
     */
    public static BitVector createBitVector(String sbuf) {
        BitVector bv = new BitVector(sbuf.length());
        for (int i = 0; i < sbuf.length(); i++) {
            bv.setBit(i, sbuf.charAt(i) == '1');
        }
        return bv;
    }

    /**
     * Returns the index of the byte in the the byte array
     * that contains the given bit.
     * <p>
     *
     * @param index the index of the bit.
     *
     * @return the index of the byte where the given bit is stored.
     *
     * @throws IndexOutOfBoundsException if index is
     *             out of bounds.
     */
    private final int byteIndex(int index) throws IndexOutOfBoundsException {

        if (index < 0 || index >= m_Data.length * 8) {
            throw new IndexOutOfBoundsException();
        } else {
            return index / 8;
        }
    }// byteIndex

    /**
     * Returns the index of the given bit in the byte
     * where it it stored.
     * <p>
     *
     * @param index the index of the bit.
     *
     * @return the bit index relative to the position in the byte
     *         that stores the specified bit.
     *
     * @throws IndexOutOfBoundsException if index is
     *             out of bounds.
     */
    private final int bitIndex(int index) throws IndexOutOfBoundsException {

        if (index < 0 || index >= m_Data.length * 8) {
            throw new IndexOutOfBoundsException();
        } else {
            return index % 8;
        }
    }// bitIndex

    private final int translateIndex(int idx) {
        if (m_MSBAccess) {
            int mod4 = idx % 4;
            int div4 = idx / 4;

            if ((div4 % 2) != 0) {
                // odd
                return (idx + ODD_OFFSETS[mod4]);
            } else {
                // straight
                return (idx + STRAIGHT_OFFSETS[mod4]);
            }
        } else {
            return idx;
        }
    }// translateIndex

    private static final int doTranslateIndex(int idx) {

        int mod4 = idx % 4;
        int div4 = idx / 4;

        if ((div4 % 2) != 0) {
            // odd
            return (idx + ODD_OFFSETS[mod4]);
        } else {
            // straight
            return (idx + STRAIGHT_OFFSETS[mod4]);
        }

    }// translateIndex

    /**
     * Factory method for creating a <tt>BitVector</tt> instance
     * wrapping the given byte data.
     *
     * @param data a byte[] containing packed bits.
     * @return the newly created <tt>BitVector</tt> instance.
     */
    public static BitVector createBitVector(byte[] data, int size) {
        BitVector bv = new BitVector(size);
        bv.setBytes(Arrays.copyOfRange(data, 0, (size + 7) / 8));
        return bv;
    }// createBitVector

    /**
     * Factory method for creating a <tt>BitVector</tt> instance
     * wrapping the given byte data.
     *
     * @param data a byte containing packed bits to replicated.
     * @return the newly created <tt>BitVector</tt> instance.
     */
    public static BitVector createBitVector(byte filler, int size) {
        BitVector bv = new BitVector(size);
        Arrays.fill(bv.m_Data, filler);
        return bv;
    }// createBitVector

    /**
     * Factory method for creating a <tt>BitVector</tt> instance
     * wrapping the given byte data. Swapping each pair of bytes
     * to accommodate for LittleBigEndian
     *
     * @param data a byte[] containing packed bits.
     * @return the newly created <tt>BitVector</tt> instance.
     */
    public static BitVector createBitVectorSwap(byte[] data, int size) {
        BitVector bv = new BitVector(size);
        bv.doBytesSwap(Arrays.copyOfRange(data, 0, (size + 7) / 8));
        return bv;
    }// createBitVector

    /**
     * Factory method for creating a <tt>BitVector</tt> instance
     * wrapping the given byte data.
     *
     * @param data a byte[] containing packed bits.
     * @return the newly created <tt>BitVector</tt> instance.
     */
    public static BitVector createBitVector(byte[] data) {
        BitVector bv = new BitVector(data.length * 8);
        bv.setBytes(data);
        return bv;
    }// createBitVector

    /**
     * Factory method for creating a <tt>BitVector</tt> instance
     * wrapping the given byte data. Swapping each pair of bytes
     * to accommodate for LittleBigEndian
     *
     * @param data a byte[] containing packed bits.
     * @return the newly created <tt>BitVector</tt> instance.
     */
    public static BitVector createBitVectorSwap(byte[] data) {
        BitVector bv = new BitVector(data.length * 8);
        bv.doBytesSwap(data);
        return bv;
    }// createBitVector

    /**
     * Factory method for creating a <tt>BitVector</tt> instance
     * wrapping the given int data.
     *
     * @param data a int[] containing packed bits.
     * @return the newly created <tt>BitVector</tt> instance.
     */
    public static BitVector createBitVector(int[] intData, int size) {
        return createBitVector(Arrays.copyOfRange(intData, 0, (size + 15) / 16));
    }// createBitVector

    /**
     * Factory method for creating a <tt>BitVector</tt> instance
     * wrapping the given int data.
     *
     * @param data a int[] containing packed bits.
     * @return the newly created <tt>BitVector</tt> instance.
     */
    public static BitVector createBitVector(int[] intData) {
        BitVector bv = new BitVector(intData.length * 16);
        byte[] byteData = new byte[bv.byteSize()];

        for (int i = 0; i < intData.length; i++) {
            byteData[i * 2] = (byte) (intData[i] & 0x00FF);
            byteData[i * 2 + 1] = (byte) (intData[i] & 0xFF00 >> 8);
        }
        bv.setBytes(byteData);
        return bv;
    }// createBitVector

    public boolean equals(BitVector compare) {
        return Arrays.equals(m_Data, compare.getBytes());
    }

    public BitVector add(BitVector bv1, BitVector bv2) {
        BitVector bvret = new BitVector(bv1.size() + bv2.size());
        int length1 = bv1.size();
        int length2 = bv2.size();
        bvret.setBytes(bv1.getBytes());
        for (int i = 0; i < length2; i++) {
            bvret.setBit(i + length1, bv2.getBit(i));
        }
        return bvret;
    }

    private static final int[] ODD_OFFSETS = { -1, -3, -5, -7 };
    private static final int[] STRAIGHT_OFFSETS = { 7, 5, 3, 1 };
}// class BitVector
