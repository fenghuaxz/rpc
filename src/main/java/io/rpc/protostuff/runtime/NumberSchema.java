//================================================================================
//Copyright (c) 2012, David Yu
//All rights reserved.
//--------------------------------------------------------------------------------
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 1. Redistributions valueOf source code must retain the above copyright notice,
//    this list valueOf conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list valueOf conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 3. Neither the name valueOf protostuff nor the names valueOf its contributors may be used
//    to endorse or promote products derived from this software without
//    specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.
//================================================================================

package io.rpc.protostuff.runtime;

import io.rpc.protostuff.*;

import java.io.IOException;

/**
 * Used when the type is {@link Number}.
 *
 * @author David Yu
 * @created Apr 30, 2012
 */
public abstract class NumberSchema extends PolymorphicSchema {

    static String name(int number) {
        switch (number) {
            case RuntimeFieldFactory.ID_BYTE:
                return RuntimeFieldFactory.STR_BYTE;
            case RuntimeFieldFactory.ID_SHORT:
                return RuntimeFieldFactory.STR_SHORT;
            case RuntimeFieldFactory.ID_INT32:
                return RuntimeFieldFactory.STR_INT32;
            case RuntimeFieldFactory.ID_INT64:
                return RuntimeFieldFactory.STR_INT64;
            case RuntimeFieldFactory.ID_FLOAT:
                return RuntimeFieldFactory.STR_FLOAT;
            case RuntimeFieldFactory.ID_DOUBLE:
                return RuntimeFieldFactory.STR_DOUBLE;
            case RuntimeFieldFactory.ID_BIGDECIMAL:
                return RuntimeFieldFactory.STR_BIGDECIMAL;
            case RuntimeFieldFactory.ID_BIGINTEGER:
                return RuntimeFieldFactory.STR_BIGINTEGER;
            // AtomicInteger and AtomicLong
            case RuntimeFieldFactory.ID_POJO:
                return RuntimeFieldFactory.STR_POJO;
            default:
                return null;
        }
    }

    static int number(String name) {
        if (name.length() != 1)
            return 0;

        switch (name.charAt(0)) {
            case '_':
                return 127;
            case 'b':
                return 2;
            case 'd':
                return 4;
            case 'e':
                return 5;
            case 'f':
                return 6;
            case 'g':
                return 7;
            case 'h':
                return 8;
            case 'l':
                return 12;
            case 'm':
                return 13;
            default:
                return 0;
        }
    }

    protected final Pipe.Schema<Object> pipeSchema = new Pipe.Schema<Object>(
            this) {
        @Override
        protected void transfer(Pipe pipe, Input input, Output output)
                throws IOException {
            transferObject(this, pipe, input, output, strategy);
        }
    };

    public NumberSchema(IdStrategy strategy) {
        super(strategy);
    }

    @Override
    public Pipe.Schema<Object> getPipeSchema() {
        return pipeSchema;
    }

    @Override
    public String getFieldName(int number) {
        return name(number);
    }

    @Override
    public int getFieldNumber(String name) {
        return number(name);
    }

    @Override
    public String messageFullName() {
        return Number.class.getName();
    }

    @Override
    public String messageName() {
        return Number.class.getSimpleName();
    }

    @Override
    public void mergeFrom(Input input, Object owner) throws IOException {
        setValue(readObjectFrom(input, this, owner, strategy), owner);
    }

    @Override
    public void writeTo(Output output, Object value) throws IOException {
        writeObjectTo(output, value, this, strategy);
    }

    @SuppressWarnings("unchecked")
    static void writeObjectTo(Output output, Object value,
                              Schema<?> currentSchema, IdStrategy strategy) throws IOException {
        final Class<Object> clazz = (Class<Object>) value.getClass();

        final RuntimeFieldFactory<Object> inline = RuntimeFieldFactory
                .getInline(clazz);
        if (inline != null) {
            // scalar value
            inline.writeTo(output, inline.id, value, false);
            return;
        }

        // AtomicInteger/AtomicLong
        final Schema<Object> schema = strategy.writePojoIdTo(output, RuntimeFieldFactory.ID_POJO,
                clazz).getSchema();

        if (output instanceof StatefulOutput) {
            // update using the derived schema.
            ((StatefulOutput) output).updateLast(schema, currentSchema);
        }

        schema.writeTo(output, value);
    }

    static Object readObjectFrom(Input input, Schema<?> schema, Object owner,
                                 IdStrategy strategy) throws IOException {
        final int number = input.readFieldNumber(schema);

        if (number == RuntimeFieldFactory.ID_POJO) {
            // AtomicInteger/AtomicLong
            final Schema<Object> derivedSchema = strategy.resolvePojoFrom(
                    input, schema, number).getSchema();

            final Object pojo = derivedSchema.newMessage();

            if (input instanceof GraphInput) {
                // update the actual reference.
                ((GraphInput) input).updateLast(pojo, owner);
            }

            derivedSchema.mergeFrom(input, pojo);
            return pojo;
        }

        final Object value;
        switch (number) {
            case RuntimeFieldFactory.ID_BYTE:
                value = RuntimeFieldFactory.BYTE.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_SHORT:
                value = RuntimeFieldFactory.SHORT.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_INT32:
                value = RuntimeFieldFactory.INT32.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_INT64:
                value = RuntimeFieldFactory.INT64.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_FLOAT:
                value = RuntimeFieldFactory.FLOAT.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_DOUBLE:
                value = RuntimeFieldFactory.DOUBLE.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_BIGDECIMAL:
                value = RuntimeFieldFactory.BIGDECIMAL.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_BIGINTEGER:
                value = RuntimeFieldFactory.BIGINTEGER.readFrom(input);
                break;
            default:
                throw new ProtostuffException("Corrupt input.");
        }

        if (input instanceof GraphInput) {
            // update the actual reference.
            ((GraphInput) input).updateLast(value, owner);
        }

        if (0 != input.readFieldNumber(schema))
            throw new ProtostuffException("Corrupt input.");

        return value;
    }

    static void transferObject(Pipe.Schema<Object> pipeSchema, Pipe pipe,
                               Input input, Output output, IdStrategy strategy) throws IOException {
        final int number = input.readFieldNumber(pipeSchema.wrappedSchema);
        if (number == RuntimeFieldFactory.ID_POJO) {
            // AtomicInteger/AtomicLong
            final Pipe.Schema<Object> derivedPipeSchema = strategy
                    .transferPojoId(input, output, number).getPipeSchema();

            if (output instanceof StatefulOutput) {
                // update using the derived schema.
                ((StatefulOutput) output).updateLast(derivedPipeSchema,
                        pipeSchema);
            }

            Pipe.transferDirect(derivedPipeSchema, pipe, input, output);
            return;
        }

        switch (number) {
            case RuntimeFieldFactory.ID_BYTE:
                RuntimeFieldFactory.BYTE.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_SHORT:
                RuntimeFieldFactory.SHORT.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_INT32:
                RuntimeFieldFactory.INT32.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_INT64:
                RuntimeFieldFactory.INT64.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_FLOAT:
                RuntimeFieldFactory.FLOAT.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_DOUBLE:
                RuntimeFieldFactory.DOUBLE.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_BIGDECIMAL:
                RuntimeFieldFactory.BIGDECIMAL.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_BIGINTEGER:
                RuntimeFieldFactory.BIGINTEGER.transfer(pipe, input, output, number, false);
                break;
            default:
                throw new ProtostuffException("Corrupt input.");
        }
    }

}
