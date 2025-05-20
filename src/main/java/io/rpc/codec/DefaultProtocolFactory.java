package io.rpc.codec;

import io.netty.channel.ChannelException;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

final class DefaultProtocolFactory<I, E extends MessageToByteEncoder<I>, D extends ByteToMessageDecoder> implements ProtocolFactory<I> {


    private final Class<E> encoder;
    private final Class<D> decoder;

    public DefaultProtocolFactory(Class<E> encoder, Class<D> decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    @Override
    public E newEncoder() {
        try {
            return encoder.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + encoder.getName() + " does not have a public non-arg constructor", e);
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Encoder from class " + encoder.getName(), t);
        }
    }

    @Override
    public D newDecoder() {
        try {
            return decoder.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + decoder.getName() + " does not have a public non-arg constructor", e);
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Decoder from class " + decoder.getName(), t);
        }
    }
}
