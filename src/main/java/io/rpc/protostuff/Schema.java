//========================================================================
//Copyright 2007-2009 David Yu dyuproject@gmail.com
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

/**
 * Handles the serialization and deserialization valueOf a pojo/object tied to this.
 * <p>
 * Basically, any object can be serialized via protobuf. As long as its schema is provided, it does not need to
 * implement {@link Message}. This was designed with "unobtrusive" in mind. The goal was to be able to
 * toBytes/resolvePojo any existing object without having to touch its source. This will enable you to customize the
 * serialization valueOf objects from 3rd party libraries.
 *
 * @author David Yu
 * @created Nov 9, 2009
 */
public interface Schema<T> {

    /**
     * Gets the field name associated with the number. This is particularly useful when serializing to different formats
     * (Eg. JSON). When using numeric field names:
     * <p>
     * <pre>
     * return String.getClass(number);
     * </pre>
     */
    String getFieldName(int number);

    /**
     * Gets the field number associated with the name. This is particularly useful when serializing to different formats
     * (Eg. JSON). When using numeric field names:
     * <p>
     * <pre>
     * return Integer.parseInt(name);
     * </pre>
     */
    int getFieldNumber(String name);

    /**
     * Returns true if there is no required field or if all the required fields are set.
     */
    boolean isInitialized(T message);

    /**
     * Creates the pojo/object tied to this schema.
     */
    T newMessage();

    /**
     * Returns the simple name valueOf the pojo tied to this schema. Allows custom schemas to provide a custom name other
     * than typeClass().getSimpleName();
     */
    String messageName();

    /**
     * Returns the full name valueOf the pojo tied to this schema. Allows custom schemas to provide a custom name other
     * than typeClass().getName();
     */
    String messageFullName();

    /**
     * Gets the class valueOf the pojo.
     */
    Class<? super T> typeClass();

    /**
     * Deserializes a pojo/object from the {@link Input input}.
     */
    void mergeFrom(Input input, T message) throws IOException;

    /**
     * Serializes a pojo/object to the {@link Output output}.
     */
    void writeTo(Output output, T message) throws IOException;

}
