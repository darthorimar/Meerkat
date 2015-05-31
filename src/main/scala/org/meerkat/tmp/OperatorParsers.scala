package org.meerkat.tmp

import org.meerkat.sppf.NonPackedNode
import org.meerkat.sppf.SPPFLookup
import org.meerkat.util.Input
import java.util.HashMap
import org.meerkat.sppf.DefaultSPPFLookup

object OperatorParsers {
  
  import AbstractOperatorParsers._
  import Parsers._
  
  object OperatorImplicits {
  
    implicit def obj1[ValA,ValB](implicit vals: ValA|~|ValB) = new CanBuildSequence[NonPackedNode,NonPackedNode,ValA,ValB] {
      implicit val o = Parsers.obj1[ValA,ValB](vals)

      type OperatorSequence = OperatorParsers.OperatorSequence[o.V]
      
      def sequence(p: AbstractOperatorSequence): OperatorSequence 
        = new OperatorSequence { 
            def apply(prec1: Prec, prec2: Prec) = p(prec1, prec2)
            def infix = p.infix; def prefix = p.prefix; def postfix = p.postfix
            def assoc = p.assoc
          }
      
      def assoc(p: OperatorSequence, a: Assoc.Assoc): OperatorSequence 
        = if ((p.infix && a != Assoc.NON_ASSOC) 
                || ((p.infix || p.prefix || p.postfix) && a == Assoc.NON_ASSOC)) 
            new OperatorSequence {
              def apply(prec1: Prec, prec2: Prec) = p(prec1, prec2)
              def infix = p.infix; def prefix = p.prefix; def postfix = p.postfix
              def assoc: Assoc.Assoc = a
            } 
        else p
      
      type OperatorSequenceBuilder = OperatorParsers.OperatorSequenceBuilder[o.V]
      def builderSeq(f: Head => OperatorSequence): OperatorSequenceBuilder
        = new OperatorSequenceBuilder { def apply(head: Head) = f(head) }
    }
    
    implicit def obj2[ValA,ValB] = new CanBuildAlternation[NonPackedNode,NonPackedNode,ValA,ValB] {
      implicit val o = Parsers.obj3[ValA,ValB]
      
      type OperatorAlternation = OperatorParsers.OperatorAlternation[ValB]     
      def alternation(f: Prec => o.AlternationBuilder): OperatorAlternation 
        = new OperatorAlternation { def apply(prec: Prec) = f(prec) }
      
      type OperatorAlternationBuilder = OperatorParsers.OperatorAlternationBuilder[ValB]
      def builderAlt(f: (Head, Group) => (Group => OperatorAlternation, Group, Option[Group])): OperatorAlternationBuilder
        = new OperatorAlternationBuilder { def apply(head: Head, group: Group) = f(head, group) }
    }
//    
//    implicit object obj3 extends CanBuildNonterminal[NonPackedNode] {
//      implicit val obj1 = Parsers.obj4
//      implicit val obj2 = Parsers.obj2
//      
//      type OperatorNonterminal = OperatorParsers.OperatorNonterminal
//    
//      def nonterminal(name: String, f: Prec => obj1.Nonterminal): OperatorNonterminal
//        = new OperatorNonterminal {
//            val table: java.util.Map[Prec, Parsers.Nonterminal] = new HashMap()
//            def apply(prec: Prec) = if (table.containsKey(prec)) table.get(prec) 
//                                    else {
//                                      val nt = f(prec)
//                                      table.put(prec, nt)
//                                      nt
//                                    }
//            override def toString = name
//          }
//    }
  
  }
  
  trait OperatorSequence[V] extends ((Prec, Prec) => Parsers.SequenceBuilder { type Value = V }) {
    def infix: Boolean; def prefix: Boolean; def postfix: Boolean
    def assoc: Assoc.Assoc
  }
  
  trait OperatorAlternation[V] extends (Prec => Parsers.AlternationBuilder { type Value = V })
  
  trait OperatorNonterminal[V] extends (Prec => Parsers.Nonterminal { type Value = V }) { import OperatorImplicits._; import AbstractOperatorParser._
    def ~ [U](p: OperatorNonterminal[U])(implicit tuple: V|~|U) = { implicit val o = obj1[V,U](tuple); seqNt(this, p) }
    def ~ (p: Symbol)(implicit tuple: V|~|p.Value) = { implicit val o = obj1[V,p.Value](tuple); seqNtSym(this, p) }
    
    def | [U >: V](p: OperatorAlternationBuilder[U]) = altOpSymOpAlt(this, p)
    def | [U >: V](p: OperatorSequenceBuilder[U]) = altOpSymOpSeq(this, p)
    def | [U >: V](p: OperatorNonterminal[U]) = altOpSym(this, p)
    
    def | [U >: V](p: AlternationBuilder { type Value = U }) = altOpSymOpAlt(this, altAltOpAlt(p))
    def | [U >: V](p: SequenceBuilder { type Value = U }) = altOpSymOpSeq(this, altSeqOpSeq(p))
    def | [U >: V](p: Symbol { type Value = U }) = altOpSym(this, altSymOpSym(p))        
  }
  
  trait OperatorSequenceBuilder[V] extends (Head => OperatorSequence[V]) { import OperatorImplicits._; import AbstractOperatorParser._
    def ~ [U](p: OperatorNonterminal[U])(implicit tuple: V|~|U) = { implicit val o = obj1[V,U](tuple); seqOpSeqNt(this, p) }
    def ~ (p: Symbol)(implicit tuple: V|~|p.Value) = { implicit val o = obj1[V,p.Value](tuple); seqOpSeqSym(this, p) }
    
    def | [U >: V](p: OperatorAlternationBuilder[U]) = altOpSeqOpAlt(this, p)
    def | [U >: V](p: OperatorSequenceBuilder[U]) = altOpSeq(this, p)
    def | [U >: V](p: OperatorNonterminal[U]) = altOpSeqOpSym(this, p)
    
    def | [U >: V](p: AlternationBuilder { type Value = U }) = altOpSeqOpAlt(this, altAltOpAlt(p))
    def | [U >: V](p: SequenceBuilder { type Value = U }) = altOpSeq(this, altSeqOpSeq(p))
    def | [U >: V](p: Symbol { type Value = U }) = altOpSeqOpSym(this, altSymOpSym(p))
    
//    def |> (p: OperatorAlternationBuilder) = greaterOpSeqOpAlt(this, p)
//    def |> (p: OperatorSequenceBuilder) = greaterOpSeq(this, p)
  }
  
  trait OperatorAlternationBuilder[V] extends ((Head, Group) => (Group => OperatorAlternation[V], Group, Option[Group])) { import OperatorImplicits._; import AbstractOperatorParser._
    def | [U >: V](p: OperatorAlternationBuilder[U]) = altOpAlt(this, p)
    def | [U >: V](p: OperatorSequenceBuilder[U]) = altOpAltOpSeq(this, p)
    def | [U >: V](p: OperatorNonterminal[U]) = altOpAltOpSym(this, p)
    
    def | [U >: V](p: AlternationBuilder { type Value = U }) = altOpAlt(this, altAltOpAlt(p))
    def | [U >: V](p: SequenceBuilder { type Value = U }) = altOpAltOpSeq(this, altSeqOpSeq(p))
    def | [U >: V](p: Symbol { type Value = U }) = altOpAltOpSym(this, altSymOpSym(p))
    
//    def |> (p: OperatorAlternationBuilder) = greaterOpAlt(this, p)
//    def |> (p: OperatorSequenceBuilder) = greaterOpAltOpSeq(this, p)
  }
  
  implicit class ParsersSeqOps[V](p: Parsers.Symbol { type Value = V }) { import OperatorImplicits._; import AbstractOperatorParser._
    def ~ [U](q: OperatorNonterminal[U])(implicit tuple: V|~|U) = { implicit val o = obj1[V,U](tuple); seqSymNt(p, q) }
    
    def | [U >: V](q: OperatorAlternationBuilder[U]) = altOpSymOpAlt(altSymOpSym(p), q)
    def | [U >: V](q: OperatorSequenceBuilder[U]) = altOpSymOpSeq(altSymOpSym(p), q)
    def | [U >: V](q: OperatorNonterminal[U]) = altOpSym(altSymOpSym(p), q)
  }
  
  implicit class ParsersAltOps1[V](p: Parsers.SequenceBuilder { type Value = V }) { import OperatorImplicits._; import AbstractOperatorParser._
    def ~ [U](q: OperatorNonterminal[U])(implicit tuple: V|~|U) = { implicit val o = obj1[V,U](tuple); seqSeqNt(p, q) }
    
    def | [U >: V](q: OperatorAlternationBuilder[U]) = altOpSeqOpAlt(altSeqOpSeq(p), q)
    def | [U >: V](q: OperatorSequenceBuilder[U]) = altOpSeq(altSeqOpSeq(p), q)
    def | [U >: V](q: OperatorNonterminal[U]) = altOpSeqOpSym(altSeqOpSeq(p), q)
  }
  
  implicit class ParsersAltOps2[V](p: Parsers.AlternationBuilder { type Value = V }) { import OperatorImplicits._; import AbstractOperatorParser._
    def | [U >: V](q: OperatorAlternationBuilder[U]) = altOpAlt(altAltOpAlt(p), q)
    def | [U >: V](q: OperatorSequenceBuilder[U]) = altOpAltOpSeq(altAltOpAlt(p), q)
    def | [U >: V](q: OperatorNonterminal[U]) = altOpAltOpSym(altAltOpAlt(p), q)
  }
  
  implicit class StringSeqOps(term: String) { import OperatorImplicits._; import AbstractOperatorParser._
    val p = Parsers.toTerminal(term)
    def ~ [U](q: OperatorNonterminal[U])(implicit tuple: p.Value|~|U) = { implicit val o = obj1[p.Value,U](tuple); seqSymNt(p, q) } 
  }
  
  implicit class StringAltOps(term: String) { import OperatorImplicits._; import AbstractOperatorParser._
    val p: Symbol { type Value = NoValue } = term
    
    def | (q: OperatorAlternationBuilder[NoValue]) = altOpSymOpAlt(altSymOpSym(p), q)
    def | (q: OperatorSequenceBuilder[NoValue]) = altOpSymOpSeq(altSymOpSym(p), q)
    def | (q: OperatorNonterminal[NoValue]) = altOpSym(altSymOpSym(p), q)
  }
  
  def left[V](p: OperatorSequenceBuilder[V]): OperatorSequenceBuilder[V] = { import OperatorImplicits._
    val o = obj1[V,V](new |~|[V,V] { type R = V })
    o.builderSeq(head => o.assoc(p(head), Assoc.LEFT))
  }
  
  def right[V](p: OperatorSequenceBuilder[V]): OperatorSequenceBuilder[V] = { import OperatorImplicits._
    val o = obj1[V,V](new |~|[V,V] { type R = V })
    o.builderSeq(head => o.assoc(p(head), Assoc.RIGHT))
  }
  
  def non_assoc[V](p: OperatorSequenceBuilder[V]): OperatorSequenceBuilder[V] = { import OperatorImplicits._
    val o = obj1[V,V](new |~|[V,V] { type R = V })
    o.builderSeq(head => o.assoc(p(head), Assoc.NON_ASSOC))
  }
  
//  def left(p: OperatorAlternationBuilder): OperatorAlternationBuilder = { import OperatorImplicits._; import AbstractOperatorParser.assocAlt
//    assocAlt(obj2)(p, Assoc.LEFT)
//  }
//  
//  def right(p: OperatorAlternationBuilder): OperatorAlternationBuilder = { import OperatorImplicits._; import AbstractOperatorParser.assocAlt
//    assocAlt(obj2)(p, Assoc.RIGHT)
//  }
//  
//  def non_assoc(p: OperatorAlternationBuilder): OperatorAlternationBuilder = { import OperatorImplicits._; import AbstractOperatorParser.assocAlt
//    assocAlt(obj2)(p, Assoc.NON_ASSOC)
//  }
//  
//  def ntAlt(name: String, p: => OperatorAlternationBuilder): OperatorNonterminal = { import OperatorImplicits._; import AbstractOperatorParser.nonterminalAlt
//    implicit val m = Parsers.obj3
//    nonterminalAlt(name, p)
//  }
//  def ntSeq(name: String, p: => OperatorSequenceBuilder): OperatorNonterminal = { import OperatorImplicits._; import AbstractOperatorParser.nonterminalSeq
//    implicit val m = Parsers.obj3
//    nonterminalSeq(name, p)
//  }
//  def ntSym(name: String, p: OperatorNonterminal): OperatorNonterminal = { import OperatorImplicits._; import AbstractOperatorParser.nonterminalSym
//    implicit val m = Parsers.obj3
//    nonterminalSym(name, p)
//  }
//    
//  def run(input: Input, sppf: SPPFLookup, parser: AbstractCPSParsers.AbstractParser[NonPackedNode]): Unit = {
//    parser(input, 0, sppf)(t => if(t.rightExtent == input.length) { println(s"Success: $t")  })
//    Trampoline.run
//  }
//  
//  def parse(sentence: String, parser: OperatorNonterminal): Unit = {
//    val input = new Input(sentence)
//    val sppf = new DefaultSPPFLookup(input)
//    
//    val p = parser((0,0))
//    run(input, sppf, p)
//    
//    println(s"Trying to find: ${p.name}(0,${sentence.length()})")
//    val startSymbol = sppf.getStartNode(p, 0, sentence.length())
//    
//    startSymbol match {
//      case None       => println("Parse error")
//      case Some(node) => println("Success: " + node)
//                         println(sppf.countAmbiguousNodes + ", " + sppf.countIntermediateNodes + ", " + sppf.countPackedNodes + ", " + sppf.countNonterminalNodes + ", " + sppf.countTerminalNodes)
//                         println("Visualizing...") 
//                         Visualization.visualize(Visualization.toDot(startSymbol.get), "sppf")
//                         println("Done!")
//    }
//  }
  
}