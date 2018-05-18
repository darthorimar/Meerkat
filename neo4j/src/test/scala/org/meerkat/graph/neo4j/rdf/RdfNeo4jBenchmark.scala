package org.meerkat.graph.neo4j.rdf

import org.meerkat.graph.rdf.RdfBenchmark
import org.meerkat.graph.neo4j.edgesToNeo4jGraph

object RdfNeo4jBenchmark extends App with RdfBenchmark {
  benchmark(edgesToNeo4jGraph)
}
