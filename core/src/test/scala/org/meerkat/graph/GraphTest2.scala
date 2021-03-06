package org.meerkat.graph

import org.meerkat.Syntax._
import org.meerkat.input.GraphxInput
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.OptionValues._

import scalax.collection.Graph
import scalax.collection.edge.Implicits._

class GraphTest2 extends FunSuite {
  val E: Nonterminal[String, Nothing] = syn(
    E ~ "+" ~ E |
      E ~ "*" ~ E |
      Num
  )
  val Num = syn("1" | "0")

  val g = Graph(
    (0 ~+#> 1)("0"),
    (0 ~+#> 2)("+"),
    (1 ~+#> 2)("*"),
    (2 ~+#> 0)("1")
  )

  test("sppfStat") {
    parseGraphAndGetSppfStatistics(E, GraphxInput(g)).value shouldBe SPPFStatistics(
      5,
      6,
      4,
      13,
      2)
  }

}
