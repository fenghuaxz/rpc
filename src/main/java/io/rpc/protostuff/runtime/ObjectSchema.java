//========================================================================
//Copyright 2007-2011 David Yu dyuproject@gmail.com
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

package io.rpc.protostuff.runtime;

import io.rpc.protostuff.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * A schema for dynamic types (fields where the type is {@link Object}).
 *
 * @author David Yu
 * @created Feb 1, 2011
 */
public abstract class ObjectSchema extends PolymorphicSchema {

    static final int ID_ENUM_VALUE = 1;
    static final int ID_ARRAY_LEN = 3;
    static final int ID_ARRAY_DIMENSION = 2;

    static String name(int number) {
        switch (number) {
            case RuntimeFieldFactory.ID_POLYMORPHIC_COLLECTION:
                return RuntimeFieldFactory.STR_POLYMORPHIC_COLLECTION;
            case RuntimeFieldFactory.ID_POLYMORPHIC_MAP:
                return RuntimeFieldFactory.STR_POLYMOPRHIC_MAP;
            case RuntimeFieldFactory.ID_DELEGATE:
                return RuntimeFieldFactory.STR_DELEGATE;

            case RuntimeFieldFactory.ID_ARRAY_DELEGATE:
                return RuntimeFieldFactory.STR_ARRAY_DELEGATE;
            case RuntimeFieldFactory.ID_ARRAY_SCALAR:
                return RuntimeFieldFactory.STR_ARRAY_SCALAR;
            case RuntimeFieldFactory.ID_ARRAY_ENUM:
                return RuntimeFieldFactory.STR_ARRAY_ENUM;
            case RuntimeFieldFactory.ID_ARRAY_POJO:
                return RuntimeFieldFactory.STR_ARRAY_POJO;

            case RuntimeFieldFactory.ID_THROWABLE:
                return RuntimeFieldFactory.STR_THROWABLE;

            case RuntimeFieldFactory.ID_BOOL:
                return RuntimeFieldFactory.STR_BOOL;
            case RuntimeFieldFactory.ID_BYTE:
                return RuntimeFieldFactory.STR_BYTE;
            case RuntimeFieldFactory.ID_CHAR:
                return RuntimeFieldFactory.STR_CHAR;
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
            case RuntimeFieldFactory.ID_STRING:
                return RuntimeFieldFactory.STR_STRING;
            case RuntimeFieldFactory.ID_BYTES:
                return RuntimeFieldFactory.STR_BYTES;
            case RuntimeFieldFactory.ID_BYTE_ARRAY:
                return RuntimeFieldFactory.STR_BYTE_ARRAY;
            case RuntimeFieldFactory.ID_BIGDECIMAL:
                return RuntimeFieldFactory.STR_BIGDECIMAL;
            case RuntimeFieldFactory.ID_BIGINTEGER:
                return RuntimeFieldFactory.STR_BIGINTEGER;
            case RuntimeFieldFactory.ID_DATE:
                return RuntimeFieldFactory.STR_DATE;
            case RuntimeFieldFactory.ID_ARRAY:
                return RuntimeFieldFactory.STR_ARRAY;
            case RuntimeFieldFactory.ID_OBJECT:
                return RuntimeFieldFactory.STR_OBJECT;
            case RuntimeFieldFactory.ID_ARRAY_MAPPED:
                return RuntimeFieldFactory.STR_ARRAY_MAPPED;
            case RuntimeFieldFactory.ID_CLASS:
                return RuntimeFieldFactory.STR_CLASS;
            case RuntimeFieldFactory.ID_CLASS_MAPPED:
                return RuntimeFieldFactory.STR_CLASS_MAPPED;
            case RuntimeFieldFactory.ID_CLASS_ARRAY:
                return RuntimeFieldFactory.STR_CLASS_ARRAY;
            case RuntimeFieldFactory.ID_CLASS_ARRAY_MAPPED:
                return RuntimeFieldFactory.STR_CLASS_ARRAY_MAPPED;

            case RuntimeFieldFactory.ID_ENUM_SET:
                return RuntimeFieldFactory.STR_ENUM_SET;
            case RuntimeFieldFactory.ID_ENUM_MAP:
                return RuntimeFieldFactory.STR_ENUM_MAP;
            case RuntimeFieldFactory.ID_ENUM:
                return RuntimeFieldFactory.STR_ENUM;
            case RuntimeFieldFactory.ID_COLLECTION:
                return RuntimeFieldFactory.STR_COLLECTION;
            case RuntimeFieldFactory.ID_MAP:
                return RuntimeFieldFactory.STR_MAP;

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
            case 'B':
                return 28;
            case 'C':
                return 29;
            case 'D':
                return 30;

            case 'F':
                return 32;
            case 'G':
                return 33;
            case 'H':
                return 34;
            case 'I':
                return 35;

            case 'Z':
                return 52;
            case '_':
                return 127;

            case 'a':
                return 1;
            case 'b':
                return 2;
            case 'c':
                return 3;
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
            case 'i':
                return 9;
            case 'j':
                return 10;
            case 'k':
                return 11;
            case 'l':
                return 12;
            case 'm':
                return 13;
            case 'n':
                return 14;
            case 'o':
                return 15;
            case 'p':
                return 16;
            case 'q':
                return 17;
            case 'r':
                return 18;
            case 's':
                return 19;
            case 't':
                return 20;
            case 'u':
                return 21;

            case 'v':
                return 22;
            case 'w':
                return 23;
            case 'x':
                return 24;
            case 'y':
                return 25;
            case 'z':
                return 26;
            default:
                return 0;
        }
    }

    protected final Pipe.Schema<Object> pipeSchema = new Pipe.Schema<Object>(this) {
        @Override
        protected void transfer(Pipe pipe, Input input, Output output) throws IOException {
            transferObject(this, pipe, input, output, strategy);
        }
    };

    public ObjectSchema(IdStrategy strategy) {
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
        return Object.class.getName();
    }

    @Override
    public String messageName() {
        return Object.class.getSimpleName();
    }

    @Override
    public void mergeFrom(Input input, Object owner) throws IOException {
        setValue(readObjectFrom(input, this, owner, strategy), owner);
    }

    @Override
    public void writeTo(Output output, Object value) throws IOException {
        writeObjectTo(output, value, this, strategy);
    }

    static ArrayWrapper newArrayWrapper(Input input, Schema<?> schema,
                                        boolean mapped, IdStrategy strategy) throws IOException {
        final Class<?> componentType = strategy.resolveArrayComponentTypeFrom(
                input, mapped);

        if (input.readFieldNumber(schema) != ID_ARRAY_LEN)
            throw new ProtostuffException("Corrupt input.");
        final int len = input.readUInt32();

        if (input.readFieldNumber(schema) != ID_ARRAY_DIMENSION)
            throw new ProtostuffException("Corrupt input.");
        final int dimensions = input.readUInt32();

        if (dimensions == 1)
            return new ArrayWrapper(Array.newInstance(componentType, len));

        final int[] arg = new int[dimensions];
        arg[0] = len;
        return new ArrayWrapper(Array.newInstance(componentType, arg));
    }

    static void transferArray(Pipe pipe, Input input, Output output, int number,
                              Pipe.Schema<?> pipeSchema, boolean mapped, IdStrategy strategy) throws IOException {
        strategy.transferArrayId(input, output, number, mapped);

        if (input.readFieldNumber(pipeSchema.wrappedSchema) != ID_ARRAY_LEN)
            throw new ProtostuffException("Corrupt input.");

        output.writeUInt32(ID_ARRAY_LEN, input.readUInt32(), false);

        if (input.readFieldNumber(pipeSchema.wrappedSchema) != ID_ARRAY_DIMENSION)
            throw new ProtostuffException("Corrupt input.");

        output.writeUInt32(ID_ARRAY_DIMENSION, input.readUInt32(), false);

        if (output instanceof StatefulOutput) {
            // update using the derived schema.
            ((StatefulOutput) output).updateLast(strategy.ARRAY_PIPE_SCHEMA, pipeSchema);
        }

        Pipe.transferDirect(strategy.ARRAY_PIPE_SCHEMA, pipe, input, output);
    }

    static void transferClass(Pipe pipe, Input input, Output output, int number,
                              Pipe.Schema<?> pipeSchema, boolean mapped, boolean array,
                              IdStrategy strategy) throws IOException {
        strategy.transferClassId(input, output, number, mapped, array);

        if (array) {
            if (input.readFieldNumber(pipeSchema.wrappedSchema) != ID_ARRAY_DIMENSION)
                throw new ProtostuffException("Corrupt input.");

            output.writeUInt32(ID_ARRAY_DIMENSION, input.readUInt32(), false);
        }
    }

    static Class<?> getArrayClass(Input input, Schema<?> schema,
                                  final Class<?> componentType) throws IOException {
        if (input.readFieldNumber(schema) != ID_ARRAY_DIMENSION)
            throw new ProtostuffException("Corrupt input.");
        final int dimensions = input.readUInt32();

        // TODO is there another way (reflection) to obtainType an array class?

        if (dimensions == 1)
            return Array.newInstance(componentType, 0).getClass();

        final int[] arg = new int[dimensions];
        arg[0] = 0;
        return Array.newInstance(componentType, arg).getClass();
    }

    @SuppressWarnings("unchecked")
    static Object readObjectFrom(final Input input, final Schema<?> schema,
                                 Object owner, IdStrategy strategy) throws IOException {
        Object value;
        final int number = input.readFieldNumber(schema);
        switch (number) {
            case RuntimeFieldFactory.ID_BOOL:
                value = RuntimeFieldFactory.BOOL.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_BYTE:
                value = RuntimeFieldFactory.BYTE.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_CHAR:
                value = RuntimeFieldFactory.CHAR.readFrom(input);
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
            case RuntimeFieldFactory.ID_STRING:
                value = RuntimeFieldFactory.STRING.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_BYTES:
                value = RuntimeFieldFactory.BYTES.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_BYTE_ARRAY:
                value = RuntimeFieldFactory.BYTE_ARRAY.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_BIGDECIMAL:
                value = RuntimeFieldFactory.BIGDECIMAL.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_BIGINTEGER:
                value = RuntimeFieldFactory.BIGINTEGER.readFrom(input);
                break;
            case RuntimeFieldFactory.ID_DATE:
                value = RuntimeFieldFactory.DATE.readFrom(input);
                break;

            case RuntimeFieldFactory.ID_ARRAY: {
                final ArrayWrapper arrayWrapper = newArrayWrapper(input, schema, false,
                        strategy);

                if (input instanceof GraphInput) {
                    // update the actual reference.
                    ((GraphInput) input).updateLast(arrayWrapper.array, owner);
                }

                strategy.COLLECTION_SCHEMA.mergeFrom(input, arrayWrapper);

                return arrayWrapper.array;
            }
            case RuntimeFieldFactory.ID_OBJECT:
                if (input.readUInt32() != 0)
                    throw new ProtostuffException("Corrupt input.");

                value = new Object();

                break;

            case RuntimeFieldFactory.ID_ARRAY_MAPPED: {
                final ArrayWrapper mArrayWrapper = newArrayWrapper(input, schema, true,
                        strategy);

                if (input instanceof GraphInput) {
                    // update the actual reference.
                    ((GraphInput) input).updateLast(mArrayWrapper.array, owner);
                }

                strategy.COLLECTION_SCHEMA.mergeFrom(input, mArrayWrapper);

                return mArrayWrapper.array;
            }
            case RuntimeFieldFactory.ID_CLASS:
                value = strategy.resolveClassFrom(input, false, false);
                break;
            case RuntimeFieldFactory.ID_CLASS_MAPPED:
                value = strategy.resolveClassFrom(input, true, false);
                break;
            case RuntimeFieldFactory.ID_CLASS_ARRAY:
                value = getArrayClass(input, schema,
                        strategy.resolveClassFrom(input, false, true));
                break;
            case RuntimeFieldFactory.ID_CLASS_ARRAY_MAPPED:
                value = getArrayClass(input, schema,
                        strategy.resolveClassFrom(input, true, true));
                break;

            case RuntimeFieldFactory.ID_ENUM: {
                final EnumIO<?> eio = strategy.resolveEnumFrom(input);

                if (input.readFieldNumber(schema) != ID_ENUM_VALUE)
                    throw new ProtostuffException("Corrupt input.");

                value = eio.readFrom(input);
                break;
            }
            case RuntimeFieldFactory.ID_ENUM_SET: {
                final Collection<?> es = strategy.resolveEnumFrom(input).newEnumSet();

                if (input instanceof GraphInput) {
                    // update the actual reference.
                    ((GraphInput) input).updateLast(es, owner);
                }

                strategy.COLLECTION_SCHEMA.mergeFrom(input, (Collection<Object>) es);

                return es;
            }
            case RuntimeFieldFactory.ID_ENUM_MAP: {
                final Map<?, Object> em = strategy.resolveEnumFrom(input).newEnumMap();

                if (input instanceof GraphInput) {
                    // update the actual reference.
                    ((GraphInput) input).updateLast(em, owner);
                }

                strategy.MAP_SCHEMA.mergeFrom(input, (Map<Object, Object>) em);

                return em;
            }
            case RuntimeFieldFactory.ID_COLLECTION: {
                final Collection<Object> collection = strategy.resolveCollectionFrom(
                        input).newMessage();

                if (input instanceof GraphInput) {
                    // update the actual reference.
                    ((GraphInput) input).updateLast(collection, owner);
                }

                strategy.COLLECTION_SCHEMA.mergeFrom(input, collection);

                return collection;
            }
            case RuntimeFieldFactory.ID_MAP: {
                final Map<Object, Object> map =
                        strategy.resolveMapFrom(input).newMessage();

                if (input instanceof GraphInput) {
                    // update the actual reference.
                    ((GraphInput) input).updateLast(map, owner);
                }

                strategy.MAP_SCHEMA.mergeFrom(input, map);

                return map;
            }
            case RuntimeFieldFactory.ID_POLYMORPHIC_COLLECTION: {
                if (0 != input.readUInt32())
                    throw new ProtostuffException("Corrupt input.");

                final Object collection = PolymorphicCollectionSchema.readObjectFrom(input,
                        strategy.POLYMORPHIC_COLLECTION_SCHEMA, owner, strategy);

                if (input instanceof GraphInput) {
                    // update the actual reference.
                    ((GraphInput) input).updateLast(collection, owner);
                }

                return collection;
            }
            case RuntimeFieldFactory.ID_POLYMORPHIC_MAP: {
                if (0 != input.readUInt32())
                    throw new ProtostuffException("Corrupt input.");

                final Object map = PolymorphicMapSchema.readObjectFrom(input,
                        strategy.POLYMORPHIC_MAP_SCHEMA, owner, strategy);

                if (input instanceof GraphInput) {
                    // update the actual reference.
                    ((GraphInput) input).updateLast(map, owner);
                }

                return map;
            }
            case RuntimeFieldFactory.ID_DELEGATE: {
                final HasDelegate<Object> hd = strategy.resolveDelegateFrom(input);
                if (1 != input.readFieldNumber(schema))
                    throw new ProtostuffException("Corrupt input.");

                value = hd.delegate.readFrom(input);
                break;
            }
            case RuntimeFieldFactory.ID_ARRAY_DELEGATE: {
                final HasDelegate<Object> hd = strategy.resolveDelegateFrom(input);

                return hd.genericElementSchema.readFrom(input, owner);
            }
            case RuntimeFieldFactory.ID_ARRAY_SCALAR: {
                final int arrayId = input.readUInt32(), id = ArraySchemas.toInlineId(arrayId);

                final ArraySchemas.Base arraySchema = ArraySchemas.getSchema(id,
                        ArraySchemas.isPrimitive(arrayId), strategy);

                return arraySchema.readFrom(input, owner);
            }
            case RuntimeFieldFactory.ID_ARRAY_ENUM: {
                final EnumIO<?> eio = strategy.resolveEnumFrom(input);

                return eio.genericElementSchema.readFrom(input, owner);
            }
            case RuntimeFieldFactory.ID_ARRAY_POJO: {
                final HasSchema<Object> hs = strategy.resolvePojoFrom(input, schema, number);

                return hs.genericElementSchema.readFrom(input, owner);
            }
            case RuntimeFieldFactory.ID_THROWABLE:
                return PolymorphicThrowableSchema.readObjectFrom(input, schema, owner,
                        strategy, number);
            case RuntimeFieldFactory.ID_POJO: {
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
            default:
                throw new ProtostuffException("Corrupt input.  Unknown field number: " + number);
        }

        if (input instanceof GraphInput) {
            // update the actual reference.
            ((GraphInput) input).updateLast(value, owner);
        }

        if (input.readFieldNumber(schema) != 0)
            throw new ProtostuffException("Corrupt input.");

        return value;
    }

    @SuppressWarnings("unchecked")
    static void writeObjectTo(Output output, Object value,
                              Schema<?> currentSchema, IdStrategy strategy) throws IOException {
        final Class<Object> clazz = (Class<Object>) value.getClass();

        final HasDelegate<Object> hd = strategy.tryWriteDelegateIdTo(output,
                RuntimeFieldFactory.ID_DELEGATE, clazz);

        if (hd != null) {
            hd.delegate.writeTo(output, 1, value, false);
            return;
        }

        final RuntimeFieldFactory<Object> inline = RuntimeFieldFactory.getInline(clazz);
        if (inline != null) {
            // scalar value
            inline.writeTo(output, inline.id, value, false);
            return;
        }

        if (Message.class.isAssignableFrom(clazz)) {
            final Schema<Object> schema = strategy.writeMessageIdTo(
                    output, RuntimeFieldFactory.ID_POJO, (Message<Object>) value);

            if (output instanceof StatefulOutput) {
                // update using the derived schema.
                ((StatefulOutput) output).updateLast(schema, currentSchema);
            }

            schema.writeTo(output, value);
            return;
        }

        HasSchema<Object> hs = strategy.tryWritePojoIdTo(output, RuntimeFieldFactory.ID_POJO, clazz, false);
        if (hs != null) {
            final Schema<Object> schema = hs.getSchema();

            if (output instanceof StatefulOutput) {
                // update using the derived schema.
                ((StatefulOutput) output).updateLast(schema, currentSchema);
            }

            schema.writeTo(output, value);
            return;
        }

        if (clazz.isEnum()) {
            EnumIO<?> eio = strategy.getEnumIO(clazz);
            strategy.writeEnumIdTo(output, RuntimeFieldFactory.ID_ENUM, clazz);
            eio.writeTo(output, ID_ENUM_VALUE, false, (Enum<?>) value);
            return;
        }

        if (clazz.getSuperclass() != null && clazz.getSuperclass().isEnum()) {
            EnumIO<?> eio = strategy.getEnumIO(clazz.getSuperclass());
            strategy.writeEnumIdTo(output, RuntimeFieldFactory.ID_ENUM, clazz.getSuperclass());
            eio.writeTo(output, ID_ENUM_VALUE, false, (Enum<?>) value);
            return;
        }

        if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();

            final HasDelegate<Object> hdArray = strategy.tryWriteDelegateIdTo(output,
                    RuntimeFieldFactory.ID_ARRAY_DELEGATE, (Class<Object>) componentType);

            if (hdArray != null) {
                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(hdArray.genericElementSchema,
                            currentSchema);
                }

                hdArray.genericElementSchema.writeTo(output, value);
                return;
            }

            final RuntimeFieldFactory<?> inlineArray = RuntimeFieldFactory.getInline(
                    componentType);
            if (inlineArray != null) {
                // scalar
                final boolean primitive = componentType.isPrimitive();
                final ArraySchemas.Base arraySchema = ArraySchemas.getSchema(
                        inlineArray.id, primitive, strategy);

                output.writeUInt32(RuntimeFieldFactory.ID_ARRAY_SCALAR,
                        ArraySchemas.toArrayId(inlineArray.id, primitive),
                        false);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(arraySchema, currentSchema);
                }

                arraySchema.writeTo(output, value);
                return;
            }

            if (componentType.isEnum()) {
                // enum
                final EnumIO<?> eio = strategy.getEnumIO(componentType);

                strategy.writeEnumIdTo(output, RuntimeFieldFactory.ID_ARRAY_ENUM, componentType);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(eio.genericElementSchema,
                            currentSchema);
                }

                eio.genericElementSchema.writeTo(output, value);
                return;
            }

            if (Message.class.isAssignableFrom(componentType) ||
                    strategy.isRegistered(componentType)) {
                // messsage / registered pojo
                hs = strategy.writePojoIdTo(output,
                        RuntimeFieldFactory.ID_ARRAY_POJO, (Class<Object>) componentType);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(hs.genericElementSchema,
                            currentSchema);
                }

                hs.genericElementSchema.writeTo(output, value);
                return;
            }

            /*
             * if(!Throwable.class.isAssignableFrom(componentType)) { boolean loadService =
             * Message.class.isAssignableFrom(componentType); HasSchema<Object> hs = strategy.getSchemaWrapper(
             * (Class<Object>)componentType, loadService); if(hs != null) {
             *
             * } }
             */

            // complex type
            int dimensions = 1;
            while (componentType.isArray()) {
                dimensions++;
                componentType = componentType.getComponentType();
            }

            strategy.writeArrayIdTo(output, componentType);
            // send the length valueOf the array
            output.writeUInt32(ID_ARRAY_LEN, Array.getLength(value), false);
            // send the dimensions valueOf the array
            output.writeUInt32(ID_ARRAY_DIMENSION, dimensions, false);

            if (output instanceof StatefulOutput) {
                // update using the derived schema.
                ((StatefulOutput) output).updateLast(strategy.ARRAY_SCHEMA, currentSchema);
            }

            strategy.ARRAY_SCHEMA.writeTo(output, value);
            return;
        }

        if (Object.class == clazz) {
            output.writeUInt32(RuntimeFieldFactory.ID_OBJECT, 0, false);
            return;
        }

        if (Class.class == value.getClass()) {
            // its a class
            final Class<?> c = ((Class<?>) value);
            if (c.isArray()) {
                int dimensions = 1;
                Class<?> componentType = c.getComponentType();
                while (componentType.isArray()) {
                    dimensions++;
                    componentType = componentType.getComponentType();
                }

                strategy.writeClassIdTo(output, componentType, true);
                // send the dimensions valueOf the array
                output.writeUInt32(ID_ARRAY_DIMENSION, dimensions, false);
                return;
            }

            strategy.writeClassIdTo(output, c, false);
            return;
        }

        if (Throwable.class.isAssignableFrom(clazz)) {
            // throwable
            PolymorphicThrowableSchema.writeObjectTo(output, value, currentSchema,
                    strategy);
            return;
        }

        if (strategy.isRegistered(clazz)) {
            // pojo
            final Schema<Object> schema = strategy.writePojoIdTo(
                    output, RuntimeFieldFactory.ID_POJO, clazz).getSchema();

            if (output instanceof StatefulOutput) {
                // update using the derived schema.
                ((StatefulOutput) output).updateLast(schema, currentSchema);
            }

            schema.writeTo(output, value);
            return;
        }

        if (Map.class.isAssignableFrom(clazz)) {
            if (Collections.class == clazz.getDeclaringClass()) {
                output.writeUInt32(RuntimeFieldFactory.ID_POLYMORPHIC_MAP, 0, false);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(
                            strategy.POLYMORPHIC_MAP_SCHEMA, currentSchema);
                }

                PolymorphicMapSchema.writeNonPublicMapTo(output, value,
                        strategy.POLYMORPHIC_MAP_SCHEMA, strategy);
                return;
            }

            if (EnumMap.class.isAssignableFrom(clazz)) {
                strategy.writeEnumIdTo(output, RuntimeFieldFactory.ID_ENUM_MAP,
                        EnumIO.getKeyTypeFromEnumMap(value));
            } else {
                strategy.writeMapIdTo(output, RuntimeFieldFactory.ID_MAP, clazz);
            }

            if (output instanceof StatefulOutput) {
                // update using the derived schema.
                ((StatefulOutput) output).updateLast(strategy.MAP_SCHEMA, currentSchema);
            }

            strategy.MAP_SCHEMA.writeTo(output, (Map<Object, Object>) value);
            return;
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            if (Collections.class == clazz.getDeclaringClass()) {
                output.writeUInt32(RuntimeFieldFactory.ID_POLYMORPHIC_COLLECTION, 0, false);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(
                            strategy.POLYMORPHIC_COLLECTION_SCHEMA, currentSchema);
                }

                PolymorphicCollectionSchema.writeNonPublicCollectionTo(output, value,
                        strategy.POLYMORPHIC_COLLECTION_SCHEMA, strategy);
                return;
            }

            if (EnumSet.class.isAssignableFrom(clazz)) {
                strategy.writeEnumIdTo(output, RuntimeFieldFactory.ID_ENUM_SET,
                        EnumIO.getElementTypeFromEnumSet(value));
            } else {
                strategy.writeCollectionIdTo(output, RuntimeFieldFactory.ID_COLLECTION, clazz);
            }

            if (output instanceof StatefulOutput) {
                // update using the derived schema.
                ((StatefulOutput) output).updateLast(strategy.COLLECTION_SCHEMA, currentSchema);
            }

            strategy.COLLECTION_SCHEMA.writeTo(output, (Collection<Object>) value);
            return;
        }

        // pojo
        final Schema<Object> schema = strategy.writePojoIdTo(
                output, RuntimeFieldFactory.ID_POJO, clazz).getSchema();

        if (output instanceof StatefulOutput) {
            // update using the derived schema.
            ((StatefulOutput) output).updateLast(schema, currentSchema);
        }

        schema.writeTo(output, value);
    }

    static void transferObject(Pipe.Schema<Object> pipeSchema, Pipe pipe,
                               Input input, Output output, IdStrategy strategy) throws IOException {
        final int number = input.readFieldNumber(pipeSchema.wrappedSchema);
        switch (number) {
            case RuntimeFieldFactory.ID_BOOL:
                RuntimeFieldFactory.BOOL.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_BYTE:
                RuntimeFieldFactory.BYTE.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_CHAR:
                RuntimeFieldFactory.CHAR.transfer(pipe, input, output, number, false);
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
            case RuntimeFieldFactory.ID_STRING:
                RuntimeFieldFactory.STRING.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_BYTES:
                RuntimeFieldFactory.BYTES.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_BYTE_ARRAY:
                RuntimeFieldFactory.BYTE_ARRAY.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_BIGDECIMAL:
                RuntimeFieldFactory.BIGDECIMAL.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_BIGINTEGER:
                RuntimeFieldFactory.BIGINTEGER.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_DATE:
                RuntimeFieldFactory.DATE.transfer(pipe, input, output, number, false);
                break;
            case RuntimeFieldFactory.ID_ARRAY:
                transferArray(pipe, input, output, number, pipeSchema, false, strategy);
                return;
            case RuntimeFieldFactory.ID_OBJECT:
                output.writeUInt32(number, input.readUInt32(), false);
                break;
            case RuntimeFieldFactory.ID_ARRAY_MAPPED:
                transferArray(pipe, input, output, number, pipeSchema, true, strategy);
                return;
            case RuntimeFieldFactory.ID_CLASS:
                transferClass(pipe, input, output, number, pipeSchema, false, false, strategy);
                break;
            case RuntimeFieldFactory.ID_CLASS_MAPPED:
                transferClass(pipe, input, output, number, pipeSchema, true, false, strategy);
                break;
            case RuntimeFieldFactory.ID_CLASS_ARRAY:
                transferClass(pipe, input, output, number, pipeSchema, false, true, strategy);
                break;
            case RuntimeFieldFactory.ID_CLASS_ARRAY_MAPPED:
                transferClass(pipe, input, output, number, pipeSchema, true, true, strategy);
                break;

            case RuntimeFieldFactory.ID_ENUM: {
                strategy.transferEnumId(input, output, number);

                if (input.readFieldNumber(pipeSchema.wrappedSchema) != ID_ENUM_VALUE)
                    throw new ProtostuffException("Corrupt input.");
                EnumIO.transfer(pipe, input, output, 1, false, strategy);
                break;
            }

            case RuntimeFieldFactory.ID_ENUM_SET: {
                strategy.transferEnumId(input, output, number);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(strategy.COLLECTION_PIPE_SCHEMA, pipeSchema);
                }

                Pipe.transferDirect(strategy.COLLECTION_PIPE_SCHEMA, pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_ENUM_MAP: {
                strategy.transferEnumId(input, output, number);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(strategy.MAP_PIPE_SCHEMA, pipeSchema);
                }

                Pipe.transferDirect(strategy.MAP_PIPE_SCHEMA, pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_COLLECTION: {
                strategy.transferCollectionId(input, output, number);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(strategy.COLLECTION_PIPE_SCHEMA, pipeSchema);
                }

                Pipe.transferDirect(strategy.COLLECTION_PIPE_SCHEMA, pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_MAP: {
                strategy.transferMapId(input, output, number);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(strategy.MAP_PIPE_SCHEMA, pipeSchema);
                }

                Pipe.transferDirect(strategy.MAP_PIPE_SCHEMA, pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_POLYMORPHIC_COLLECTION: {
                if (0 != input.readUInt32())
                    throw new ProtostuffException("Corrupt input.");
                output.writeUInt32(number, 0, false);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(
                            strategy.POLYMORPHIC_COLLECTION_PIPE_SCHEMA, pipeSchema);
                }

                Pipe.transferDirect(strategy.POLYMORPHIC_COLLECTION_PIPE_SCHEMA,
                        pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_POLYMORPHIC_MAP: {
                if (0 != input.readUInt32())
                    throw new ProtostuffException("Corrupt input.");
                output.writeUInt32(number, 0, false);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(
                            strategy.POLYMORPHIC_MAP_PIPE_SCHEMA, pipeSchema);
                }

                Pipe.transferDirect(strategy.POLYMORPHIC_MAP_PIPE_SCHEMA,
                        pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_DELEGATE: {
                final HasDelegate<Object> hd = strategy.transferDelegateId(input,
                        output, number);
                if (1 != input.readFieldNumber(pipeSchema.wrappedSchema))
                    throw new ProtostuffException("Corrupt input.");

                hd.delegate.transfer(pipe, input, output, 1, false);
                break;
            }

            case RuntimeFieldFactory.ID_ARRAY_DELEGATE: {
                final HasDelegate<Object> hd = strategy.transferDelegateId(input,
                        output, number);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(
                            hd.genericElementSchema.getPipeSchema(),
                            pipeSchema);
                }

                Pipe.transferDirect(hd.genericElementSchema.getPipeSchema(),
                        pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_ARRAY_SCALAR: {
                final int arrayId = input.readUInt32(), id = ArraySchemas.toInlineId(arrayId);

                final ArraySchemas.Base arraySchema = ArraySchemas.getSchema(id,
                        ArraySchemas.isPrimitive(arrayId), strategy);

                output.writeUInt32(number, arrayId, false);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(arraySchema.getPipeSchema(),
                            pipeSchema);
                }

                Pipe.transferDirect(arraySchema.getPipeSchema(), pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_ARRAY_ENUM: {
                final EnumIO<?> eio = strategy.resolveEnumFrom(input);

                strategy.writeEnumIdTo(output, number, eio.enumClass);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(
                            eio.genericElementSchema.getPipeSchema(),
                            pipeSchema);
                }

                Pipe.transferDirect(eio.genericElementSchema.getPipeSchema(),
                        pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_ARRAY_POJO: {
                final HasSchema<Object> hs = strategy.transferPojoId(input, output,
                        number);

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(
                            hs.genericElementSchema.getPipeSchema(),
                            pipeSchema);
                }

                Pipe.transferDirect(hs.genericElementSchema.getPipeSchema(),
                        pipe, input, output);
                return;
            }

            case RuntimeFieldFactory.ID_THROWABLE:
                PolymorphicThrowableSchema.transferObject(pipeSchema, pipe, input,
                        output, strategy, number);
                return;

            case RuntimeFieldFactory.ID_POJO:
                final Pipe.Schema<Object> derivedPipeSchema = strategy.transferPojoId(
                        input, output, number).getPipeSchema();

                if (output instanceof StatefulOutput) {
                    // update using the derived schema.
                    ((StatefulOutput) output).updateLast(derivedPipeSchema, pipeSchema);
                }

                Pipe.transferDirect(derivedPipeSchema, pipe, input, output);
                return;
            default:
                throw new ProtostuffException("Corrupt input.  Unknown field number: " + number);
        }

        if (input.readFieldNumber(pipeSchema.wrappedSchema) != 0)
            throw new ProtostuffException("Corrupt input.");
    }

    /**
     * An array wrapper internally used for adding objects.
     */
    static final class ArrayWrapper implements Collection<Object> {
        final Object array;
        int offset = 0;

        ArrayWrapper(Object array) {
            this.array = array;
        }

        @Override
        public boolean add(Object value) {
            Array.set(array, offset++, value);
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Object> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Object> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] arg0) {
            throw new UnsupportedOperationException();
        }
    }

}
