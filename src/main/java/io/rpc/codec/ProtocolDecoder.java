package io.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import io.rpc.beans.Ping;
import io.rpc.beans.Request;
import io.rpc.beans.Response;
import io.rpc.protostuff.GraphIOUtil;
import io.rpc.protostuff.Schema;
import io.rpc.protostuff.runtime.RuntimeSchema;

public class ProtocolDecoder extends LengthFieldBasedFrameDecoder {

    public ProtocolDecoder() {
        this(32 * 1024);
    }

    public ProtocolDecoder(int maxFrameLength) {
        super(maxFrameLength, 1, 4, 0, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if ((in = (ByteBuf) super.decode(ctx, in)) == null) {
            return null;
        }

        byte magic = in.readByte();
        int len = in.readInt();

        byte[] data = new byte[len];
        in.readBytes(data);
        ReferenceCountUtil.release(in);

        Class<?> magicType;

        switch (magic) {

            case 0xa:
                return Ping.PING;

            case 0xb:
                magicType = Request.class;
                break;

            case 0xc:
                magicType = Response.class;
                break;

            default:
                ctx.close();
                return null;
        }
        return decode(data, magicType);
    }

    protected <T> T decode(byte[] data, Class<T> clazz) {
        T message;
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        GraphIOUtil.mergeFrom(data, message = schema.newMessage(), schema);
        return message;
    }
}
