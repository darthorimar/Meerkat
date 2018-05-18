package org.meerkat.graph.rdf

import org.meerkat.graph.{edgesToInMemoryGraph, parseGraphFromAllPositions}
import org.meerkat.input.Input
import org.meerkat.parsers.AbstractCPSParsers
import org.scalameter.api._
import org.scalameter.{Key, Quantity, Warmer, config}

import scala.collection.mutable

trait RdfBenchmark extends RdfMixin {
  def benchmark(edgesToGraph: (List[(Int, String, Int)], Int) => Input[L]): Unit = {

    val results = new mutable.ArrayBuffer[(String, Quantity[Double], Quantity[Double])]()
    for ((file, _, _) <- rdfs) {
      val triples = getTriples(file)
      val (edges, nodesCount) = triplesToEdges(triples)
      val graph = edgesToInMemoryGraph(edges, nodesCount)

      def parseAndGetRunningTime(grammar: AbstractCPSParsers.AbstractSymbol[L, _, _]) = {
        val time = config(
          Key.exec.benchRuns -> 20,
          Key.verbose -> true
        ) withWarmer {
          new Warmer.Default
        } withMeasurer {
          new Measurer.IgnoringGC
        } measure {
          parseGraphFromAllPositions(grammar, graph).length
        }
        time
      }

      val time1 = parseAndGetRunningTime(grammar.G1)
      val time2 = parseAndGetRunningTime(grammar.G2)
      results += ((file, time1, time2))
    }

    println("Mesure results are:")
    for ((file, time1, time2) <- results) {
      println(s"$file, $time1, $time2")
    }
  }
}