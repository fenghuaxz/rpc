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

import io.rpc.protostuff.Pipe;
import io.rpc.protostuff.Schema;

/**
 * Used when the type is either polymorphic or too complex. Unlike DerivativeSchema, this is designed to have no concept
 * valueOf merging.
 *
 * @author David Yu
 * @created Apr 30, 2012
 */
public abstract class PolymorphicSchema implements Schema<Object> {

    /**
     * The handler who's job is to set the value to the owner.
     */
    public interface Handler {
        void setValue(Object value, Object owner);
    }

    /**
     * A factory which creates a schema with the handler connected to it.
     */
    public interface Factory {
        PolymorphicSchema newSchema(Class<?> typeClass,
                                    IdStrategy strategy, Handler handler);
    }

    public final IdStrategy strategy;

    public PolymorphicSchema(IdStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public boolean isInitialized(Object message) {
        return true;
    }

    @Override
    public Object newMessage() {
        // cannot instantiate because the type is dynamic.
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<? super Object> typeClass() {
        return Object.class;
    }

    /**
     * The pipe schema associated with this schema.
     */
    public abstract Pipe.Schema<Object> getPipeSchema();

    /**
     * Set the value to the owner.
     */
    protected abstract void setValue(Object value, Object owner);
}
