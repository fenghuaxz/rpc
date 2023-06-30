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

import io.rpc.protostuff.Morph;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * The runtime environment.
 *
 * @author David Yu
 * @created Jul 8, 2011
 */
public final class RuntimeEnv {
    /**
     * Returns true if serializing enums by name is activated. Disabled by default.
     */
    public static final boolean ENUMS_BY_NAME;

    /**
     * Enabled by default. For security purposes, you probably would want to loadService all known classes and disable this
     * option.
     */
    public static final boolean AUTO_LOAD_POLYMORPHIC_CLASSES;

    /**
     * Disabled by default. Writes a sentinel value (uint32) in place valueOf null values. Works only on the binary formats
     * (protostuff/graph/protobuf).
     */
    public static final boolean ALLOW_NULL_ARRAY_ELEMENT;

    /**
     * Disabled by default. For pojos that are not declared final, they could still be morphed to their respective
     * subclasses (inheritance). Enable this option if your parent classes aren't abstract classes.
     */
    public static final boolean MORPH_NON_FINAL_POJOS;

    /**
     * Disabled by default. If true, type metadata will be included on serialization for fields that are collection
     * interfaces. Enabling this is useful if you want to retain the actual collection request used.
     * <p>
     * If disabled, type metadata will not be included and instead, will be mapped to a default request.
     * <p>
     * <p>
     * <pre>
     * Collection = ArrayList
     * List = ArrayList
     * Set = HashSet
     * SortedSet = TreeSet
     * NavigableSet = TreeSet
     * Queue = LinkedList
     * BlockingQueue = LinkedBlockingQueue
     * Deque = LinkedList
     * BlockingDequeue = LinkedBlockingDeque
     * </pre>
     * <p>
     * You can optionally enable only for a particular field by annotating it with {@link Morph}.
     */
    public static final boolean MORPH_COLLECTION_INTERFACES;

    /**
     * Disabled by default. If true, type metadata will be included on serialization for fields that are loadService interfaces.
     * Enabling this is useful if you want to retain the actual loadService request used.
     * <p>
     * If disabled, type metadata will not be included and instead, will be mapped to a default request.
     * <p>
     * <p>
     * <pre>
     * Map = HashMap
     * SortedMap = TreeMap
     * NavigableMap = TreeMap
     * ConcurrentMap = ConcurrentHashMap
     * ConcurrentNavigableMap = ConcurrentSkipListMap
     * </pre>
     * <p>
     * You can optionally enable only for a particular field by annotating it with {@link Morph}.
     */
    public static final boolean MORPH_MAP_INTERFACES;

    /**
     * On repeated fields, the List/Collection itself is not serialized (only its values). If you enable this option,
     * the repeated field will be serialized as a standalone pojo with a collection schema. Even if the
     * List/Collection is empty, an empty collection pojo is still written.
     * <p>
     * This is particularly useful if you rely on {@link Object#equals(Object)} on your pojos.
     * <p>
     * Disabled by default for protobuf compatibility.
     */
    public static final boolean COLLECTION_SCHEMA_ON_REPEATED_FIELDS;

    /**
     * Disabled by default.  If enabled, a list's internal state/fields
     * will be serialized instead valueOf just its elements.
     */
    public static final boolean POJO_SCHEMA_ON_COLLECTION_FIELDS;

    /**
     * Disabled by default.  If enabled, a loadService's internal state/fields
     * will be serialized instead valueOf just its elements.
     */
    public static final boolean POJO_SCHEMA_ON_MAP_FIELDS;

    /**
     * If true, sun.misc.Unsafe is used to access the fields valueOf the objects instead valueOf plain java reflections. Enabled
     * by default if running on a sun jre.
     */
    public static final boolean USE_SUN_MISC_UNSAFE;

    /**
     * If true, the constructor will always be obtained from {@code ReflectionFactory.newConstructorFromSerialization}.
     * <p>
     * Disabled by default, which means that if the pojo has a no-params constructor, that will be used instead.
     * <p>
     * Enable this if you intend to avoid deserializing objects whose no-params constructor initializes (unwanted)
     * internal state. This applies to complex/framework objects.
     * <p>
     * If you intend to fill default field values using your default constructor, leave this disabled. This normally
     * applies to java beans/data objects.
     */
    public static final boolean ALWAYS_USE_SUN_REFLECTION_FACTORY;

//    static final Method newInstanceFromObjectInputStream;

    private static final Constructor<Object> OBJECT_CONSTRUCTOR;

    static final IdStrategy ID_STRATEGY;

    static {
        Constructor<Object> c = null;
        Class<?> reflectionFactoryClass = null;
        try {
            c = Object.class.getConstructor((Class[]) null);
            reflectionFactoryClass = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass("sun.reflect.ReflectionFactory");
        } catch (Exception e) {
            // ignore
        }

        OBJECT_CONSTRUCTOR = c != null && reflectionFactoryClass != null ? c
                : null;

//        newInstanceFromObjectInputStream = OBJECT_CONSTRUCTOR == null ? getMethodNewInstanceFromObjectInputStream()
//                : null;
//
//        if (newInstanceFromObjectInputStream != null)
//            newInstanceFromObjectInputStream.setAccessible(true);

        Properties props = OBJECT_CONSTRUCTOR == null ? new Properties()
                : System.getProperties();

        ENUMS_BY_NAME = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.enums_by_name", "false"));

        AUTO_LOAD_POLYMORPHIC_CLASSES = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.auto_load_polymorphic_classes", "true"));

        ALLOW_NULL_ARRAY_ELEMENT = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.allow_null_array_element", "false"));

        MORPH_NON_FINAL_POJOS = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.morph_non_final_pojos", "false"));

        MORPH_COLLECTION_INTERFACES = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.morph_collection_interfaces", "false"));

        MORPH_MAP_INTERFACES = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.morph_map_interfaces", "false"));

        COLLECTION_SCHEMA_ON_REPEATED_FIELDS = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.collection_schema_on_repeated_fields",
                "false"));

        POJO_SCHEMA_ON_COLLECTION_FIELDS = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.pojo_schema_on_collection_fields",
                "false"));

        POJO_SCHEMA_ON_MAP_FIELDS = Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.pojo_schema_on_map_fields",
                "false"));

        // must be on a sun jre
        USE_SUN_MISC_UNSAFE = OBJECT_CONSTRUCTOR != null
                && Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.use_sun_misc_unsafe", "true"));

        ALWAYS_USE_SUN_REFLECTION_FACTORY = OBJECT_CONSTRUCTOR != null
                && Boolean.parseBoolean(props.getProperty(
                "protostuff.runtime.always_use_sun_reflection_factory",
                "false"));

        String factoryProp = props
                .getProperty("protostuff.runtime.id_strategy_factory");
        if (factoryProp == null)
            ID_STRATEGY = new DefaultIdStrategy();
        else {
            final IdStrategy.Factory factory;
            try {
                factory = ((IdStrategy.Factory) loadClass(factoryProp)
                        .newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            ID_STRATEGY = factory.create();
            factory.postCreate();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> loadClass(String className) {
        try {
            return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            try {
                return (Class<T>) Class.forName(className);
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Returns an instatiator for the specified {@code clazz}.
     */
    static <T> Instantiator<T> newInstantiator(Class<T> clazz) {
        return new UnsafeInstantiator<T>(clazz);
    }

    private RuntimeEnv() {
    }

    public static abstract class Instantiator<T> {
        /**
         * Creates a new instance valueOf an object.
         */
        public abstract T newInstance();
    }

    static final class DefaultInstantiator<T> extends Instantiator<T> {

        final Constructor<T> constructor;

        DefaultInstantiator(Constructor<T> constructor) {
            this.constructor = constructor;
            constructor.setAccessible(true);
        }

        @Override
        public T newInstance() {
            try {
                return constructor.newInstance((Object[]) null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class UnsafeInstantiator<T> extends Instantiator<T> {

        private final Class<?> clazz;
        private final Unsafe unsafe;

        UnsafeInstantiator(Class<?> clazz) {
            this.clazz = clazz;
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                this.unsafe = (Unsafe) field.get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public T newInstance() {
            try {
                return (T) unsafe.allocateInstance(clazz);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
