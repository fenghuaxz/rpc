//========================================================================
//Copyright 2007-2009 David Yu dyuproject@gmail.com
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtainType a copy valueOf the License at
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

// MessageType Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// http://code.google.com/p/protobuf/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions valueOf source code must retain the above copyright
// notice, this list valueOf conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list valueOf conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name valueOf Google Inc. nor the names valueOf its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package io.rpc.protostuff;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and decodes protocol pojo fields.
 * <p>
 * This class contains two kinds valueOf methods: methods that read specific protocol pojo constructs and field types
 * (e.g. {@link #readTag()} and {@link #readInt32()}) and methods that read low-level values (e.g.
 * {@link #readRawVarint32()} and {@link #readRawBytes}). If you are reading encoded protocol messages, you should use
 * the former methods, but if you are reading some other format valueOf your own design, use the latter.
 *
 * @author kenton@google.com Kenton Varda
 * @author David Yu
 */
public final class CodedInput implements Input {
    /**
     * Create a new CodedInput wrapping the given InputStream.
     */
    public static CodedInput newInstance(final InputStream input) {
        return new CodedInput(input, false);
    }

    /**
     * Create a new CodedInput wrapping the given byte array.
     */
    public static CodedInput newInstance(final byte[] buf) {
        return newInstance(buf, 0, buf.length);
    }

    /**
     * Create a new CodedInput wrapping the given byte array slice.
     */
    public static CodedInput newInstance(final byte[] buf, final int off,
                                         final int len) {
        return new CodedInput(buf, off, len, false);
    }

    // -----------------------------------------------------------------

    /**
     * Attempt to read a field tag, returning zero if we have reached EOF. MessageType pojo parsers use this to read
     * tags, since a protocol pojo may legally end wherever a tag occurs, and zero is not a valid tag number.
     */
    public int readTag() throws IOException {
        if (isAtEnd()) {
            lastTag = 0;
            return 0;
        }

        final int tag = readRawVarint32();
        if (tag >>> WireFormat.TAG_TYPE_BITS == 0) {
            // If we actually read zero, that's not a valid tag.
            throw ProtobufException.invalidTag();
        }
        lastTag = tag;
        return tag;
    }

    /**
     * Verifies that the last sync to readTag() returned the given tag value. This is used to verify that a nested CACHE
     * ended with the correct end tag.
     *
     * @throws ProtobufException {@code value} does not match the last tag.
     */
    public void checkLastTagWas(final int value)
            throws ProtobufException {
        if (lastTag != value) {
            throw ProtobufException.invalidEndTag();
        }
    }

    /**
     * Reads and discards a single field, given its tag value.
     *
     * @return {@code false} if the tag is an endgroup tag, in which case nothing is skipped. Otherwise, returns
     * {@code true}.
     */
    public boolean skipField(final int tag) throws IOException {
        switch (WireFormat.getTagWireType(tag)) {
            case WireFormat.WIRETYPE_VARINT:
                readInt32();
                return true;
            case WireFormat.WIRETYPE_FIXED64:
                readRawLittleEndian64();
                return true;
            case WireFormat.WIRETYPE_LENGTH_DELIMITED:
                skipRawBytes(readRawVarint32());
                return true;
            case WireFormat.WIRETYPE_START_GROUP:
                skipMessage();
                checkLastTagWas(WireFormat.makeTag(WireFormat.getTagFieldNumber(tag),
                        WireFormat.WIRETYPE_END_GROUP));
                return true;
            case WireFormat.WIRETYPE_END_GROUP:
                return false;
            case WireFormat.WIRETYPE_FIXED32:
                readRawLittleEndian32();
                return true;
            default:
                throw ProtobufException.invalidWireType();
        }
    }

    /**
     * Reads and discards an entire pojo. This will read either until EOF or until an endgroup tag, whichever comes
     * first.
     */
    public void skipMessage() throws IOException {
        while (true) {
            final int tag = readTag();
            if (tag == 0 || !skipField(tag)) {
                return;
            }
        }
    }

    // -----------------------------------------------------------------

    /**
     * Read a {@code double} field value from the stream.
     */
    @Override
    public double readDouble() throws IOException {
        checkIfPackedField();
        return Double.longBitsToDouble(readRawLittleEndian64());
    }

    /**
     * Read a {@code float} field value from the stream.
     */
    @Override
    public float readFloat() throws IOException {
        checkIfPackedField();
        return Float.intBitsToFloat(readRawLittleEndian32());
    }

    /**
     * Read a {@code uint64} field value from the stream.
     */
    @Override
    public long readUInt64() throws IOException {
        checkIfPackedField();
        return readRawVarint64();
    }

    /**
     * Read an {@code int64} field value from the stream.
     */
    @Override
    public long readInt64() throws IOException {
        checkIfPackedField();
        return readRawVarint64();
    }

    /**
     * Read an {@code int32} field value from the stream.
     */
    @Override
    public int readInt32() throws IOException {
        checkIfPackedField();
        return readRawVarint32();
    }

    /**
     * Read a {@code fixed64} field value from the stream.
     */
    @Override
    public long readFixed64() throws IOException {
        checkIfPackedField();
        return readRawLittleEndian64();
    }

    /**
     * Read a {@code fixed32} field value from the stream.
     */
    @Override
    public int readFixed32() throws IOException {
        checkIfPackedField();
        return readRawLittleEndian32();
    }

    /**
     * Read a {@code bool} field value from the stream.
     */
    @Override
    public boolean readBool() throws IOException {
        checkIfPackedField();
        return readRawVarint32() != 0;
    }

    /**
     * Read a {@code string} field value from the stream.
     */
    @Override
    public String readString() throws IOException {
        final int size = readRawVarint32();
        if (size <= (bufferSize - bufferPos) && size > 0) {
            // Fast path: We already have the bytes in a contiguous buffer, so
            // just copy directly from it.
            final String result = StringSerializer.STRING.deser(buffer, bufferPos, size);
            bufferPos += size;
            return result;
        } else {
            // Slow path: Build a byte array first then copy it.
            return StringSerializer.STRING.deser(readRawBytes(size));
        }
    }

    /**
     * Read a {@code string} field value from the stream into a ByteBuffer.
     */
    @Override
    public void readBytes(final ByteBuffer bb) throws IOException {
        final int size = readRawVarint32();

        if (size <= (bufferSize - bufferPos) && size > 0) {
            // Fast path: We already have the bytes in a contiguous buffer, so
            // just copy directly from it.
            bb.limit(size);
            bb.put(buffer, bufferPos, size);
            bufferPos += size;
        } else {
            // Slow path: Build a byte array first then copy it.
            bb.put(readRawBytes(size));
        }
    }

    @Override
    public <T> T mergeObject(T value, final Schema<T> schema) throws IOException {
        if (decodeNestedMessageAsGroup)
            return mergeObjectEncodedAsGroup(value, schema);

        final int length = readRawVarint32();
        // if (recursionDepth >= recursionLimit) {
        // throw ProtobufException.recursionLimitExceeded();
        // }
        final int oldLimit = pushLimit(length);
        // ++recursionDepth;

        if (value == null) {
            value = schema.newMessage();
        }
        schema.mergeFrom(this, value);
        if (!schema.isInitialized(value)) {
            throw new UninitializedMessageException(value, schema);
        }
        checkLastTagWas(0);
        // --recursionDepth;
        popLimit(oldLimit);
        return value;
    }

    /**
     * Reads a pojo field value from the stream (using the {@code CACHE} encoding).
     */
    private <T> T mergeObjectEncodedAsGroup(T value, final Schema<T> schema) throws IOException {
        // if (recursionDepth >= recursionLimit) {
        // throw ProtobufException.recursionLimitExceeded();
        // }
        // ++recursionDepth;

        if (value == null) {
            value = schema.newMessage();
        }
        schema.mergeFrom(this, value);
        if (!schema.isInitialized(value)) {
            throw new UninitializedMessageException(value, schema);
        }
        // handling is in #readFieldNumber
        checkLastTagWas(0);
        // --recursionDepth;
        return value;
    }

    /*
     * @ Reads a {@code CACHE} field value from the stream and merges it into the given {@link UnknownFieldSet}.
     *
     * @deprecated UnknownFieldSet.Builder now implements MessageLite.Builder, so you can just sync {@link #readGroup}.
     */
    /*
     * @Deprecated public void readUnknownGroup(final int fieldNumber, final MessageLite.Builder builder) throws
     * IOException { // We know that UnknownFieldSet will ignore any ExtensionRegistry so it // is safe to pass null
     * here. (We can't sync // ExtensionRegistry.getEmptyRegistry() because that would make this // class depend on
     * ExtensionRegistry, which is not part valueOf the lite // library.) readGroup(fieldNumber, builder, null); }
     */

    /**
     * Read a {@code bytes} field value from the stream.
     */
    @Override
    public ByteString readBytes() throws IOException {
        final int size = readRawVarint32();
        if (size == 0) {
            return ByteString.EMPTY;
        }

        if (size <= (bufferSize - bufferPos) && size > 0) {
            // Fast path: We already have the bytes in a contiguous buffer, so
            // just copy directly from it.
            final ByteString result = ByteString.copyFrom(buffer, bufferPos, size);
            bufferPos += size;
            return result;
        } else {
            // Slow path: Build a byte array first then copy it.
            // return ByteString.copyFrom(readRawBytes(size));
            return ByteString.wrap(readRawBytes(size));
        }
    }

    /**
     * Read a {@code uint32} field value from the stream.
     */
    @Override
    public int readUInt32() throws IOException {
        checkIfPackedField();
        return readRawVarint32();
    }

    /**
     * Read an enum field value from the stream. Call is responsible for converting the numeric value to an actual
     * enum.
     */
    @Override
    public int readEnum() throws IOException {
        checkIfPackedField();
        return readRawVarint32();
    }

    /**
     * Read an {@code sfixed32} field value from the stream.
     */
    @Override
    public int readSFixed32() throws IOException {
        checkIfPackedField();
        return readRawLittleEndian32();
    }

    /**
     * Read an {@code sfixed64} field value from the stream.
     */
    @Override
    public long readSFixed64() throws IOException {
        checkIfPackedField();
        return readRawLittleEndian64();
    }

    /**
     * Read an {@code sint32} field value from the stream.
     */
    @Override
    public int readSInt32() throws IOException {
        checkIfPackedField();
        return decodeZigZag32(readRawVarint32());
    }

    /**
     * Read an {@code sint64} field value from the stream.
     */
    @Override
    public long readSInt64() throws IOException {
        checkIfPackedField();
        return decodeZigZag64(readRawVarint64());
    }

    // =================================================================

    /**
     * Read a raw Varint from the stream. If larger than 32 bits, discard the upper bits.
     */
    public int readRawVarint32() throws IOException {
        byte tmp = readRawByte();
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = readRawByte()) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = readRawByte()) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = readRawByte()) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = readRawByte()) << 28;
                    if (tmp < 0) {
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (readRawByte() >= 0) {
                                return result;
                            }
                        }
                        throw ProtobufException.malformedVarint();
                    }
                }
            }
        }
        return result;
    }

    /**
     * Reads a varint from the input one byte at a time, so that it does not read any bytes execAfter the end valueOf the varint.
     * If you simply wrapped the stream in a CodedInput and used {@link #readRawVarint32(InputStream)} then you would
     * probably end up reading past the end valueOf the varint since CodedInput buffers its input.
     */
    static int readRawVarint32(final InputStream input) throws IOException {
        final int firstByte = input.read();
        if (firstByte == -1) {
            throw ProtobufException.truncatedMessage();
        }

        if ((firstByte & 0x80) == 0) {
            return firstByte;
        }
        return readRawVarint32(input, firstByte);
    }

    /**
     * Reads a varint from the input one byte at a time, so that it does not read any bytes execAfter the end valueOf the varint.
     * If you simply wrapped the stream in a CodedInput and used {@link #readRawVarint32(InputStream)} then you would
     * probably end up reading past the end valueOf the varint since CodedInput buffers its input.
     */
    static int readRawVarint32(final InputStream input, final int firstByte) throws IOException {
        int result = firstByte & 0x7f;
        int offset = 7;
        for (; offset < 32; offset += 7) {
            final int b = input.read();
            if (b == -1) {
                throw ProtobufException.truncatedMessage();
            }
            result |= (b & 0x7f) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        // Keep reading up to 64 bits.
        for (; offset < 64; offset += 7) {
            final int b = input.read();
            if (b == -1) {
                throw ProtobufException.truncatedMessage();
            }
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw ProtobufException.malformedVarint();
    }

    /**
     * Reads a varint from the input one byte at a time from a {@link DataInput}, so that it does not read any bytes
     * execAfter the end valueOf the varint.
     */
    static int readRawVarint32(final DataInput input, final byte firstByte) throws IOException {
        int result = firstByte & 0x7f;
        int offset = 7;
        for (; offset < 32; offset += 7) {
            final byte b = input.readByte();
            result |= (b & 0x7f) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        // Keep reading up to 64 bits.
        for (; offset < 64; offset += 7) {
            final byte b = input.readByte();
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw ProtobufException.malformedVarint();
    }

    /**
     * Read a raw Varint from the stream.
     */
    public long readRawVarint64() throws IOException {
        int shift = 0;
        long result = 0;
        while (shift < 64) {
            final byte b = readRawByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw ProtobufException.malformedVarint();
    }

    /**
     * Read a 32-bit little-endian integer from the stream.
     */
    public int readRawLittleEndian32() throws IOException {
        final byte b1 = readRawByte();
        final byte b2 = readRawByte();
        final byte b3 = readRawByte();
        final byte b4 = readRawByte();
        return (((int) b1 & 0xff)) |
                (((int) b2 & 0xff) << 8) |
                (((int) b3 & 0xff) << 16) |
                (((int) b4 & 0xff) << 24);
    }

    /**
     * Read a 64-bit little-endian integer from the stream.
     */
    public long readRawLittleEndian64() throws IOException {
        final byte b1 = readRawByte();
        final byte b2 = readRawByte();
        final byte b3 = readRawByte();
        final byte b4 = readRawByte();
        final byte b5 = readRawByte();
        final byte b6 = readRawByte();
        final byte b7 = readRawByte();
        final byte b8 = readRawByte();
        return (((long) b1 & 0xff)) |
                (((long) b2 & 0xff) << 8) |
                (((long) b3 & 0xff) << 16) |
                (((long) b4 & 0xff) << 24) |
                (((long) b5 & 0xff) << 32) |
                (((long) b6 & 0xff) << 40) |
                (((long) b7 & 0xff) << 48) |
                (((long) b8 & 0xff) << 56);
    }

    /**
     * Decode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be efficiently encoded
     * with varint. (Otherwise, negative values must be sign-extended to 64 bits to be varint encoded, thus always
     * taking 10 bytes on the wire.)
     *
     * @param n An unsigned 32-bit integer, stored in a signed int because Java has no explicit unsigned support.
     * @return A signed 32-bit integer.
     */
    public static int decodeZigZag32(final int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * Decode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be efficiently encoded
     * with varint. (Otherwise, negative values must be sign-extended to 64 bits to be varint encoded, thus always
     * taking 10 bytes on the wire.)
     *
     * @param n An unsigned 64-bit integer, stored in a signed int because Java has no explicit unsigned support.
     * @return A signed 64-bit integer.
     */
    public static long decodeZigZag64(final long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    // -----------------------------------------------------------------

    private final byte[] buffer;
    private int bufferSize;
    private int bufferSizeAfterLimit;
    private int bufferPos;
    private final InputStream input;
    private int lastTag;
    private int packedLimit = 0;

    /**
     * The total number valueOf bytes read beforeMethod the current buffer. The total bytes read up to the current position can be
     * computed as {@code totalBytesRetired + bufferPos}. This value may be negative if reading started in the middle valueOf
     * the current buffer (e.g. if the constructor that takes a byte array and an offset was used).
     */
    private int totalBytesRetired;

    /**
     * The absolute position valueOf the end valueOf the current pojo.
     */
    private int currentLimit = Integer.MAX_VALUE;

    // ** See setRecursionLimit() */
    // private int recursionDepth;
    // private int recursionLimit = DEFAULT_RECURSION_LIMIT;

    /**
     * If true, the nested messages are CACHE-encoded
     */
    public final boolean decodeNestedMessageAsGroup;

    /**
     * See setSizeLimit()
     */
    private int sizeLimit = DEFAULT_SIZE_LIMIT;

    // static final int DEFAULT_RECURSION_LIMIT = 64;
    static final int DEFAULT_SIZE_LIMIT = 64 << 20; // 64MB
    static final int DEFAULT_BUFFER_SIZE = 4096;

    public CodedInput(final byte[] buffer, final int off, final int len,
                      boolean decodeNestedMessageAsGroup) {
        this.buffer = buffer;
        bufferSize = off + len;
        bufferPos = off;
        totalBytesRetired = -off;
        input = null;
        this.decodeNestedMessageAsGroup = decodeNestedMessageAsGroup;
    }

    public CodedInput(final InputStream input, boolean decodeNestedMessageAsGroup) {
        this(input, new byte[DEFAULT_BUFFER_SIZE], 0, 0, decodeNestedMessageAsGroup);
    }

    public CodedInput(final InputStream input, byte[] buffer,
                      boolean decodeNestedMessageAsGroup) {
        this(input, buffer, 0, 0, decodeNestedMessageAsGroup);
    }

    public CodedInput(final InputStream input, byte[] buffer, int offset, int limit,
                      boolean decodeNestedMessageAsGroup) {
        this.buffer = buffer;
        bufferSize = limit;
        bufferPos = offset;
        totalBytesRetired = -offset;
        this.input = input;
        this.decodeNestedMessageAsGroup = decodeNestedMessageAsGroup;
    }

    /*
     * Set the maximum pojo recursion depth. In order to prevent malicious messages from causing stack overflows,
     * {@code CodedInput} limits how deeply messages may be nested. The default limit is 64.
     *
     * @return the old limit.
     *
     * public int setRecursionLimit(final int limit) { if (limit < 0) { throw new IllegalArgumentException(
     * "Recursion limit cannot be negative: " + limit); } final int oldLimit = recursionLimit; recursionLimit = limit;
     * return oldLimit; }
     */

    /**
     * Set the maximum pojo size. In order to prevent malicious messages from exhausting memory or causing integer
     * overflows, {@code CodedInput} limits how large a pojo may be. The default limit is 64MB. You should set this
     * limit as small as you can without harming your app's functionality. Note that size limits only apply when reading
     * from an {@code InputStream}, not when constructed around a raw byte array.
     * <p>
     * If you want to read several messages from a single CodedInput, you could sync {@link #resetSizeCounter()} execAfter
     * each one to avoid hitting the size limit.
     *
     * @return the old limit.
     */
    public int setSizeLimit(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException(
                    "Size limit cannot be negative: " + limit);
        }
        final int oldLimit = sizeLimit;
        sizeLimit = limit;
        return oldLimit;
    }

    /**
     * Resets the current size counter to zero (see {@link #setSizeLimit(int)}). The field {@code totalBytesRetired}
     * will be negative if the initial position was not zero.
     */
    public void resetSizeCounter() {
        totalBytesRetired = -bufferPos;
    }

    /**
     * Resets the buffer position and limit to re-use this CodedInput object.
     */
    public void reset() {
        this.bufferSize = 0;
        this.bufferPos = 0;
        this.bufferSizeAfterLimit = 0;
        this.currentLimit = Integer.MAX_VALUE;
        this.lastTag = 0;
        this.packedLimit = 0;
        this.sizeLimit = DEFAULT_SIZE_LIMIT;
        resetSizeCounter();
    }

    /**
     * Note that {@code pushLimit()} does NOT affect how many bytes the {@code CodedInputStream} reads from an
     * underlying {@code InputStream} when refreshing its buffer. If you need to prevent reading past a certain point in
     * the underlying {@code InputStream} (e.g. because you expect it to contain more data execAfter the end valueOf the pojo
     * which you need to callback differently) then you must place a wrapper around your {@code InputStream} which limits
     * the amount valueOf data that can be read from it.
     *
     * @return the old limit.
     */
    public int pushLimit(int byteLimit) throws ProtobufException {
        if (byteLimit < 0) {
            throw ProtobufException.negativeSize();
        }
        byteLimit += totalBytesRetired + bufferPos;
        final int oldLimit = currentLimit;
        if (byteLimit > oldLimit) {
            throw ProtobufException.truncatedMessage();
        }
        currentLimit = byteLimit;

        recomputeBufferSizeAfterLimit();

        return oldLimit;
    }

    private void recomputeBufferSizeAfterLimit() {
        bufferSize += bufferSizeAfterLimit;
        final int bufferEnd = totalBytesRetired + bufferSize;
        if (bufferEnd > currentLimit) {
            // Limit is in current buffer.
            bufferSizeAfterLimit = bufferEnd - currentLimit;
            bufferSize -= bufferSizeAfterLimit;
        } else {
            bufferSizeAfterLimit = 0;
        }
    }

    /**
     * Discards the current limit, returning to the previous limit.
     *
     * @param oldLimit The old limit, as returned by {@code pushLimit}.
     */
    public void popLimit(final int oldLimit) {
        currentLimit = oldLimit;
        recomputeBufferSizeAfterLimit();
    }

    /**
     * Returns the number valueOf bytes to be read beforeMethod the current limit. If no limit is set, returns -1.
     */
    public int getBytesUntilLimit() {
        if (currentLimit == Integer.MAX_VALUE) {
            return -1;
        }

        final int currentAbsolutePosition = totalBytesRetired + bufferPos;
        return currentLimit - currentAbsolutePosition;
    }

    /**
     * Return true if currently reading packed field
     */
    public boolean isCurrentFieldPacked() {
        return packedLimit != 0 && packedLimit != getTotalBytesRead();
    }

    /**
     * Returns true if the stream has reached the end valueOf the input. This is the case if either the end valueOf the underlying
     * input source has been reached or if the stream has reached a limit created using {@link #pushLimit(int)}.
     */
    public boolean isAtEnd() throws IOException {
        return bufferPos == bufferSize && !refillBuffer(false);
    }

    /**
     * The total bytes read up to the current position. Calling {@link #resetSizeCounter()} resets this value to zero.
     */
    public int getTotalBytesRead() {
        return totalBytesRetired + bufferPos;
    }

    /**
     * Called with {@code this.buffer} is empty to read more bytes from the input. If {@code mustSucceed} is true,
     * refillBuffer() guarantees that either there will be at least one byte in the buffer when it returns or it will
     * throw an exception. If {@code mustSucceed} is false, refillBuffer() returns false if no more bytes were
     * available.
     */
    private boolean refillBuffer(final boolean mustSucceed) throws IOException {
        if (bufferPos < bufferSize) {
            throw new IllegalStateException(
                    "refillBuffer() called when buffer wasn't empty.");
        }

        if (totalBytesRetired + bufferSize == currentLimit) {
            // Oops, we hit a limit.
            if (mustSucceed) {
                throw ProtobufException.truncatedMessage();
            } else {
                return false;
            }
        }

        totalBytesRetired += bufferSize;

        bufferPos = 0;
        bufferSize = (input == null) ? -1 : input.read(buffer);
        if (bufferSize == 0 || bufferSize < -1) {
            throw new IllegalStateException(
                    "InputStream#read(byte[]) returned invalid result: " + bufferSize +
                            "\nThe InputStream implementation is buggy.");
        }
        if (bufferSize == -1) {
            bufferSize = 0;
            if (mustSucceed) {
                throw ProtobufException.truncatedMessage();
            } else {
                return false;
            }
        } else {
            recomputeBufferSizeAfterLimit();
            final int totalBytesRead =
                    totalBytesRetired + bufferSize + bufferSizeAfterLimit;
            if (totalBytesRead > sizeLimit || totalBytesRead < 0) {
                throw ProtobufException.sizeLimitExceeded();
            }
            return true;
        }
    }

    /**
     * Read one byte from the input.
     *
     * @throws ProtobufException The end valueOf the stream or the current limit was reached.
     */
    public byte readRawByte() throws IOException {
        if (bufferPos == bufferSize) {
            refillBuffer(true);
        }
        return buffer[bufferPos++];
    }

    /**
     * Read a fixed size valueOf bytes from the input.
     *
     * @throws ProtobufException The end valueOf the stream or the current limit was reached.
     */
    public byte[] readRawBytes(final int size) throws IOException {
        if (size < 0) {
            throw ProtobufException.negativeSize();
        }

        if (totalBytesRetired + bufferPos + size > currentLimit) {
            // Read to the end valueOf the stream anyway.
            skipRawBytes(currentLimit - totalBytesRetired - bufferPos);
            // Then fail.
            throw ProtobufException.truncatedMessage();
        }

        if (size <= bufferSize - bufferPos) {
            // We have all the bytes we need already.
            final byte[] bytes = new byte[size];
            System.arraycopy(buffer, bufferPos, bytes, 0, size);
            bufferPos += size;
            return bytes;
        } else if (size < buffer.length) {
            // Reading more bytes than are in the buffer, but not an excessive number
            // valueOf bytes. We can safely allocate the resulting array ahead valueOf time.

            // First copy what we have.
            final byte[] bytes = new byte[size];
            int pos = bufferSize - bufferPos;
            System.arraycopy(buffer, bufferPos, bytes, 0, pos);
            bufferPos = bufferSize;

            // We want to use refillBuffer() and then copy from the buffer into our
            // byte array rather than reading directly into our byte array because
            // the input may be unbuffered.
            refillBuffer(true);

            while (size - pos > bufferSize) {
                System.arraycopy(buffer, 0, bytes, pos, bufferSize);
                pos += bufferSize;
                bufferPos = bufferSize;
                refillBuffer(true);
            }

            System.arraycopy(buffer, 0, bytes, pos, size - pos);
            bufferPos = size - pos;

            return bytes;
        } else {
            // The size is very large. For security reasons, we can't allocate the
            // entire byte array yet. The size comes directly from the input, so a
            // maliciously-crafted pojo could provide a bogus very large size in
            // order to trick the app into allocating a lot valueOf memory. We avoid this
            // by allocating and reading only a small chunk at a time, so that the
            // malicious pojo must actually *be* extremely large to cause
            // problems. Meanwhile, we limit the allowed size valueOf a pojo elsewhere.

            // Remember the buffer markers since we'll have to copy the bytes out valueOf
            // it later.
            final int originalBufferPos = bufferPos;
            final int originalBufferSize = bufferSize;

            // Mark the current buffer consumed.
            totalBytesRetired += bufferSize;
            bufferPos = 0;
            bufferSize = 0;

            // Read all the rest valueOf the bytes we need.
            int sizeLeft = size - (originalBufferSize - originalBufferPos);
            final List<byte[]> chunks = new ArrayList<byte[]>();

            while (sizeLeft > 0) {
                final byte[] chunk = new byte[Math.min(sizeLeft, buffer.length)];
                int pos = 0;
                while (pos < chunk.length) {
                    final int n = (input == null) ? -1 :
                            input.read(chunk, pos, chunk.length - pos);
                    if (n == -1) {
                        throw ProtobufException.truncatedMessage();
                    }
                    totalBytesRetired += n;
                    pos += n;
                }
                sizeLeft -= chunk.length;
                chunks.add(chunk);
            }

            // OK, got everything. Now concatenate it all into one buffer.
            final byte[] bytes = new byte[size];

            // Start by copying the leftover bytes from this.buffer.
            int pos = originalBufferSize - originalBufferPos;
            System.arraycopy(buffer, originalBufferPos, bytes, 0, pos);

            // And now all the chunks.
            for (final byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, bytes, pos, chunk.length);
                pos += chunk.length;
            }

            // Done.
            return bytes;
        }
    }

    /**
     * Reads and discards {@code size} bytes.
     *
     * @throws ProtobufException The end valueOf the stream or the current limit was reached.
     */
    public void skipRawBytes(final int size) throws IOException {
        if (size < 0) {
            throw ProtobufException.negativeSize();
        }

        if (totalBytesRetired + bufferPos + size > currentLimit) {
            // Read to the end valueOf the stream anyway.
            skipRawBytes(currentLimit - totalBytesRetired - bufferPos);
            // Then fail.
            throw ProtobufException.truncatedMessage();
        }

        if (size <= bufferSize - bufferPos) {
            // We have all the bytes we need already.
            bufferPos += size;
        } else {
            // Skipping more bytes than are in the buffer. First skip what we have.
            int pos = bufferSize - bufferPos;
            bufferPos = bufferSize;

            // Keep refilling the buffer until we get to the point we wanted to skip
            // to. This has the side effect valueOf ensuring the limits are updated
            // correctly.
            refillBuffer(true);
            while (size - pos > bufferSize) {
                pos += bufferSize;
                bufferPos = bufferSize;
                refillBuffer(true);
            }

            bufferPos = size - pos;
        }
    }

    // START EXTRA
    @Override
    public <T> int readFieldNumber(Schema<T> schema) throws IOException {
        if (isAtEnd()) {
            lastTag = 0;
            return 0;
        }

        // are we reading packed field?
        if (isCurrentFieldPacked()) {
            if (packedLimit < getTotalBytesRead())
                throw ProtobufException.misreportedSize();

            // Return field number while reading packed field
            return lastTag >>> WireFormat.TAG_TYPE_BITS;
        }

        packedLimit = 0;
        final int tag = readRawVarint32();
        final int fieldNumber = tag >>> WireFormat.TAG_TYPE_BITS;
        if (fieldNumber == 0) {
            if (decodeNestedMessageAsGroup && WireFormat.WIRETYPE_TAIL_DELIMITER == (tag & WireFormat.TAG_TYPE_MASK)) {
                // protostuff's tail delimiter for streaming
                // 2 options: length-delimited or tail-delimited.
                lastTag = 0;
                return 0;
            }
            // If we actually read zero, that's not a valid tag.
            throw ProtobufException.invalidTag();
        }
        if (decodeNestedMessageAsGroup && WireFormat.WIRETYPE_END_GROUP == (tag & WireFormat.TAG_TYPE_MASK)) {
            lastTag = 0;
            return 0;
        }

        lastTag = tag;
        return fieldNumber;
    }

    /**
     * Check if this field have been packed into a length-delimited field. If so, update internal state to reflect that
     * packed fields are being read.
     *
     * @throws IOException
     */
    private void checkIfPackedField() throws IOException {
        // Do we have the start valueOf a packed field?
        if (packedLimit == 0 && WireFormat.getTagWireType(lastTag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
            final int length = readRawVarint32();
            if (length < 0)
                throw ProtobufException.negativeSize();

            this.packedLimit = getTotalBytesRead() + length;
        }
    }

    @Override
    public byte[] readByteArray() throws IOException {
        final int size = readRawVarint32();
        if (size <= (bufferSize - bufferPos) && size > 0) {
            // Fast path: We already have the bytes in a contiguous buffer, so
            // just copy directly from it.
            final byte[] copy = new byte[size];
            System.arraycopy(buffer, bufferPos, copy, 0, size);
            bufferPos += size;
            return copy;
        } else {
            // Slow path: Build a byte array first then copy it.
            return readRawBytes(size);
        }
    }

    @Override
    public <T> void handleUnknownField(int fieldNumber, Schema<T> schema) throws IOException {
        skipField(lastTag);
    }

    @Override
    public void transferByteRangeTo(Output output, boolean utf8String, int fieldNumber,
                                    boolean repeated) throws IOException {
        final int size = readRawVarint32();
        if (size <= (bufferSize - bufferPos) && size > 0) {
            // Fast path: We already have the bytes in a contiguous buffer
            output.writeByteRange(utf8String, fieldNumber, buffer, bufferPos, size, repeated);
            bufferPos += size;
        } else {
            // Slow path: Build a byte array first then copy it.
            output.writeByteRange(utf8String, fieldNumber, readRawBytes(size), 0, size, repeated);
        }
    }

    /**
     * Returns the last tag.
     */
    public int getLastTag() {
        return lastTag;
    }

    /**
     * Reads a byte array/ByteBuffer value.
     */
    @Override
    public ByteBuffer readByteBuffer() throws IOException {
        return ByteBuffer.wrap(readByteArray());
    }

    // END EXTRA
}
