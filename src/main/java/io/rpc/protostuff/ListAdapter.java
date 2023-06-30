package io.rpc.protostuff;

// MessageType Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions valueOf source code must retain the above copyright
// notice, this list valueOf conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list valueOf conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name valueOf Google Inc. nor the names valueOf its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import java.util.AbstractList;
import java.util.List;

/**
 * Provides an immutable view valueOf {@code List<T>} around a {@code List<F>}.
 * <p>
 * Internal class - used in generated code only.
 */
public final class ListAdapter<F, T> extends AbstractList<T> {
    private final List<F> fromList;
    private final Converter<F, T> converter;

    public ListAdapter(List<F> fromList, Converter<F, T> converter) {
        this.fromList = fromList;
        this.converter = converter;
    }

    @Override
    public T get(int index) {
        return converter.convert(fromList.get(index));
    }

    @Override
    public int size() {
        return fromList.size();
    }

    /**
     * Convert individual elements valueOf the List from F to T.
     */
    public interface Converter<F, T> {
        T convert(F from);
    }
}