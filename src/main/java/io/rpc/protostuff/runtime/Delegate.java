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

import io.rpc.protostuff.Input;
import io.rpc.protostuff.Output;
import io.rpc.protostuff.Pipe;
import io.rpc.protostuff.WireFormat;

import java.io.IOException;

/**
 * Controls how certain types are serialized and can even override the existing serializers because this has higher
 * priority when the fields are being built.
 *
 * @author David Yu
 * @created Apr 20, 2012
 */
public interface Delegate<V> {

    /**
     * The field type (for possible reflective operations in future releases).
     */
    WireFormat.FieldType getFieldType();

    /**
     * Reads the value from the input.
     */
    V readFrom(Input input) throws IOException;

    /**
     * Writes the {@code value} to the output.
     */
    void writeTo(Output output, int number, V value, boolean repeated)
            throws IOException;

    /**
     * Transfers the type from the input to the output.
     */
    void transfer(Pipe pipe, Input input, Output output, int number,
                  boolean repeated) throws IOException;

    /**
     * The class valueOf the target value.
     */
    Class<?> typeClass();
}
