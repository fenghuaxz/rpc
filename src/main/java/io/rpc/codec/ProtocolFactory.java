package io.rpc.codec;

import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

public interface ProtocolFactory<I> {

    MessageToByteEncoder<I> newEncoder();

    ByteToMessageDecoder newDecoder();

    static <I, E extends MessageToByteEncoder<I>, D extends ByteToMessageDecoder> ProtocolFactory<I> newFactory(Class<E> encoder, Class<D> decoder) {
        return new DefaultProtocolFactory<>(encoder, decoder);
    }
}
