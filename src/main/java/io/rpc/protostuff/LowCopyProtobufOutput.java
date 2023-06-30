package io.rpc.protostuff;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Output that differs from the standard by attempting to avoid extra copies valueOf large ByteBuffer fields. When used with
 * ByteBuffer=true compiler option, we can splice in ByteBuffer objects without copying them. Most valueOf the magic lives in
 * LinkBuffer, so this class exists just to toBytes to a LinkBuffer.
 *
 * @author Ryan Rawson
 */
public final class LowCopyProtobufOutput implements Output {

    public LinkBuffer buffer;

    public LowCopyProtobufOutput() {
        buffer = new LinkBuffer();
    }

    public LowCopyProtobufOutput(final LinkBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void writeInt32(int fieldNumber, int value, boolean repeated) throws IOException {
        if (value < 0) {
            buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_VARINT));
            buffer.writeVarInt64(value);
        } else {
            buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_VARINT));
            buffer.writeVarInt32(value);
        }
    }

    @Override
    public void writeUInt32(int fieldNumber, int value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_VARINT));
        buffer.writeVarInt32(value);
    }

    @Override
    public void writeSInt32(int fieldNumber, int value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_VARINT));
        buffer.writeVarInt32(ProtobufOutput.encodeZigZag32(value));
    }

    @Override
    public void writeFixed32(int fieldNumber, int value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_FIXED32));
        buffer.writeInt32LE(value);
    }

    @Override
    public void writeSFixed32(int fieldNumber, int value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_FIXED32));
        buffer.writeInt32LE(value);
    }

    @Override
    public void writeInt64(int fieldNumber, long value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_VARINT));
        buffer.writeVarInt64(value);
    }

    @Override
    public void writeUInt64(int fieldNumber, long value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_VARINT));
        buffer.writeVarInt64(value);
    }

    @Override
    public void writeSInt64(int fieldNumber, long value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_VARINT));
        buffer.writeVarInt64(ProtobufOutput.encodeZigZag64(value));
    }

    @Override
    public void writeFixed64(int fieldNumber, long value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_FIXED64));
        buffer.writeInt64LE(value);
    }

    @Override
    public void writeSFixed64(int fieldNumber, long value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_FIXED64));
        buffer.writeInt64LE(value);
    }

    @Override
    public void writeFloat(int fieldNumber, float value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_FIXED32));
        buffer.writeInt32LE(Float.floatToRawIntBits(value));
    }

    @Override
    public void writeDouble(int fieldNumber, double value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_FIXED64));
        buffer.writeInt64LE(Double.doubleToRawLongBits(value));
    }

    @Override
    public void writeBool(int fieldNumber, boolean value, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_VARINT));
        buffer.writeByte(value ? (byte) 0x01 : 0x00);
    }

    @Override
    public void writeEnum(int fieldNumber, int number, boolean repeated) throws IOException {
        writeInt32(fieldNumber, number, repeated);
    }

    @Override
    public void writeString(int fieldNumber, CharSequence value, boolean repeated) throws IOException {
        // TODO the original implementation is a lot more complex, is this compatible?
        byte[] strbytes = value.toString().getBytes(StandardCharsets.UTF_8);
        writeByteArray(fieldNumber, strbytes, repeated);
    }

    @Override
    public void writeBytes(int fieldNumber, ByteString value, boolean repeated) throws IOException {
        writeByteArray(fieldNumber, value.getBytes(), repeated);
    }

    @Override
    public void writeByteArray(int fieldNumber, byte[] bytes, boolean repeated) throws IOException {
        writeByteRange(false, fieldNumber, bytes, 0, bytes.length, repeated);
    }

    @Override
    public void writeByteRange(boolean utf8String, int fieldNumber, byte[] value,
                               int offset, int length, boolean repeated) throws IOException {
        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED));
        buffer.writeVarInt32(length);
        buffer.writeByteArray(value, offset, length);
    }

    @Override
    public <T> void writeObject(final int fieldNumber, final T value, final Schema<T> schema,
                                final boolean repeated) throws IOException {
        LinkBuffer subBuf = new LinkBuffer(buffer.allocSize);
        // now toBytes:
        LowCopyProtobufOutput subOutput = new LowCopyProtobufOutput(subBuf);
        schema.writeTo(subOutput, value);
        List<ByteBuffer> subBuffers = subBuf.finish();

        long subSize = subBuf.size();

        buffer.writeVarInt32(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED));
        buffer.writeVarInt64(subSize);
        for (ByteBuffer b : subBuffers) {
            buffer.writeByteBuffer(b);
        }
    }

    @Override
    public void writeBytes(int fieldNumber, ByteBuffer value, boolean repeated) throws IOException {
        writeByteRange(false, fieldNumber, value.array(), value.arrayOffset() + value.position(),
                value.remaining(), repeated);
    }
}
