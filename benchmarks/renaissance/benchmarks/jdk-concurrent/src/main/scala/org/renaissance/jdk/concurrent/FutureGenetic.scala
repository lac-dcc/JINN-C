package org.renaissance.jdk.concurrent

import org.renaissance.Config
import org.renaissance.License
import org.renaissance.RenaissanceBenchmark
import org.renaissance.Benchmark._

@Name("future-genetic")
@Group("jdk-concurrent")
@Summary("Runs a genetic algorithm using the Jenetics library and futures.")
@Licenses(Array(License.APACHE2))
@Repetitions(50)
class FutureGenetic extends RenaissanceBenchmark {

  // TODO: Consolidate benchmark parameters across the suite.
  //  See: https://github.com/renaissance-benchmarks/renaissance/issues/27

  val threadCount = org.renaissance.BenchmarkInputs.getNumThreads

  val randomSeed = 7

  var geneMinValue = -2000

  var geneMaxValue = 2000

  var geneCount = 200

  var chromosomeCount = org.renaissance.BenchmarkInputs.getChromoCount // 50

  var generationCount = org.renaissance.BenchmarkInputs.getGenCount //5000

  var benchmark: JavaJenetics = null

  override def setUpBeforeAll(c: Config): Unit = {
    if (c.functionalTest) {
      chromosomeCount = 10
      generationCount = 200
    }
    benchmark = new JavaJenetics(
      geneMinValue,
      geneMaxValue,
      geneCount,
      chromosomeCount,
      generationCount,
      threadCount,
      randomSeed
    )
    benchmark.setupBeforeAll()
  }

  override def tearDownAfterAll(c: Config): Unit = {
    benchmark.tearDownAfterAll()
  }

  override def runIteration(c: Config): Unit = {
    blackHole(benchmark.runRepetition())
  }
}
