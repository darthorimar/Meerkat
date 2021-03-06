/*
 * Copyright (c) 2015, Anastasia Izmaylova and Ali Afroozeh, Centrum Wiskunde & Informatica (CWI)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this
 *    list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */

package org.meerkat.sppf

import scala.collection.mutable
import org.meerkat.tree._

trait Slot {
  def ruleType: Rule
}

trait SPPFNode extends Cloneable {
  type T <: SPPFNode
  def children: Seq[T]
  def size: Int

  def copy(): SPPFNode
}

trait NonPackedNode extends SPPFNode {

  type T = PackedNode

  var first: T = _

  var rest: mutable.Buffer[T] = _

  val name: Any
  val leftExtent, rightExtent: Int

  def children: Seq[T] =
    if (first == null) mutable.ListBuffer()
    else if (rest == null) mutable.ListBuffer(first)
    else mutable.ListBuffer(first) ++ rest

  def init(): Unit = rest = mutable.Buffer[T]()

  def addPackedNode(packedNode: PackedNode,
                    leftChild: Option[NonPackedNode],
                    rightChild: NonPackedNode): Boolean = {
    attachChildren(packedNode, leftChild, rightChild)
    if (first == null) {
      first = packedNode
    } else {
      if (rest == null) init()
      rest += packedNode
    }

    true
  }

  def attachChildren(packedNode: PackedNode,
                     leftChild: Option[NonPackedNode],
                     rightChild: NonPackedNode): Unit =
    leftChild match {
      case Some(c) =>
        packedNode.leftChild = c; packedNode.rightChild = rightChild
      case None => packedNode.leftChild = rightChild
    }

  def isAmbiguous: Boolean = rest != null

  def hasChildren: Boolean = first != null || rest != null

  def size: Int = {
    val s = if (first != null) 1 else 0
    if (rest != null) s + rest.size else s
  }

  def isIntermediateNode: Boolean = this.isInstanceOf[IntermediateNode]

  override def toString: String = s"$name,$leftExtent,$rightExtent"
}

case class NonterminalNode(name: Any, leftExtent: Int, rightExtent: Int)
    extends NonPackedNode {
  override def copy(): NonterminalNode = {
    val copy = NonterminalNode(name, leftExtent, rightExtent)
    copy.first = this.first
    copy.rest = this.rest
    copy
  }
}

case class IntermediateNode(name: Any, leftExtent: Int, rightExtent: Int)
    extends NonPackedNode {
  override def copy(): IntermediateNode = {
    val copy = IntermediateNode(name, leftExtent, rightExtent)
    copy.first = this.first
    copy.rest = this.rest
    copy
  }
}

trait AbstractTerminalNode

case class EpsilonNode(extent: Int) extends NonPackedNode {
  override val leftExtent: Int  = extent
  override val rightExtent: Int = extent
  override val name: Any        = "Epsilon node"

  override def copy(): EpsilonNode = {
    val copy = new EpsilonNode(extent)
    copy.first = this.first
    copy.rest = this.rest
    copy
  }
}

case class EdgeNode[+L](s: L, leftExtent: Int, rightExtent: Int, out: Boolean)
    extends NonPackedNode
    with AbstractTerminalNode {

  override val name: Any = s

  override def copy(): EdgeNode[L] = {
    val copy = new EdgeNode(s, leftExtent, rightExtent, out)
    copy.first = this.first
    copy.rest = this.rest
    copy
  }
}

case class VertexNode[+N](s: N, extent: Int) extends NonPackedNode {
  override val name: Any        = s
  override val leftExtent: Int  = extent
  override val rightExtent: Int = extent

  override def copy(): VertexNode[N] = {
    val copy = new VertexNode(s, extent)
    copy.first = this.first
    copy.rest = this.rest
    copy
  }
}

case class PackedNode(slot: Slot, parent: NonPackedNode) extends SPPFNode {

  type T = NonPackedNode

  var leftChild: T  = _
  var rightChild: T = _

  def pivot: Int = leftChild.rightExtent

  def ruleType: Rule = slot.ruleType

  def children: mutable.Buffer[T] =
    mutable.ListBuffer(leftChild, rightChild).filter(_ != null)

  def size: Int = if (hasRightChild) 2 else 1

  def hasRightChild: Boolean = rightChild != null

  override def toString = s"$slot,$pivot, parent=($parent)"

  override def equals(o: Any): Boolean = o match {
    case p: PackedNode =>
      slot == p.slot && parent == p.parent && pivot == p.pivot
    case _ => false
  }

  override def copy(): PackedNode = {
    val copy = PackedNode(slot, parent)
    copy.leftChild = this.leftChild
    copy.rightChild = this.rightChild
    copy
  }
}
