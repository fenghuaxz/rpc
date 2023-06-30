//========================================================================
//Copyright 2016 David Yu
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

/**
 * Read/send from/to fields using sun.misc.Unsafe
 *
 * @author David Yu
 * @created Oct 11, 2016
 */
public final class UnsafeAccessor extends Accessor {
    static final Factory FACTORY = new Factory() {
        public Accessor create(java.lang.reflect.Field f) {
            return new UnsafeAccessor(f);
        }
    };

    public final long offset;

    public UnsafeAccessor(java.lang.reflect.Field f) {
        super(f);
        offset = RuntimeUnsafeFieldFactory.us.objectFieldOffset(f);
    }

    @Override
    public void set(Object owner, Object value) {
        RuntimeUnsafeFieldFactory.us.putObject(owner, offset, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object owner) {
        return (T) RuntimeUnsafeFieldFactory.us.getObject(owner, offset);
    }

}