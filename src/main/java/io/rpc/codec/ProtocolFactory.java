package io.rpc.codec;

public interface ProtocolFactory {

    ProtocolEncoder newEncoder();

    ProtocolDecoder newDecoder();

    static <E extends ProtocolEncoder, D extends ProtocolDecoder> ProtocolFactory newFactory(Class<E> encoder, Class<D> decoder) {
        return new DefaultProtocolFactory<>(encoder, decoder);
    }
}
