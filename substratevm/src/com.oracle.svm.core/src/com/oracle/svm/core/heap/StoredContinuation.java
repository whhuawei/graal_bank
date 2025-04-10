/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.heap;

import org.graalvm.nativeimage.c.function.CodePointer;

import com.oracle.svm.core.hub.Hybrid;

import jdk.graal.compiler.word.Word;

/**
 * Persisted execution state of a yielded continuation, use via {@link StoredContinuationAccess}.
 *
 * Stored continuations are {@link Hybrid} objects where the array part contains the raw stack data.
 * After writing the stack data into the object, we manually emit the correct GC write barriers for
 * all the references (see {@link Heap#dirtyAllReferencesOf}).
 */
@Hybrid(componentType = Word.class)
public final class StoredContinuation {
    CodePointer ip;

    /** Must be allocated via {@link StoredContinuationAccess}. */
    private StoredContinuation() {
    }
}
