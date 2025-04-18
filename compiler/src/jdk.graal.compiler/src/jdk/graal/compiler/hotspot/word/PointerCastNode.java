/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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
package jdk.graal.compiler.hotspot.word;

import static jdk.graal.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static jdk.graal.compiler.nodeinfo.NodeSize.SIZE_0;

import jdk.graal.compiler.core.common.type.Stamp;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.graph.NodeClass;
import jdk.graal.compiler.hotspot.word.HotSpotOperation.HotspotOpcode;
import jdk.graal.compiler.nodeinfo.NodeInfo;
import jdk.graal.compiler.nodes.NodeView;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.nodes.calc.FloatingNode;
import jdk.graal.compiler.nodes.spi.Canonicalizable;
import jdk.graal.compiler.nodes.spi.CanonicalizerTool;
import jdk.graal.compiler.nodes.spi.LIRLowerable;
import jdk.graal.compiler.nodes.spi.NodeLIRBuilderTool;
import jdk.vm.ci.meta.Value;

/**
 * Cast between Word and metaspace pointers exposed by the {@link HotspotOpcode#FROM_POINTER},
 * {@link HotspotOpcode#FROM_COMPRESSED_POINTER} and {@link HotspotOpcode#TO_KLASS_POINTER}
 * operations.
 */
@NodeInfo(cycles = CYCLES_0, size = SIZE_0)
public final class PointerCastNode extends FloatingNode implements Canonicalizable, LIRLowerable, Node.ValueNumberable {

    public static final NodeClass<PointerCastNode> TYPE = NodeClass.create(PointerCastNode.class);
    @Input ValueNode input;

    protected PointerCastNode(Stamp stamp, ValueNode input) {
        super(TYPE, stamp);
        this.input = input;
    }

    public static ValueNode create(Stamp stamp, ValueNode input) {
        ValueNode canonicalNode = canonicalize(stamp, input);
        if (canonicalNode != null) {
            return canonicalNode;
        }
        return new PointerCastNode(stamp, input);
    }

    public ValueNode getInput() {
        return input;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        Value value = generator.operand(input);
        assert value.getValueKind().equals(generator.getLIRGeneratorTool().getLIRKind(stamp(NodeView.DEFAULT))) : "PointerCastNode shouldn't change the LIRKind";

        generator.setResult(this, value);
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        ValueNode canonicalNode = canonicalize(this.stamp, this.input);
        if (canonicalNode != null) {
            return canonicalNode;
        }
        return this;
    }

    private static ValueNode canonicalize(Stamp stamp, ValueNode input) {
        // Replace useless cascade of casts (e.g., in the allocation snippets)
        if (input instanceof PointerCastNode) {
            PointerCastNode pointerCast = (PointerCastNode) input;
            if (pointerCast.input.stamp(NodeView.DEFAULT).equals(stamp)) {
                return pointerCast.input;
            }
        }
        return null;
    }
}
