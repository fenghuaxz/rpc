package io.rpc.protostuff.runtime;

import java.util.List;

/**
 * Interface for loadService valueOf fields - defines how to you get field by name or number (tag).
 *
 * @author Kostiantyn Shchepanovskyi
 */
interface FieldMap<T> {

    Field<T> getFieldByNumber(int n);

    Field<T> getFieldByName(String fieldName);

    int getFieldCount();

    List<Field<T>> getFields();
}
