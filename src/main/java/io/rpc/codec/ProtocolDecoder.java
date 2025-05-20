package io.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import io.rpc.beans.Ping;
import io.rpc.beans.Request;
import io.rpc.beans.Response;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class ProtocolDecoder extends LengthFieldBasedFrameDecoder {

    public ProtocolDecoder() {
        this(32 * 1024);
    }

    public ProtocolDecoder(int maxFrameLength) {
        this(maxFrameLength, 1, 4, 0, 0);
    }

    public ProtocolDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
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

    @SuppressWarnings("unchecked")
    protected <T> T decode(byte[] data, Class<T> clazz) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        return (T) ois.readObject();
    }
}
