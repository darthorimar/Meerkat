package org.meerkat.graph.rdf

import org.meerkat.graph.edgesToInMemoryGraph



object GraphxRdfBenchmark extends App with RdfBenchmark {
  benchmark(edgesToInMemoryGraph)
}
