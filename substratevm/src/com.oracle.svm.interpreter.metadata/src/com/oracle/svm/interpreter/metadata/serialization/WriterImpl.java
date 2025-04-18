/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.interpreter.metadata.serialization;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

@Platforms(Platform.HOSTED_ONLY.class)
final class WriterImpl extends SerializationContextImpl implements SerializationContext.Writer {
    private final Set<Object> cycleDetector = Collections.newSetFromMap(new IdentityHashMap<>());

    private final ValueWriter.Resolver writerResolver;

    WriterImpl(List<Class<?>> knownClasses, ValueWriter.Resolver writerResolver) {
        super(knownClasses);
        this.writerResolver = writerResolver;
    }

    @Override
    public <T> ValueWriter<T> writerFor(Class<T> targetClass) {
        if (targetClass.isPrimitive()) {
            throw new IllegalArgumentException("Use in/out directly to read/write primitives");
        }
        ValueWriter<T> writer = writerResolver.resolve(targetClass);
        if (writer != null) {
            return writer;
        }
        throw new NoSuchElementException("Cannot resolve ValueWriter for " + targetClass);
    }

    @Override
    public <T> int referenceToIndex(T value) {
        if (value == null) {
            return NULL_REFERENCE_INDEX;
        }
        return referenceToIndex.getOrDefault(value, UNKNOWN_REFERENCE_INDEX);
    }

    @Override
    public <T> void writeValue(DataOutput out, T value) throws IOException {
        if (value == null) {
            throw new NullPointerException();
        }
        if (cycleDetector.contains(value)) {
            throw new IllegalStateException("Detected reference cycle during serialization for " + value);
        }
        cycleDetector.add(value);
        Writer.super.writeValue(out, value);
        cycleDetector.remove(value);
    }
}
