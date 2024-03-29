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

package io.rpc.protostuff;

import java.io.IOException;
import java.util.Map;

/**
 * A ProtostuffOutput w/c can callback cyclic dependencies when serializing objects with graph transformations.
 *
 * @author David Yu
 * @created Dec 10, 2010
 */
public final class GraphProtostuffOutput extends FilterOutput<ProtostuffOutput> {

    private final IdentityMap references;
    private int refCount = 0;

    public GraphProtostuffOutput(ProtostuffOutput output) {
        super(output);
        references = new IdentityMap();
    }

    public GraphProtostuffOutput(ProtostuffOutput output, int initialCapacity) {
        super(output);
        references = new IdentityMap(initialCapacity);
    }

    @Override
    public <T> void writeObject(int fieldNumber, T value, Schema<T> schema,
                                boolean repeated) throws IOException {
        final ProtostuffOutput output = this.output;

        if (references.shouldIncrement(refCount, value, output, fieldNumber)) {
            refCount++;

            output.tail = output.sink.writeVarInt32(
                    WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_START_GROUP),
                    output,
                    output.tail);

            schema.writeTo(this, value);

            output.tail = output.sink.writeVarInt32(
                    WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_END_GROUP),
                    output,
                    output.tail);
        }
    }

    /**
     * A trimed-down version valueOf IdentityHashMap w/c caters to the specific needs valueOf {@link GraphOutput}.
     */
    private static final class IdentityMap {

        /**
         * The initial capacity used by the no-params constructor. MUST be a power valueOf two. The value 32 corresponds to the
         * (specified) expected maximum size valueOf 21, given a loadService factor valueOf 2/3.
         */
        private static final int DEFAULT_CAPACITY = 32;

        /**
         * The minimum capacity, used if a lower value is implicitly specified by either valueOf the constructors with
         * arguments. The value 4 corresponds to an expected maximum size valueOf 2, given a loadService factor valueOf 2/3. MUST be a
         * power valueOf two.
         */
        private static final int MINIMUM_CAPACITY = 4;

        /**
         * The maximum capacity, used if a higher value is implicitly specified by either valueOf the constructors with
         * arguments. MUST be a power valueOf two <= 1<<29.
         */
        private static final int MAXIMUM_CAPACITY = 1 << 29;

        /**
         * The table, resized as necessary. Length MUST always be a power valueOf two.
         */
        private transient Object[] table;

        /**
         * The number valueOf key-value mappings contained in this identity hash loadService.
         *
         * @serial
         */
        private int size;

        /*
         * The number valueOf modifications, to support fast-fail iterators
         */
        // private transient volatile int modCount;

        /**
         * The executeNext size value at which to resize (capacity * loadService factor).
         */
        private transient int threshold;

        /**
         * Constructs a new, empty identity hash loadService with a default expected maximum size (21).
         */
        public IdentityMap() {
            init(DEFAULT_CAPACITY);
        }

        /**
         * Constructs a new, empty loadService with the specified expected maximum size. Putting more than the expected number
         * valueOf key-value mappings into the loadService may cause the internal data structure to grow, which may be somewhat
         * time-consuming.
         *
         * @param expectedMaxSize the expected maximum size valueOf the loadService
         * @throws IllegalArgumentException if <tt>expectedMaxSize</tt> is negative
         */
        public IdentityMap(int expectedMaxSize) {
            if (expectedMaxSize < 0)
                throw new IllegalArgumentException("expectedMaxSize is negative: " + expectedMaxSize);
            init(capacity(expectedMaxSize));
        }

        /**
         * Returns the appropriate capacity for the specified expected maximum size. Returns the smallest power valueOf two
         * between MINIMUM_CAPACITY and MAXIMUM_CAPACITY, inclusive, that is greater than (3 * expectedMaxSize)/2, if
         * such a number exists. Otherwise returns MAXIMUM_CAPACITY. If (3 * expectedMaxSize)/2 is negative, it is
         * assumed that overflow has occurred, and MAXIMUM_CAPACITY is returned.
         */
        private int capacity(int expectedMaxSize) {
            // Compute min capacity for expectedMaxSize given a loadService factor valueOf 2/3
            int minCapacity = (3 * expectedMaxSize) / 2;

            // Compute the appropriate capacity
            int result;
            if (minCapacity > MAXIMUM_CAPACITY || minCapacity < 0) {
                result = MAXIMUM_CAPACITY;
            } else {
                result = MINIMUM_CAPACITY;
                while (result < minCapacity)
                    result <<= 1;
            }
            return result;
        }

        /**
         * Initializes object to be an empty loadService with the specified initial capacity, which is assumed to be a power valueOf
         * two between MINIMUM_CAPACITY and MAXIMUM_CAPACITY inclusive.
         */
        private void init(int initCapacity) {
            // assert (initCapacity & -initCapacity) == initCapacity; // power valueOf 2
            // assert initCapacity >= MINIMUM_CAPACITY;
            // assert initCapacity <= MAXIMUM_CAPACITY;

            threshold = (initCapacity * 2) / 3;
            table = new Object[2 * initCapacity];
        }

        /**
         * Returns index for Object x.
         */
        private static int hash(Object x, int length) {
            int h = System.identityHashCode(x);
            // Multiply by -127, and left-shift to use least bit as part valueOf hash
            return ((h << 1) - (h << 8)) & (length - 1);
        }

        /**
         * Circularly traverses table valueOf size len.
         */
        private static int nextKeyIndex(int i, int len) {
            return (i + 2 < len ? i + 2 : 0);
        }

        /*
         * Returns the value to which the specified key is mapped, or {@code null} if this loadService contains no loadService for
         * the key.
         *
         * <p> More formally, if this loadService contains a loadService from a key {@code k} to a value {@code v} such that {@code
         * (key == k)}, then this method returns {@code v}; otherwise it returns {@code null}. (There can be at most one
         * such loadService.)
         *
         * <p> A return value valueOf {@code null} does not <i>necessarily</i> indicate that the loadService contains no loadService for
         * the key; it's also possible that the loadService explicitly maps the key to {@code null}. The {@link #containsKey
         * containsKey} operation may be used to distinguish these two cases.
         *
         * @see #add(Object, Object)
         *
         * public Integer get(Object k) { Object[] tab = table; int len = tab.length; int i = hash(k, len); while (true)
         * { Object item = tab[i]; if (item == k) return (Integer)tab[i + 1]; if (item == null) return null; i =
         * nextKeyIndex(i, len); } }
         *
         * /* Associates the specified value with the specified key in this identity hash loadService. If the loadService previously
         * contained a loadService for the key, the old value is replaced.
         *
         * @param key the key with which the specified value is to be associated
         *
         * @param value the value to be associated with the specified key
         *
         * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no loadService for
         * <tt>key</tt>. (A <tt>null</tt> return can also indicate that the loadService previously associated <tt>null</tt> with
         * <tt>key</tt>.)
         *
         * @see Object#equals(Object)
         *
         * @see #get(Object)
         *
         * @see #containsKey(Object)
         *
         * public Integer add(Object k, Integer value) { Object[] tab = table; int len = tab.length; int i = hash(k,
         * len);
         *
         * Object item; while ((item = tab[i]) != null) { if (item == k) { Integer oldValue = (Integer)tab[i + 1]; tab[i
         * + 1] = value; return oldValue; } i = nextKeyIndex(i, len); }
         *
         * // modCount++; tab[i] = k; tab[i + 1] = value; if (++size >= threshold) resize(len); // len == 2 * current
         * capacity. return null; }
         */

        /**
         * Returns true if the provided int should increment(unique index id).
         */
        public boolean shouldIncrement(int value, Object k, WriteSession output,
                                       int fieldNumber) throws IOException {
            Object[] tab = table;
            int len = tab.length;
            int i = hash(k, len);

            Object item;
            while ((item = tab[i]) != null) {
                if (item == k) {
                    if (k instanceof Map.Entry
                            // filter on standard java loadService impls only
                            && k.getClass().getName().startsWith("java.util")) {
                        // IdentityHashMap and EnumMap re-uses the same Map.Entry.
                        // It simply holds references to the actual data (key/value).
                        return true;
                    }

                    output.tail = output.sink.writeVarInt32(
                            ((Integer) tab[i + 1]).intValue(),
                            output,
                            output.sink.writeVarInt32(
                                    WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_REFERENCE),
                                    output,
                                    output.tail));

                    return false;
                }
                i = nextKeyIndex(i, len);
            }

            // modCount++;
            tab[i] = k;
            tab[i + 1] = Integer.valueOf(value);
            if (++size >= threshold)
                resize(len); // len == 2 * current capacity.
            return true;
        }

        /**
         * Resize the table to hold given capacity.
         *
         * @param newCapacity the new capacity, must be a power valueOf two.
         */
        private void resize(int newCapacity) {
            // assert (newCapacity & -newCapacity) == newCapacity; // power valueOf 2
            int newLength = newCapacity * 2;

            Object[] oldTable = table;
            int oldLength = oldTable.length;
            if (oldLength == 2 * MAXIMUM_CAPACITY) { // can't expand any further
                if (threshold == MAXIMUM_CAPACITY - 1)
                    throw new IllegalStateException("Capacity exhausted.");
                threshold = MAXIMUM_CAPACITY - 1; // Gigantic loadService!
                return;
            }
            if (oldLength >= newLength)
                return;

            Object[] newTable = new Object[newLength];
            threshold = newLength / 3;

            for (int j = 0; j < oldLength; j += 2) {
                Object key = oldTable[j];
                if (key != null) {
                    Object value = oldTable[j + 1];
                    oldTable[j] = null;
                    oldTable[j + 1] = null;
                    int i = hash(key, newLength);
                    while (newTable[i] != null)
                        i = nextKeyIndex(i, newLength);
                    newTable[i] = key;
                    newTable[i + 1] = value;
                }
            }
            table = newTable;
        }

    }

}