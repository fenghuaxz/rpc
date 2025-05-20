package io.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.rpc.beans.Ping;
import io.rpc.beans.Request;
import io.rpc.beans.Response;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class ProtocolEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof Ping) {
            out.writeByte(0xa);
            out.writeInt(0);
            return;
        }

        if (msg instanceof Request) {
            byte[] data = encode(msg);
            out.writeByte(0xb);
            out.writeInt(data.length);
            out.writeBytes(data);
            return;
        }

        if (msg instanceof Response) {
            byte[] data = encode(msg);
            out.writeByte(0xc);
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }

    protected <T> byte[] encode(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
        return bos.toByteArray();
    }
}
