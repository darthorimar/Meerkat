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

package org.meerkat.benchmark

import java.lang.management._
import java.io.File
import org.apache.commons.io.FileUtils
import scala.collection.mutable.ListBuffer
import com.google.common.testing.GcFinalization
import org.meerkat.parsers._
import org.meerkat.parsers.Parsers._
import scala.concurrent.impl.Future
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import org.meerkat.util.Input
import java.util.concurrent.Callable

class MeerkatBenchmark(parser: Nonterminal,
                       files: scala.Seq[File],
                       warmupCount: Int = 0,
                       runCount: Int = 1,
                       runGCInBetween: Boolean = false,
                       timeOut: Int = -1) {

  printf("%-20s %-20s %-20s %-20s %-20s %-15s %-15s\n", "size", "user_time", "nonterminal_nodes", "intermediate_nodes", "terminal_nodes", "packed_nodes", "ambiguous_nodes")
  
  def run() {
    for (f <- files) {
      val input: Input = Input(scala.io.Source.fromFile(f).mkString)
      
      println("#" + f)
      run(input)
      if (runGCInBetween) GcFinalization.awaitFullGc()
    }
  }
                       
  private def run(input: Input) {
    parse(parser, input) match {
      case Right(x) => printStatistics(x.stat, input)
      case Left(x)  => println(s"Parse error $x")
    }
  }
  
  def printStatistics(s: ParseStatistics, input: Input): Unit =  s match {
    case ParseStatistics(nanoTime, userTime, cpuTime, countNonterminalNodes, countIntermediateNodes, countTerminalNodes, countPackedNodes, countAmbiguousNodes) => {
      printf("%-20d %-20d %-20d %-20d %-20d %-15d %-15d\n", input.length, userTime, countNonterminalNodes, countIntermediateNodes, countTerminalNodes, countPackedNodes, countAmbiguousNodes)
    }
  }
}

object MeerkatBenchmark {
  
  def apply(parser: Nonterminal,
            files: scala.Seq[File],
            warmupCount: Int = 0,
            runCount: Int = 1,
            runGCInBetween: Boolean = false,
            timeOut: Int = -1): MeerkatBenchmark = new MeerkatBenchmark(parser, files, warmupCount, runCount, runGCInBetween, timeOut)
}

