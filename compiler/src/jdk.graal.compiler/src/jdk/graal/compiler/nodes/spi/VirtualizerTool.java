/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.nodes.spi;

import java.util.List;

import jdk.graal.compiler.debug.DebugContext;
import jdk.graal.compiler.debug.GraalError;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.nodes.WithExceptionNode;
import jdk.graal.compiler.nodes.java.MonitorIdNode;
import jdk.graal.compiler.nodes.virtual.VirtualObjectNode;
import jdk.graal.compiler.options.OptionValues;
import jdk.vm.ci.meta.JavaKind;

/**
 * This tool can be used to query the current state (normal/virtualized/re-materialized) of values
 * and to describe the actions that would be taken for this state.
 *
 * See also {@link Virtualizable}.
 */
public interface VirtualizerTool extends CoreProviders {

    /**
     * This method should be used to query the maximum size of virtualized objects before attempting
     * virtualization.
     *
     * @return the maximum number of entries for virtualized objects.
     */
    int getMaximumEntryCount();

    // methods working on virtualized/materialized objects

    /**
     * Introduces a new virtual object to the current state.
     *
     * @param virtualObject the new virtual object.
     * @param entryState the initial state of the virtual object's fields.
     * @param locks the initial locking depths.
     * @param sourcePosition a source position for the new node or null if none is available
     * @param ensureVirtualized true if this object needs to stay virtual
     */
    void createVirtualObject(VirtualObjectNode virtualObject, ValueNode[] entryState, List<MonitorIdNode> locks, NodeSourcePosition sourcePosition, boolean ensureVirtualized);

    /**
     * Returns a VirtualObjectNode if the given value is aliased with a virtual object that is still
     * virtual, the materialized value of the given value is aliased with a virtual object that was
     * materialized, the replacement if the give value was replaced, otherwise the given value.
     *
     * Replacements via {@link #replaceWithValue(ValueNode)} are not immediately committed. This
     * method can be used to determine if a value was replaced by another one (e.g., a load field by
     * the loaded value).
     */
    ValueNode getAlias(ValueNode value);

    /**
     * Sets the entry (field or array element) with the given index in the virtualized object.
     *
     * @param index the index to be set.
     * @param value the new value for the given index.
     * @param accessKind the kind of the store which might be different than
     *            {@link VirtualObjectNode#entryKind}.
     * @return true if the operation was permitted
     */
    boolean setVirtualEntry(VirtualObjectNode virtualObject, int index, ValueNode value, JavaKind accessKind, long offset);

    default void setVirtualEntry(VirtualObjectNode virtualObject, int index, ValueNode value) {
        if (!setVirtualEntry(virtualObject, index, value, null, 0)) {
            throw new GraalError("unexpected failure when updating virtual entry");
        }
    }

    ValueNode getEntry(VirtualObjectNode virtualObject, int index);

    boolean canVirtualizeLock(VirtualObjectNode virtualObject, MonitorIdNode monitorId);

    void addLock(VirtualObjectNode virtualObject, MonitorIdNode monitorId);

    MonitorIdNode removeLock(VirtualObjectNode virtualObject);

    void setEnsureVirtualized(VirtualObjectNode virtualObject, boolean ensureVirtualized);

    // operations on the current node

    /**
     * Deletes the current node and replaces it with the given virtualized object. If the current
     * node is a {@link WithExceptionNode}, kills the exception edge.
     *
     * @param virtualObject the virtualized object that should replace the current node.
     */
    void replaceWithVirtual(VirtualObjectNode virtualObject);

    /**
     * Deletes the current node and replaces it with the given value.
     *
     * @param replacement the value that should replace the current node.
     */
    void replaceWithValue(ValueNode replacement);

    /**
     * Deletes the current node. If the current node is a {@link WithExceptionNode}, kills the
     * exception edge.
     */
    void delete();

    /**
     * Replaces an input of the current node.
     *
     * @param oldInput the old input value.
     * @param replacement the new input value.
     */
    void replaceFirstInput(Node oldInput, Node replacement);

    /**
     * Adds the given node to the graph. This action will only be performed when, and if, the
     * changes are committed. This should be used for nodes which have been explicitly created by
     * the caller. If it's unclear who might have created a node, use
     * {@link #ensureAdded(ValueNode)}.
     *
     * @param node the node to add.
     */
    void addNode(ValueNode node);

    /**
     * Adds the given node to the graph. This action will only be performed when, and if, the
     * changes are committed. This will only add the node if it hasn't already been added when the
     * changed are committed.
     *
     * @param node the node to add.
     */
    void ensureAdded(ValueNode node);

    /**
     * This method performs either {@link #replaceWithValue(ValueNode)} or
     * {@link #replaceWithVirtual(VirtualObjectNode)}, depending on the given value.
     *
     * @param value the replacement value
     */
    void replaceWith(ValueNode value);

    /**
     *
     * If state is virtual, materialization is performed for the given state.
     *
     * @return true if materialization happened, false if not.
     */
    boolean ensureMaterialized(VirtualObjectNode virtualObject);

    /**
     *
     * Returns whether deoptimization can recover from virtualizing large unsafe accesses to a byte
     * array.
     *
     * @return true if deoptimization can recover, false if not.
     */
    boolean canVirtualizeLargeByteArrayUnsafeAccess();

    OptionValues getOptions();

    DebugContext getDebug();

    /**
     *
     * Creates a deep-copy of the VirtualizerTool, snapshotting the current virtual ObjectStates.
     *
     * @return new VirtualizerTool, deep-copied from this.
     */
    VirtualizerTool createSnapshot();
}
